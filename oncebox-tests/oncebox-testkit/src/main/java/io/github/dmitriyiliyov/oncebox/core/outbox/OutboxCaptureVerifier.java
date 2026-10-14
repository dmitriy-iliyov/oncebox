package io.github.dmitriyiliyov.oncebox.core.outbox;

import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The guarantees of a capture - a poller locking a batch - that hold only against the real engine: that a late
 * outcome of a capture that was taken away does not overwrite the newer one, and that a capture is not taken
 * for stuck by an instance running in another time zone. Shared by the dialect modules.
 */
public class OutboxCaptureVerifier {

    private static final UUID EVENT_ID = UUID.fromString("0192f5a0-0000-7000-8000-00000000000a");
    private static final String EVENT_TYPE = "capture-verifier";
    private static final UUID CAPTURE_X = UUID.fromString("0192f5a0-0000-7000-8000-0000000000c1");
    private static final UUID CAPTURE_Y = UUID.fromString("0192f5a0-0000-7000-8000-0000000000c2");
    private static final Duration MAX_BATCH_PROCESSING_TIME = Duration.ofMinutes(5);

    private final OutboxRepository repository;
    private final JdbcTemplate jdbcTemplate;
    private final DefaultOutboxProcessorVerifier.IdPreparer idPreparer;

    public OutboxCaptureVerifier(OutboxRepository repository,
                                 JdbcTemplate jdbcTemplate,
                                 DefaultOutboxProcessorVerifier.IdPreparer idPreparer) {
        this.repository = repository;
        this.jdbcTemplate = jdbcTemplate;
        this.idPreparer = idPreparer;
    }

    /**
     * Poller X captures the event and stalls; recovery hands it back; poller Y captures it, sends it and records
     * PROCESSED; then X records its own, stale outcome - a failure with no retries left.
     */
    public void finalizeBatch_whenStaleCaptureFinalizesAfterRecapture_shouldKeepNewerOutcome() {
        repository.save(new OutboxEvent(EVENT_ID, EVENT_TYPE, "java.lang.String", "\"payload\""));
        OutboxManager manager = new DefaultOutboxManager(repository);

        List<OutboxEvent> capturedByX = manager.loadBatch(EVENT_TYPE, 10, CAPTURE_X);
        // a zero processing time makes the capture of X count as stuck right away
        assertThat(manager.recoverStuckBatch(Duration.ZERO, 100)).isEqualTo(1);
        List<OutboxEvent> capturedByY = manager.loadBatch(EVENT_TYPE, 10, CAPTURE_Y);
        assertThat(capturedByY).extracting(OutboxEvent::getId).containsExactly(EVENT_ID);
        manager.finalizeBatch(capturedByY, Set.of(EVENT_ID), Set.of(), 3, retryCount -> Duration.ZERO, CAPTURE_Y);

        manager.finalizeBatch(capturedByX, Set.of(), Set.of(EVENT_ID), 0, retryCount -> Duration.ZERO, CAPTURE_X);

        assertThat(status()).isEqualTo(EventStatus.PROCESSED);
    }

    /**
     * The event is captured by an instance whose JVM runs in UTC and inspected for recovery right away by an
     * instance running three hours ahead.
     */
    public void recoverStuckBatch_whenRecoveringInstanceRunsInAnotherZone_shouldNotRecoverFreshCapture() {
        TimeZone original = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
            repository.save(new OutboxEvent(EVENT_ID, EVENT_TYPE, "java.lang.String", "\"payload\""));
            new DefaultOutboxManager(repository).loadBatch(EVENT_TYPE, 10, CAPTURE_X);

            TimeZone.setDefault(TimeZone.getTimeZone("GMT+03:00"));
            int recovered = new DefaultOutboxManager(repository).recoverStuckBatch(MAX_BATCH_PROCESSING_TIME, 100);

            assertThat(recovered).isZero();
            assertThat(status()).isEqualTo(EventStatus.IN_PROCESS);
        } finally {
            TimeZone.setDefault(original);
        }
    }

    private EventStatus status() {
        String status = jdbcTemplate.queryForObject(
                "SELECT status FROM outbox_events WHERE id = ?", String.class, idPreparer.prepare(EVENT_ID)
        );
        return EventStatus.valueOf(status);
    }
}
