package io.github.dmitriyiliyov.oncebox.postgresql;

import io.github.dmitriyiliyov.oncebox.core.outbox.dlq.DefaultOutboxDlqTransfer;
import io.github.dmitriyiliyov.oncebox.core.outbox.dlq.DefaultOutboxDlqTransferVerifier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;
import java.util.stream.Stream;

public class PostgreSqlDefaultOutboxDlqTransferConcurrentTests extends BasePostgresSqlIntegrationTests {

    private static final int ITERATION = 3;
    private final DefaultOutboxDlqTransferVerifier verifier;

    @Autowired
    PostgreSqlDefaultOutboxDlqTransferConcurrentTests(
            PostgreSqlOutboxRepository outboxRepository,
            PostgreSqlOutboxDlqRepository dlqRepository,
            @Qualifier("postgresOutboxDlqTransfer") DefaultOutboxDlqTransfer transfer,
            JdbcTemplate jdbcTemplate
    ) {
        this.verifier = new DefaultOutboxDlqTransferVerifier(
                jdbcTemplate,
                outboxRepository,
                dlqRepository,
                transfer,
                raw -> (UUID) raw
        );
    }

    static Stream<Arguments> concurrentArgs() {
        return Stream.of(
                Arguments.of(100, 2),
                Arguments.of(100, 5),
                Arguments.of(100, 10),
                Arguments.of(1000, 10),
                Arguments.of(1000, 100)
        );
    }

    @AfterEach
    void cleanup() throws InterruptedException {
        Thread.sleep(1000);
        verifier.getJdbcTemplate().execute("DELETE FROM outbox_events");
        verifier.getJdbcTemplate().execute("DELETE FROM outbox_dlq_events");
    }

    @MethodSource("concurrentArgs")
    @ParameterizedTest
    @DisplayName("CT transferToDlq() concurrent execution should move each event exactly once")
    void transferToDlq_concurrent(int eventCount, int threadCount) throws InterruptedException {
        for (int i = 0; i < ITERATION; i++) {
            verifier.transferToDlq_concurrent(eventCount, threadCount);
            cleanup();
        }
    }

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
