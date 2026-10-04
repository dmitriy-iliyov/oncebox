package io.github.dmitriyiliyov.oncebox.postgresql;

import io.github.dmitriyiliyov.oncebox.core.outbox.OutboxCaptureVerifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Transactional
class PostgreSqlOutboxCaptureIntegrationTests extends BasePostgresSqlIntegrationTests {

    @Autowired
    private PostgreSqlOutboxRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private Clock clock;

    private OutboxCaptureVerifier verifier;

    @BeforeEach
    void setUp() {
        verifier = new OutboxCaptureVerifier(repository, jdbcTemplate, clock, id -> id);
    }

    @Test
    @DisplayName("IT finalizeBatch() when a stale capture finalizes after the event was recaptured should keep the newer outcome")
    void finalizeBatch_whenStaleCaptureFinalizesAfterRecapture_shouldKeepNewerOutcome() {
        verifier.finalizeBatch_whenStaleCaptureFinalizesAfterRecapture_shouldKeepNewerOutcome();
    }

    @Test
    @DisplayName("IT recoverStuckBatch() when the recovering instance runs in another time zone should not recover a fresh capture")
    void recoverStuckBatch_whenRecoveringInstanceRunsInAnotherZone_shouldNotRecoverFreshCapture() {
        verifier.recoverStuckBatch_whenRecoveringInstanceRunsInAnotherZone_shouldNotRecoverFreshCapture();
    }
}
