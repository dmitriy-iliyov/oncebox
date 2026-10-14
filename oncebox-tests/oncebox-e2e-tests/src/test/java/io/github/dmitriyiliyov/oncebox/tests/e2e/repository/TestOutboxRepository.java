package io.github.dmitriyiliyov.oncebox.tests.e2e.repository;

import io.github.dmitriyiliyov.oncebox.core.outbox.EventStatus;
import io.github.dmitriyiliyov.oncebox.core.outbox.dlq.DlqStatus;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

/**
 * Direct database access used by e2e tests to inspect outbox state and to simulate
 * the passage of time by shifting event timestamps into the past.
 */
public interface TestOutboxRepository {

    int countEvents();

    int countEventsByStatus(EventStatus status);

    List<UUID> findEventIdsByStatus(EventStatus status);

    List<UUID> findEventIds();

    int findRetryCount(UUID id);

    UUID insertEvent(String eventType, EventStatus status, String payloadType, String payload, Duration age);

    void shiftEventTimestamps(UUID id, Duration shift);

    int countDlqEvents();

    int countDlqEventsByStatus(DlqStatus status);

    List<UUID> findDlqEventIdsByStatus(DlqStatus status);

    UUID insertDlqEvent(String eventType, DlqStatus dlqStatus, String payloadType, String payload);

    void shiftDlqEventTimestamps(UUID id, Duration shift);

    boolean isConsumed(UUID eventId);

    void shiftConsumedEventTimestamp(UUID eventId, Duration shift);

    void saveProducedBusiness(UUID verifyId);

    int countProducedBusiness(UUID verifyId);

    void saveConsumedBusiness(UUID verifyId);

    int countConsumedBusiness(UUID verifyId);

    int countConsumedBusiness();

    void truncateAll();
}
