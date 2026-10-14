package io.github.dmitriyiliyov.oncebox.core.outbox.dlq;

import io.github.dmitriyiliyov.oncebox.core.TestDatabaseClock;
import io.github.dmitriyiliyov.oncebox.core.outbox.EventStatus;
import io.github.dmitriyiliyov.oncebox.core.outbox.OutboxEvent;
import io.github.dmitriyiliyov.oncebox.core.utils.ResultSetMapper;
import io.github.dmitriyiliyov.oncebox.core.utils.SqlUuidHelper;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

public class OutboxDlqRepositoryVerifier {

    // well inside the 6000 seconds an expired event is aged by, and far beyond a fresh one
    private static final Duration TTL = Duration.ofSeconds(1000);

    private final OutboxDlqRepository repository;
    private final JdbcTemplate jdbcTemplate;
    private final SqlUuidHelper sqlUuidHelper;
    private final ResultSetMapper mapper;
    private final TestDatabaseClock clock;

    public OutboxDlqRepositoryVerifier(OutboxDlqRepository repository,
                                       JdbcTemplate jdbcTemplate,
                                       SqlUuidHelper sqlUuidHelper,
                                       ResultSetMapper mapper,
                                       TestDatabaseClock clock) {
        this.repository = repository;
        this.jdbcTemplate = jdbcTemplate;
        this.sqlUuidHelper = sqlUuidHelper;
        this.mapper = mapper;
        this.clock = clock;
    }

    public void saveBatch_whenEventIsMoved_shouldKeepItsTimesAndStampMovedAtByDatabaseClock() {
        Instant createdAt = Instant.parse("2026-01-01T10:00:00Z");
        Instant updatedAt = Instant.parse("2026-01-01T10:01:00Z");
        Instant nextRetryAt = Instant.parse("2026-01-01T10:05:00Z");
        OutboxDlqEvent event = OutboxDlqEvent.moved(new OutboxEvent(UUID.randomUUID(), EventStatus.FAILED,
                "ORDER_CREATED", "io.example.OrderCreated", "{}", 3, nextRetryAt, createdAt, updatedAt));

        repository.saveBatch(List.of(event));

        OutboxDlqEvent found = findById(event.getId()).orElseThrow();
        assertThat(found.getCreatedAt()).isEqualTo(createdAt);
        assertThat(found.getUpdatedAt()).isEqualTo(updatedAt);
        assertThat(found.getNextRetryAt()).isEqualTo(nextRetryAt);
        assertThat(clock.distanceFromNow("outbox_dlq_events", "moved_at", event.getId()))
                .isBetween(Duration.ofSeconds(-30), Duration.ofSeconds(30));
    }

    public void saveBatch_whenReadInAnotherJvmZone_shouldGiveBackTheSameMoments() {
        Instant createdAt = Instant.parse("2026-01-01T10:00:00Z");
        TimeZone original = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
            OutboxDlqEvent event = OutboxDlqEvent.moved(new OutboxEvent(UUID.randomUUID(), EventStatus.FAILED,
                    "ORDER_CREATED", "io.example.OrderCreated", "{}", 3, createdAt, createdAt, createdAt));
            repository.saveBatch(List.of(event));

            TimeZone.setDefault(TimeZone.getTimeZone("GMT+03:00"));
            OutboxDlqEvent found = findById(event.getId()).orElseThrow();

            assertThat(found.getCreatedAt()).isEqualTo(createdAt);
        } finally {
            TimeZone.setDefault(original);
        }
    }

    public void saveBatch_singleEvent_persistedCorrectly() {
        OutboxDlqEvent event = buildEvent(DlqStatus.MOVED);

        repository.saveBatch(List.of(event));

        Optional<OutboxDlqEvent> found = findById(event.getId());
        assertThat(found).isPresent();
        assertThat(found.get()).satisfies(e -> {
            assertThat(e.getId()).isEqualTo(event.getId());
            assertThat(e.getStatus()).isEqualTo(event.getStatus());
            assertThat(e.getDlqStatus()).isEqualTo(event.getDlqStatus());
            assertThat(e.getEventType()).isEqualTo(event.getEventType());
            assertThat(e.getPayloadType()).isEqualTo(event.getPayloadType());
            assertThat(e.getPayload()).isEqualTo(event.getPayload());
            assertThat(e.getRetryCount()).isEqualTo(event.getRetryCount());
        });
    }

    public void saveBatch_multipleEvents_allPersisted() {
        List<OutboxDlqEvent> events = List.of(
                buildEvent(DlqStatus.MOVED),
                buildEvent(DlqStatus.RESOLVED),
                buildEvent(DlqStatus.TO_RETRY)
        );

        repository.saveBatch(events);

        events.forEach(e -> assertThat(findById(e.getId())).isPresent());
    }

    public void deleteBatch_existingIds_deletedAndReturnsCount() {
        OutboxDlqEvent e1 = buildEvent(DlqStatus.MOVED);
        OutboxDlqEvent e2 = buildEvent(DlqStatus.MOVED);
        repository.saveBatch(List.of(e1, e2));

        int deleted = repository.deleteBatch(Set.of(e1.getId(), e2.getId()));

        assertThat(deleted).isEqualTo(2);
        assertThat(findById(e1.getId())).isEmpty();
        assertThat(findById(e2.getId())).isEmpty();
    }

    public void deleteBatch_emptyIds_returnsZero() {
        assertThat(repository.deleteBatch(Set.of())).isEqualTo(0);
    }

    public void deleteBatch_doesNotAffectOtherEvents() {
        OutboxDlqEvent target = buildEvent(DlqStatus.MOVED);
        OutboxDlqEvent unrelated = buildEvent(DlqStatus.MOVED);
        repository.saveBatch(List.of(target, unrelated));

        repository.deleteBatch(Set.of(target.getId()));

        assertThat(findById(unrelated.getId())).isPresent();
    }

    public void deleteBatchByStatusOlderThan_matches_deleted() {
        Instant past = Instant.now().minusSeconds(6000);
        OutboxDlqEvent e1 = buildEventWithUpdatedAt(DlqStatus.RESOLVED, past);
        OutboxDlqEvent e2 = buildEventWithUpdatedAt(DlqStatus.RESOLVED, past);
        repository.saveBatch(List.of(e1, e2));

        int deleted = repository.deleteBatchByStatusOlderThan(DlqStatus.RESOLVED, TTL, 10);

        assertThat(deleted).isEqualTo(2);
        assertThat(findById(e1.getId())).isEmpty();
        assertThat(findById(e2.getId())).isEmpty();
    }

    public void deleteBatchByStatusOlderThan_newerThanAge_notDeleted() {
        Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        OutboxDlqEvent e1 = buildEventWithUpdatedAt(DlqStatus.RESOLVED, now);
        repository.saveBatch(List.of(e1));

        int deleted = repository.deleteBatchByStatusOlderThan(DlqStatus.RESOLVED, TTL, 10);

        assertThat(deleted).isZero();
        assertThat(findById(e1.getId())).isPresent();
    }

    public void deleteBatchByStatusOlderThan_wrongStatus_notDeleted() {
        Instant past = Instant.now().minusSeconds(6000);
        OutboxDlqEvent e1 = buildEventWithUpdatedAt(DlqStatus.MOVED, past);
        repository.saveBatch(List.of(e1));

        int deleted = repository.deleteBatchByStatusOlderThan(DlqStatus.RESOLVED, TTL, 10);

        assertThat(deleted).isZero();
        assertThat(findById(e1.getId())).isPresent();
    }

    public void deleteBatchByStatusOlderThan_respectsBatchSize() {
        Instant past = Instant.now().minusSeconds(6000);
        OutboxDlqEvent e1 = buildEventWithUpdatedAt(DlqStatus.RESOLVED, past);
        OutboxDlqEvent e2 = buildEventWithUpdatedAt(DlqStatus.RESOLVED, past);
        OutboxDlqEvent e3 = buildEventWithUpdatedAt(DlqStatus.RESOLVED, past);
        repository.saveBatch(List.of(e1, e2, e3));

        int deleted = repository.deleteBatchByStatusOlderThan(DlqStatus.RESOLVED, TTL, 2);

        assertThat(deleted).isEqualTo(2);
        long remaining = List.of(e1, e2, e3).stream().filter(e -> findById(e.getId()).isPresent()).count();
        assertThat(remaining).isEqualTo(1);
    }

    public void deleteBatchByStatusOlderThan_noMatches_returnsZero() {
        int deleted = repository.deleteBatchByStatusOlderThan(DlqStatus.RESOLVED, TTL, 10);
        assertThat(deleted).isZero();
    }

    public OutboxDlqEvent buildEventWithUpdatedAt(DlqStatus dlqStatus, Instant updatedAt) {
        Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        return new OutboxDlqEvent(
                UUID.randomUUID(),
                EventStatus.FAILED,
                "ORDER_CREATED",
                "io.example.OrderCreated",
                "{\"orderId\":\"123\"}",
                3,
                now.plusSeconds(60),
                now,
                updatedAt,
                dlqStatus,
                now
        );
    }

    public OutboxDlqEvent buildEvent(DlqStatus dlqStatus) {
        return buildEventWithMovedAt(dlqStatus, Instant.now().truncatedTo(ChronoUnit.MILLIS));
    }

    public OutboxDlqEvent buildEventWithMovedAt(DlqStatus dlqStatus, Instant movedAt) {
        Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        return new OutboxDlqEvent(
                UUID.randomUUID(),
                EventStatus.FAILED,
                "ORDER_CREATED",
                "io.example.OrderCreated",
                "{\"orderId\":\"123\"}",
                3,
                now.plusSeconds(60),
                now,
                now,
                dlqStatus,
                movedAt
        );
    }

    public Optional<OutboxDlqEvent> findById(UUID id) {
        String sql = """
            SELECT id, status, dlq_status, event_type, payload_type, payload, retry_count, next_retry_at, created_at, updated_at, moved_at
            FROM outbox_dlq_events
            WHERE id = ?
        """;
        List<OutboxDlqEvent> results = jdbcTemplate.query(
                sql,
                ps -> sqlUuidHelper.setToPs(ps, 1, id),
                (rs, rowNum) -> mapper.toDlqEvent(rs)
        );
        return results.stream().findFirst();
    }
}
