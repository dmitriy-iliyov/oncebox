package io.github.dmitriyiliyov.oncebox.mysql;

import io.github.dmitriyiliyov.oncebox.core.publisher.OutboxCaptureVerifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.nio.ByteBuffer;
import java.time.Clock;

@Transactional
class MySqlOutboxCaptureIntegrationTests extends BaseMySqlIntegrationTests {

    @Autowired
    private MySqlOutboxRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private Clock clock;

    private OutboxCaptureVerifier verifier;

    @BeforeEach
    void setUp() {
        verifier = new OutboxCaptureVerifier(repository, jdbcTemplate, clock, id -> {
            ByteBuffer bb = ByteBuffer.allocate(16);
            bb.putLong(id.getMostSignificantBits());
            bb.putLong(id.getLeastSignificantBits());
            return bb.array();
        });
    }

    @Test
    @DisplayName("IT finalizeBatch() when a stale capture finalizes after the event was recaptured should keep the newer outcome")
    void finalizeBatch_whenStaleCaptureFinalizesAfterRecapture_shouldKeepNewerOutcome() {
        verifier.finalizeBatch_whenStaleCaptureFinalizesAfterRecapture_shouldKeepNewerOutcome();
    }
}
