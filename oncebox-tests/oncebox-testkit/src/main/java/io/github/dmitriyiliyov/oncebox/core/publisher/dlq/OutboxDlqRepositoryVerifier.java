package io.github.dmitriyiliyov.oncebox.core.publisher.dlq;

import io.github.dmitriyiliyov.oncebox.core.publisher.domain.EventStatus;
import io.github.dmitriyiliyov.oncebox.core.utils.ResultSetMapper;
import io.github.dmitriyiliyov.oncebox.core.utils.SqlIdHelper;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class OutboxDlqRepositoryVerifier {

    private final OutboxDlqRepository repository;
    private final JdbcTemplate jdbcTemplate;
    private final SqlIdHelper sqlIdHelper;
    private final ResultSetMapper mapper;

    public OutboxDlqRepositoryVerifier(OutboxDlqRepository repository,
                                JdbcTemplate jdbcTemplate,
                                SqlIdHelper sqlIdHelper,
                                ResultSetMapper mapper) {
        this.repository = repository;
        this.jdbcTemplate = jdbcTemplate;
        this.sqlIdHelper = sqlIdHelper;
        this.mapper = mapper;
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

    public void deleteBatchByStatusAndThreshold_matches_deleted() {
        Instant now = Instant.now();
        OutboxDlqEvent e1 = buildEventWithUpdatedAt(DlqStatus.RESOLVED, now.minusSeconds(6000));
        OutboxDlqEvent e2 = buildEventWithUpdatedAt(DlqStatus.RESOLVED, now.minusSeconds(6000));
        repository.saveBatch(List.of(e1, e2));

        int deleted = repository.deleteBatchByStatusAndThreshold(DlqStatus.RESOLVED, now, 10);

        assertThat(deleted).isEqualTo(2);
        assertThat(findById(e1.getId())).isEmpty();
        assertThat(findById(e2.getId())).isEmpty();
    }

    public void deleteBatchByStatusAndThreshold_newerThanThreshold_notDeleted() {
        Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        OutboxDlqEvent e1 = buildEventWithUpdatedAt(DlqStatus.RESOLVED, now);
        repository.saveBatch(List.of(e1));

        int deleted = repository.deleteBatchByStatusAndThreshold(DlqStatus.RESOLVED, now.minusSeconds(1000), 10);

        assertThat(deleted).isZero();
        assertThat(findById(e1.getId())).isPresent();
    }

    public void deleteBatchByStatusAndThreshold_wrongStatus_notDeleted() {
        Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        OutboxDlqEvent e1 = buildEventWithUpdatedAt(DlqStatus.MOVED, now.minusSeconds(1000));
        repository.saveBatch(List.of(e1));

        int deleted = repository.deleteBatchByStatusAndThreshold(DlqStatus.RESOLVED, now, 10);

        assertThat(deleted).isZero();
        assertThat(findById(e1.getId())).isPresent();
    }

    public void deleteBatchByStatusAndThreshold_respectsBatchSize() {
        Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        Instant past = now.minusSeconds(1000);
        OutboxDlqEvent e1 = buildEventWithUpdatedAt(DlqStatus.RESOLVED, past);
        OutboxDlqEvent e2 = buildEventWithUpdatedAt(DlqStatus.RESOLVED, past);
        OutboxDlqEvent e3 = buildEventWithUpdatedAt(DlqStatus.RESOLVED, past);
        repository.saveBatch(List.of(e1, e2, e3));

        int deleted = repository.deleteBatchByStatusAndThreshold(DlqStatus.RESOLVED, now, 2);

        assertThat(deleted).isEqualTo(2);
        long remaining = List.of(e1, e2, e3).stream().filter(e -> findById(e.getId()).isPresent()).count();
        assertThat(remaining).isEqualTo(1);
    }

    public void deleteBatchByStatusAndThreshold_noMatches_returnsZero() {
        int deleted = repository.deleteBatchByStatusAndThreshold(DlqStatus.RESOLVED, Instant.now(), 10);
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
                ps -> sqlIdHelper.setIdToPs(ps, 1, id),
                (rs, rowNum) -> mapper.toDlqEvent(rs)
        );
        return results.stream().findFirst();
    }
}
