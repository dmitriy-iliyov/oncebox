package io.github.dmitriyiliyov.oncebox.oracle;

import io.github.dmitriyiliyov.oncebox.core.outbox.OutboxEvent;
import io.github.dmitriyiliyov.oncebox.core.outbox.dlq.DlqStatus;
import io.github.dmitriyiliyov.oncebox.core.outbox.dlq.OutboxDlqEvent;
import io.github.dmitriyiliyov.oncebox.core.outbox.dlq.OutboxDlqRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.ByteBuffer;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class OracleOutboxDlqRepositoryConcurrentTests extends BaseOracleIntegrationTests {

    private static final int TOTAL_EVENTS = 100;
    private static final int BATCH_SIZE = 10;

    private final OutboxDlqRepository repository;
    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate txTemplate;

    @Autowired
    OracleOutboxDlqRepositoryConcurrentTests(@Qualifier("oracleOutboxDlqRepository") OutboxDlqRepository repository,
                                             @Qualifier("oracleJdbcTemplate") JdbcTemplate jdbcTemplate,
                                             PlatformTransactionManager transactionManager) {
        this.repository = repository;
        this.jdbcTemplate = jdbcTemplate;
        this.txTemplate = new TransactionTemplate(transactionManager);
    }

    @AfterEach
    void cleanup() {
        jdbcTemplate.execute("DELETE FROM outbox_dlq_events");
    }

    @Test
    @DisplayName("IT second DLQ poller locks a disjoint batch while the first holds its batch open")
    void concurrentPollers_secondPollerGetsDisjointBatch() throws Exception {
        seedDlqEvents(TOTAL_EVENTS);

        CountDownLatch firstLocked = new CountDownLatch(1);
        CountDownLatch secondDone = new CountDownLatch(1);
        AtomicReference<List<OutboxDlqEvent>> batchA = new AtomicReference<>(List.of());
        AtomicReference<Throwable> pollerAError = new AtomicReference<>();

        Thread pollerA = new Thread(() -> {
            try {
                txTemplate.executeWithoutResult(status -> {
                    batchA.set(repository.findAndLockBatchByStatus(
                            DlqStatus.TO_RETRY, BATCH_SIZE, DlqStatus.IN_PROCESS));
                    firstLocked.countDown();
                    try {
                        secondDone.await(15, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
            } catch (Throwable t) {
                pollerAError.set(t);
                firstLocked.countDown();
            }
        }, "dlq-poller-A");
        pollerA.start();

        assertThat(firstLocked.await(15, TimeUnit.SECONDS)).as("poller A should acquire its batch").isTrue();
        assertThat(pollerAError.get()).isNull();

        List<OutboxDlqEvent> batchB = txTemplate.execute(status ->
                repository.findAndLockBatchByStatus(DlqStatus.TO_RETRY, BATCH_SIZE, DlqStatus.IN_PROCESS));
        secondDone.countDown();
        pollerA.join(15_000);

        Set<UUID> idsA = batchA.get().stream().map(OutboxEvent::getId).collect(Collectors.toSet());
        Set<UUID> idsB = batchB == null ? Set.of() : batchB.stream().map(OutboxEvent::getId).collect(Collectors.toSet());

        assertThat(idsA).as("poller A should lock a full batch").hasSize(BATCH_SIZE);
        assertThat(idsB).as("poller B should lock its own batch instead of being starved").hasSize(BATCH_SIZE);
        assertThat(idsB).as("the two pollers must not share events").doesNotContainAnyElementsOf(idsA);
    }

    private void seedDlqEvents(int count) {
        Instant past = Instant.now().minusSeconds(3600);
        jdbcTemplate.batchUpdate(
                """
                INSERT INTO outbox_dlq_events
                (id, status, dlq_status, event_type, payload_type, payload,
                 retry_count, next_retry_at, created_at, updated_at, moved_at)
                VALUES (?, 'FAILED', 'TO_RETRY', 'dlq-poller', 'p', '{}', 0, ?, ?, ?, ?)
                """,
                new java.util.AbstractList<Object[]>() {
                    @Override
                    public Object[] get(int index) {
                        UUID id = UUID.randomUUID();
                        ByteBuffer bb = ByteBuffer.allocate(16);
                        bb.putLong(id.getMostSignificantBits());
                        bb.putLong(id.getLeastSignificantBits());
                        Timestamp ts = Timestamp.from(past.plusMillis(index));
                        return new Object[]{bb.array(), ts, ts, ts, ts};
                    }

                    @Override
                    public int size() {
                        return count;
                    }
                }
        );
    }
}
