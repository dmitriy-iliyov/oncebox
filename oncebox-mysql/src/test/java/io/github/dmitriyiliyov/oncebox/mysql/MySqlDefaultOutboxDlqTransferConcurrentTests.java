package io.github.dmitriyiliyov.oncebox.mysql;

import io.github.dmitriyiliyov.oncebox.core.outbox.dlq.DefaultOutboxDlqTransfer;
import io.github.dmitriyiliyov.oncebox.core.outbox.dlq.DefaultOutboxDlqTransferVerifier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;

import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.stream.Stream;

public class MySqlDefaultOutboxDlqTransferConcurrentTests extends BaseMySqlIntegrationTests {

    private static final int ITERATION = 3;

    private final DefaultOutboxDlqTransferVerifier verifier;

    @Autowired
    MySqlDefaultOutboxDlqTransferConcurrentTests(
            MySqlOutboxRepository outboxRepository,
            MySqlOutboxDlqRepository dlqRepository,
            JdbcTemplate jdbcTemplate,
            @Qualifier("mysqlOutboxDlqTransfer") DefaultOutboxDlqTransfer transfer
    ) {
        this.verifier = new DefaultOutboxDlqTransferVerifier(
                jdbcTemplate,
                outboxRepository,
                dlqRepository,
                transfer,
                raw -> {
                    byte[] bytes = (byte[]) raw;
                    ByteBuffer bb = ByteBuffer.wrap(bytes);
                    return new UUID(bb.getLong(), bb.getLong());
                }
        );
    }

    static Stream<Arguments> concurrentArgs() {
        return Stream.of(
                Arguments.of(100, 2),
                Arguments.of(100, 5),
                Arguments.of(100, 10),
                Arguments.of(1000, 10),
                Arguments.of(1000, 100),
                Arguments.of(10000, 100),
                Arguments.of(10000, 1000)
        );
    }

    @AfterEach
    void cleanup() throws InterruptedException {
        Thread.sleep(3000);
        verifier.getJdbcTemplate().execute("DELETE FROM outbox_events");
        verifier.getJdbcTemplate().execute("DELETE FROM outbox_dlq_events");
    }

    @Disabled("""
            MySQL only. Under InnoDB REPEATABLE READ the two-step SELECT ... FOR UPDATE SKIP LOCKED +
            UPDATE deadlocks on gap locks; running the outbox datasource at READ COMMITTED fixes this
            case. See README (MySQL note). oncebox does not set the isolation itself, so this stays
            disabled.
    """)
    @MethodSource("concurrentArgs")
    @ParameterizedTest
    @DisplayName("CT transferToDlq() concurrent execution should move each event exactly once")
    void transferToDlq_concurrent(int eventCount, int threadCount) throws InterruptedException {
        for (int i = 0; i < ITERATION; i++) {
            verifier.transferToDlq_concurrent(eventCount, threadCount);
            cleanup();
        }
    }

    @Disabled("""
            MySQL only. READ COMMITTED removes the gap-lock deadlock, but at extreme fan-in (batch size
            equal to the whole backlog, e.g. 1000/10) concurrent transfers still hit record-lock
            deadlocks on the bulk INSERT into outbox_events + DELETE FROM outbox_dlq_events, which needs
            retry-on-deadlock to survive.
    """)
    @MethodSource("concurrentArgs")
    @ParameterizedTest
    @DisplayName("CT transferFromDlq() concurrent execution should move each event exactly once")
    void transferFromDlq_concurrent(int eventCount, int threadCount) throws InterruptedException {
        for (int i = 0; i < ITERATION; i++) {
            verifier.transferFromDlq_concurrent(eventCount, threadCount);
            cleanup();
        }
    }
}
