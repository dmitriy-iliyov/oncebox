package io.github.dmitriyiliyov.oncebox.core.publisher;

import io.github.dmitriyiliyov.oncebox.core.publisher.domain.EventStatus;
import io.github.dmitriyiliyov.oncebox.core.publisher.domain.OutboxEvent;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The guarantees of a capture - a poller locking a batch - that hold only against the real engine: that a late
 * outcome of a capture that was taken away does not overwrite the newer one. Shared by the dialect modules.
 */
public class OutboxCaptureVerifier {

    private static final UUID EVENT_ID = UUID.fromString("0192f5a0-0000-7000-8000-00000000000a");
    private static final String EVENT_TYPE = "capture-verifier";
    private static final UUID CAPTURE_X = UUID.fromString("0192f5a0-0000-7000-8000-0000000000c1");
    private static final UUID CAPTURE_Y = UUID.fromString("0192f5a0-0000-7000-8000-0000000000c2");
    private static final Duration MAX_BATCH_PROCESSING_TIME = Duration.ofMinutes(5);

    private final OutboxRepository repository;
    private final JdbcTemplate jdbcTemplate;
    private final Clock clock;
    private final DefaultOutboxProcessorVerifier.IdPreparer idPreparer;

    public OutboxCaptureVerifier(OutboxRepository repository,
                                 JdbcTemplate jdbcTemplate,
                                 Clock clock,
                                 DefaultOutboxProcessorVerifier.IdPreparer idPreparer) {
        this.repository = repository;
        this.jdbcTemplate = jdbcTemplate;
        this.clock = clock;
        this.idPreparer = idPreparer;
    }

    /**
     * Poller X captures the event and stalls; recovery hands it back; poller Y captures it, sends it and records
     * PROCESSED; then X records its own, stale outcome - a failure with no retries left.
     */
    public void finalizeBatch_whenStaleCaptureFinalizesAfterRecapture_shouldKeepNewerOutcome() {
        repository.save(new OutboxEvent(EVENT_ID, EVENT_TYPE, "java.lang.String", "\"payload\"",
                clock.instant().minusSeconds(1)));
        OutboxManager poller = new DefaultOutboxManager(repository, clock);
        OutboxManager recovery = new DefaultOutboxManager(repository, Clock.offset(clock, Duration.ofMinutes(10)));

        List<OutboxEvent> capturedByX = poller.loadBatch(EVENT_TYPE, 10, CAPTURE_X);
        assertThat(recovery.recoverStuckBatch(MAX_BATCH_PROCESSING_TIME, 100)).isEqualTo(1);
        List<OutboxEvent> capturedByY = poller.loadBatch(EVENT_TYPE, 10, CAPTURE_Y);
        assertThat(capturedByY).extracting(OutboxEvent::getId).containsExactly(EVENT_ID);
        poller.finalizeBatch(capturedByY, Set.of(EVENT_ID), Set.of(), 3, retryCount -> clock.instant(), CAPTURE_Y);

        poller.finalizeBatch(capturedByX, Set.of(), Set.of(EVENT_ID), 0, retryCount -> clock.instant(), CAPTURE_X);

        assertThat(status()).isEqualTo(EventStatus.PROCESSED);
    }

    private EventStatus status() {
        String status = jdbcTemplate.queryForObject(
                "SELECT status FROM outbox_events WHERE id = ?", String.class, idPreparer.prepare(EVENT_ID)
        );
        return EventStatus.valueOf(status);
    }
}
