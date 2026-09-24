package io.github.dmitriyiliyov.oncebox.mysql;

import io.github.dmitriyiliyov.oncebox.core.publisher.OutboxManager;
import io.github.dmitriyiliyov.oncebox.core.publisher.domain.OutboxEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;

import java.nio.ByteBuffer;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class MySqlOutboxRepositoryConcurrentTests extends BaseMySqlIntegrationTests {

    private static final int TOTAL_EVENTS = 500;
    private static final int POLLER_COUNT = 8;
    private static final int BATCH_SIZE = 25;

    private final OutboxManager manager;
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    MySqlOutboxRepositoryConcurrentTests(@Qualifier("mysqlOutboxManager") OutboxManager manager,
                                         @Qualifier("mysqlJdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.manager = manager;
        this.jdbcTemplate = jdbcTemplate;
    }

    @AfterEach
    void cleanup() {
        jdbcTemplate.execute("DELETE FROM outbox_events");
    }

    @Disabled("""
            Documents the horizontal-scaling contract for MySQL. Under the InnoDB default isolation
            (REPEATABLE READ) concurrent pollers deadlock on next-key/gap locks taken by the two-step
            SELECT ... FOR UPDATE SKIP LOCKED + UPDATE, so this fails out of the box. It passes once the
            application runs the outbox datasource at READ COMMITTED. Kept
            disabled because oncebox does not set the transaction isolation itself.
    """)
    @Test
    @DisplayName("CT concurrent pollers should lock each PENDING event exactly once")
    void concurrentPollers_lockEachEventExactlyOnce() throws Exception {
        String eventType = "multi-poller-" + UUID.randomUUID();
        seedPendingEvents(eventType, TOTAL_EVENTS);

        ConcurrentLinkedQueue<UUID> claimed = new ConcurrentLinkedQueue<>();
        AtomicReference<Throwable> firstError = new AtomicReference<>();
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(POLLER_COUNT);
        ExecutorService pool = Executors.newFixedThreadPool(POLLER_COUNT);

        for (int i = 0; i < POLLER_COUNT; i++) {
            pool.submit(() -> {
                try {
                    start.await();
                    while (true) {
                        List<OutboxEvent> batch = manager.loadBatch(eventType, BATCH_SIZE, UUID.randomUUID());
                        if (batch.isEmpty()) {
                            break;
                        }
                        batch.forEach(event -> claimed.add(event.getId()));
                    }
                } catch (Throwable t) {
                    firstError.compareAndSet(null, t);
                } finally {
                    done.countDown();
                }
            });
        }

        start.countDown();
        boolean finished = done.await(60, TimeUnit.SECONDS);
        pool.shutdownNow();

        assertThat(finished).as("pollers should finish within timeout").isTrue();
        assertThat(firstError.get()).as("no poller should fail (e.g. deadlock)").isNull();

        List<UUID> distinct = claimed.stream().distinct().collect(Collectors.toList());
        assertThat(claimed).as("no event should be claimed by more than one poller").hasSize(distinct.size());
        assertThat(distinct).as("every PENDING event should be claimed exactly once").hasSize(TOTAL_EVENTS);
        assertThat(pendingCount(eventType)).as("no PENDING event should be left behind").isZero();
    }

    private void seedPendingEvents(String eventType, int count) {
        Instant past = Instant.now().minusSeconds(3600);
        jdbcTemplate.batchUpdate(
                """
                INSERT INTO outbox_events
                (id, status, event_type, payload_type, payload, retry_count, next_retry_at, created_at, updated_at)
                VALUES (?, 'PENDING', ?, 'p', '{}', 0, ?, ?, ?)
                """,
                new java.util.AbstractList<Object[]>() {
                    @Override
                    public Object[] get(int index) {
                        UUID id = UUID.randomUUID();
                        ByteBuffer bb = ByteBuffer.allocate(16);
                        bb.putLong(id.getMostSignificantBits());
                        bb.putLong(id.getLeastSignificantBits());
                        Timestamp ts = Timestamp.from(past);
                        return new Object[]{bb.array(), eventType, ts, ts, ts};
                    }

                    @Override
                    public int size() {
                        return count;
                    }
                }
        );
    }

    private int pendingCount(String eventType) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM outbox_events WHERE event_type = ? AND status = 'PENDING'",
                Integer.class, eventType
        );
        return count == null ? 0 : count;
    }
}
