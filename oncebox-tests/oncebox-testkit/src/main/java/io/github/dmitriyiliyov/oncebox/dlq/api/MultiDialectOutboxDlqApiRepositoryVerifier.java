package io.github.dmitriyiliyov.oncebox.dlq.api;

import io.github.dmitriyiliyov.oncebox.core.publisher.dlq.DlqStatus;
import io.github.dmitriyiliyov.oncebox.core.publisher.dlq.OutboxDlqEvent;
import io.github.dmitriyiliyov.oncebox.core.publisher.domain.EventStatus;
import io.github.dmitriyiliyov.oncebox.core.utils.RepositoryUtils;
import io.github.dmitriyiliyov.oncebox.core.utils.ResultSetMapper;
import io.github.dmitriyiliyov.oncebox.core.utils.SqlIdHelper;
import io.github.dmitriyiliyov.oncebox.dlq.api.exception.InvalidDlqFilterException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;

public class MultiDialectOutboxDlqApiRepositoryVerifier {

    private final OutboxDlqApiRepository repository;
    private final JdbcTemplate jdbcTemplate;
    private final SqlIdHelper idHelper;
    private final ResultSetMapper mapper;

    public MultiDialectOutboxDlqApiRepositoryVerifier(
            OutboxDlqApiRepository repository,
            JdbcTemplate jdbcTemplate,
            SqlIdHelper idHelper,
            ResultSetMapper mapper
    ) {
        this.repository = repository;
        this.jdbcTemplate = jdbcTemplate;
        this.idHelper = idHelper;
        this.mapper = mapper;
    }

    public void findById_notExisting_returnsEmpty() {
        assertThat(repository.findById(UUID.randomUUID())).isEmpty();
    }

    public void findById_existingId_returnsEvent() {
        OutboxDlqEvent event = buildEvent(DlqStatus.MOVED);
        saveBatch(List.of(event));

        assertThat(repository.findById(event.getId()))
                .isPresent()
                .get()
                .extracting(OutboxDlqEvent::getId, OutboxDlqEvent::getDlqStatus, OutboxDlqEvent::getEventType)
                .containsExactly(event.getId(), DlqStatus.MOVED, "ORDER_CREATED");
    }

    public void findByIdForUpdate_notExisting_returnsEmpty() {
        assertThat(repository.findByIdForUpdate(UUID.randomUUID())).isEmpty();
    }

    public void findByIdForUpdate_existingId_returnsEvent() {
        OutboxDlqEvent event = buildEvent(DlqStatus.MOVED);
        saveBatch(List.of(event));

        assertThat(repository.findByIdForUpdate(event.getId()))
                .isPresent()
                .get()
                .extracting(OutboxDlqEvent::getId, OutboxDlqEvent::getDlqStatus, OutboxDlqEvent::getEventType)
                .containsExactly(event.getId(), DlqStatus.MOVED, "ORDER_CREATED");
    }

    public void findBatch_byStatus_returnsOnlyMatchingStatus() {
        saveBatch(List.of(
                buildEvent(DlqStatus.MOVED),
                buildEvent(DlqStatus.MOVED),
                buildEvent(DlqStatus.RESOLVED)
        ));

        DlqFilter filter = DlqFilter.builder().status(DlqStatus.MOVED).build();
        List<OutboxDlqEvent> result = repository.findBatch(filter, 0, 10);

        assertThat(result)
                .isNotEmpty()
                .extracting(OutboxDlqEvent::getDlqStatus)
                .containsOnly(DlqStatus.MOVED);
    }

    public void findBatch_pagination_page1AndPage2DoNotOverlap() {
        List<OutboxDlqEvent> events = IntStream.range(0, 5)
                .mapToObj(i -> buildEvent(DlqStatus.MOVED))
                .toList();
        saveBatch(events);

        DlqFilter filter = DlqFilter.builder().status(DlqStatus.MOVED).build();
        List<UUID> page1 = repository.findBatch(filter, 0, 2)
                .stream().map(OutboxDlqEvent::getId).toList();
        List<UUID> page2 = repository.findBatch(filter, 1, 2)
                .stream().map(OutboxDlqEvent::getId).toList();

        assertThat(page1).hasSize(2);
        assertThat(page2).hasSize(2);
        assertThat(page1).doesNotContainAnyElementsOf(page2);
    }

    public void findBatch_noMatches_returnsEmpty() {
        DlqFilter filter = DlqFilter.builder().status(DlqStatus.RESOLVED).build();
        assertThat(repository.findBatch(filter, 1, 10)).isEmpty();
    }

    public void findBatch_orderedByMovedAt() {
        Instant base = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        OutboxDlqEvent first = buildEventWithMovedAt(DlqStatus.MOVED, base);
        OutboxDlqEvent second = buildEventWithMovedAt(DlqStatus.MOVED, base.plusSeconds(10));
        OutboxDlqEvent third = buildEventWithMovedAt(DlqStatus.MOVED, base.plusSeconds(20));
        saveBatch(List.of(third, first, second));

        DlqFilter filter = DlqFilter.builder().status(DlqStatus.MOVED).build();
        List<OutboxDlqEvent> result = repository.findBatch(filter, 0, 10);

        assertThat(result)
                .extracting(OutboxDlqEvent::getId)
                .containsExactly(first.getId(), second.getId(), third.getId());
    }

    public void findBatch_byEventType_returnsOnlyMatchingType() {
        saveBatch(List.of(
                buildEvent(DlqStatus.MOVED, "ORDER_CREATED"),
                buildEvent(DlqStatus.RESOLVED, "ORDER_CREATED"),
                buildEvent(DlqStatus.MOVED, "PAYMENT_PROCESSED")
        ));

        DlqFilter filter = DlqFilter.builder().eventType("ORDER_CREATED").build();
        List<OutboxDlqEvent> result = repository.findBatch(filter, 0, 10);

        assertThat(result)
                .hasSize(2)
                .extracting(OutboxDlqEvent::getEventType)
                .containsOnly("ORDER_CREATED");
    }

    public void findBatch_byStatusAndEventType_returnsOnlyMatching() {
        saveBatch(List.of(
                buildEvent(DlqStatus.MOVED, "ORDER_CREATED"),
                buildEvent(DlqStatus.MOVED, "ORDER_CREATED"),
                buildEvent(DlqStatus.RESOLVED, "ORDER_CREATED"),
                buildEvent(DlqStatus.MOVED, "PAYMENT_PROCESSED")
        ));

        DlqFilter filter = DlqFilter.builder()
                .status(DlqStatus.MOVED)
                .eventType("ORDER_CREATED")
                .build();
        List<OutboxDlqEvent> result = repository.findBatch(filter, 0, 10);

        assertThat(result)
                .hasSize(2)
                .allSatisfy(event -> {
                    assertThat(event.getDlqStatus()).isEqualTo(DlqStatus.MOVED);
                    assertThat(event.getEventType()).isEqualTo("ORDER_CREATED");
                });
    }

    public void findBatch_noFilters_returnsAll() {
        saveBatch(List.of(
                buildEvent(DlqStatus.MOVED),
                buildEvent(DlqStatus.RESOLVED),
                buildEvent(DlqStatus.IN_PROCESS),
                buildEvent(DlqStatus.MOVED),
                buildEvent(DlqStatus.RESOLVED),
                buildEvent(DlqStatus.IN_PROCESS),
                buildEvent(DlqStatus.MOVED),
                buildEvent(DlqStatus.RESOLVED),
                buildEvent(DlqStatus.IN_PROCESS),
                buildEvent(DlqStatus.MOVED),
                buildEvent(DlqStatus.RESOLVED),
                buildEvent(DlqStatus.IN_PROCESS)
        ));

        DlqFilter filter = DlqFilter.builder().build();
        List<OutboxDlqEvent> result = repository.findBatch(filter, 0, 10);

        assertThat(result).hasSize(10);
    }

    public void count_emptyTable_returnsZero() {
        assertThat(repository.count(DlqFilter.builder().build())).isEqualTo(0);
    }

    public void count_withEvents_returnsTotalCount() {
        saveBatch(List.of(
                buildEvent(DlqStatus.MOVED),
                buildEvent(DlqStatus.RESOLVED),
                buildEvent(DlqStatus.IN_PROCESS)
        ));

        assertThat(repository.count(DlqFilter.builder().build())).isEqualTo(3);
    }

    public void count_byStatus_noMatches_returnsZero() {
        saveBatch(List.of(buildEvent(DlqStatus.RESOLVED)));

        DlqFilter filter = DlqFilter.builder().status(DlqStatus.MOVED).build();
        assertThat(repository.count(filter)).isEqualTo(0);
    }

    public void count_byStatus_withMatches_returnsOnlyMatchingCount() {
        saveBatch(List.of(
                buildEvent(DlqStatus.MOVED),
                buildEvent(DlqStatus.MOVED),
                buildEvent(DlqStatus.RESOLVED)
        ));

        assertThat(repository.count(DlqFilter.builder().status(DlqStatus.MOVED).build())).isEqualTo(2);
        assertThat(repository.count(DlqFilter.builder().status(DlqStatus.RESOLVED).build())).isEqualTo(1);
        assertThat(repository.count(DlqFilter.builder().status(DlqStatus.IN_PROCESS).build())).isEqualTo(0);
    }

    public void count_byEventType_noMatches_returnsZero() {
        saveBatch(List.of(buildEvent(DlqStatus.MOVED, "ORDER_CREATED")));

        DlqFilter filter = DlqFilter.builder().eventType("PAYMENT_PROCESSED").build();
        assertThat(repository.count(filter)).isEqualTo(0);
    }

    public void count_byEventType_withMatches_returnsOnlyMatchingCount() {
        saveBatch(List.of(
                buildEvent(DlqStatus.MOVED, "ORDER_CREATED"),
                buildEvent(DlqStatus.RESOLVED, "ORDER_CREATED"),
                buildEvent(DlqStatus.MOVED, "PAYMENT_PROCESSED")
        ));

        assertThat(repository.count(DlqFilter.builder().eventType("ORDER_CREATED").build())).isEqualTo(2);
        assertThat(repository.count(DlqFilter.builder().eventType("PAYMENT_PROCESSED").build())).isEqualTo(1);
    }

    public void count_byStatusAndEventType_noMatches_returnsZero() {
        saveBatch(List.of(buildEvent(DlqStatus.MOVED, "ORDER_CREATED")));

        assertThat(repository.count(DlqFilter.builder().status(DlqStatus.RESOLVED).eventType("ORDER_CREATED").build())).isEqualTo(0);
        assertThat(repository.count(DlqFilter.builder().status(DlqStatus.MOVED).eventType("PAYMENT_PROCESSED").build())).isEqualTo(0);
    }

    public void count_byStatusAndEventType_withMatches_returnsOnlyMatchingCount() {
        saveBatch(List.of(
                buildEvent(DlqStatus.MOVED, "ORDER_CREATED"),
                buildEvent(DlqStatus.MOVED, "ORDER_CREATED"),
                buildEvent(DlqStatus.RESOLVED, "ORDER_CREATED"),
                buildEvent(DlqStatus.MOVED, "PAYMENT_PROCESSED")
        ));

        assertThat(repository.count(DlqFilter.builder().status(DlqStatus.MOVED).eventType("ORDER_CREATED").build())).isEqualTo(2);
        assertThat(repository.count(DlqFilter.builder().status(DlqStatus.RESOLVED).eventType("ORDER_CREATED").build())).isEqualTo(1);
        assertThat(repository.count(DlqFilter.builder().status(DlqStatus.MOVED).eventType("PAYMENT_PROCESSED").build())).isEqualTo(1);
        assertThat(repository.count(DlqFilter.builder().status(DlqStatus.IN_PROCESS).eventType("ORDER_CREATED").build())).isEqualTo(0);
    }

    public void updateStatus_existingEvent_statusChanged() {
        OutboxDlqEvent event = buildEvent(DlqStatus.MOVED);
        saveBatch(List.of(event));

        repository.updateStatus(event.getId(), DlqStatus.RESOLVED);

        assertThat(repository.findById(event.getId()))
                .isPresent()
                .get()
                .extracting(OutboxDlqEvent::getDlqStatus)
                .isEqualTo(DlqStatus.RESOLVED);
    }

    public void updateStatus_notExistingId_doesNotThrow() {
        assertThatCode(() -> repository.updateStatus(UUID.randomUUID(), DlqStatus.RESOLVED))
                .doesNotThrowAnyException();
    }

    public void updateBatchStatus_multipleIds_allUpdated() {
        OutboxDlqEvent e1 = buildEvent(DlqStatus.MOVED);
        OutboxDlqEvent e2 = buildEvent(DlqStatus.MOVED);
        saveBatch(List.of(e1, e2));

        DlqFilter filter = DlqFilter.builder().status(DlqStatus.RESOLVED).ids(Set.of(e1.getId(), e2.getId())).build();
        repository.updateBatchStatus(filter, DlqStatus.IN_PROCESS);

        assertThat(repository.findById(e1.getId()).get().getDlqStatus()).isEqualTo(DlqStatus.RESOLVED);
        assertThat(repository.findById(e2.getId()).get().getDlqStatus()).isEqualTo(DlqStatus.RESOLVED);
    }

    public void updateBatchStatus_doesNotAffectOtherEvents() {
        OutboxDlqEvent target = buildEvent(DlqStatus.MOVED);
        OutboxDlqEvent unrelated = buildEvent(DlqStatus.MOVED);
        saveBatch(List.of(target, unrelated));

        DlqFilter filter = DlqFilter.builder().status(DlqStatus.RESOLVED).ids(Set.of(target.getId())).build();
        repository.updateBatchStatus(filter, DlqStatus.IN_PROCESS);

        assertThat(repository.findById(unrelated.getId()).get().getDlqStatus())
                .isEqualTo(DlqStatus.MOVED);
    }

    public void updateBatchStatus_emptyIds_throwsException() {
        DlqFilter filter = DlqFilter.builder().status(DlqStatus.RESOLVED).ids(Set.of()).build();
        assertThatThrownBy(() -> repository.updateBatchStatus(filter, DlqStatus.IN_PROCESS))
                .isInstanceOf(InvalidDlqFilterException.class);
    }

    public void updateBatchStatus_skipsInProcess() {
        OutboxDlqEvent movable = buildEvent(DlqStatus.MOVED);
        OutboxDlqEvent locked = buildEvent(DlqStatus.IN_PROCESS);
        saveBatch(List.of(movable, locked));

        DlqFilter filter = DlqFilter.builder().status(DlqStatus.RESOLVED).ids(Set.of(movable.getId(), locked.getId())).build();
        repository.updateBatchStatus(filter, DlqStatus.IN_PROCESS);

        assertThat(repository.findById(movable.getId()).get().getDlqStatus())
                .isEqualTo(DlqStatus.RESOLVED);

        assertThat(repository.findById(locked.getId()).get().getDlqStatus())
                .isEqualTo(DlqStatus.IN_PROCESS);
    }

    public void updateBatchStatus_byEventType_updatesOnlyMatchingType() {
        OutboxDlqEvent e1 = buildEvent(DlqStatus.MOVED, "TYPE_A");
        OutboxDlqEvent e2 = buildEvent(DlqStatus.MOVED, "TYPE_B");
        saveBatch(List.of(e1, e2));

        DlqFilter filter = DlqFilter.builder().status(DlqStatus.RESOLVED).eventType("TYPE_A").build();
        repository.updateBatchStatus(filter, DlqStatus.IN_PROCESS);

        assertThat(repository.findById(e1.getId()).get().getDlqStatus())
                .isEqualTo(DlqStatus.RESOLVED);

        assertThat(repository.findById(e2.getId()).get().getDlqStatus())
                .isEqualTo(DlqStatus.MOVED);
    }

    public void updateBatchStatus_byEventType_skipsInProcess() {
        OutboxDlqEvent movable = buildEvent(DlqStatus.MOVED, "TYPE_A");
        OutboxDlqEvent locked = buildEvent(DlqStatus.IN_PROCESS, "TYPE_A");
        saveBatch(List.of(movable, locked));

        DlqFilter filter = DlqFilter.builder().status(DlqStatus.RESOLVED).eventType("TYPE_A").build();
        repository.updateBatchStatus(filter, DlqStatus.IN_PROCESS);

        assertThat(repository.findById(movable.getId()).get().getDlqStatus())
                .isEqualTo(DlqStatus.RESOLVED);

        assertThat(repository.findById(locked.getId()).get().getDlqStatus())
                .isEqualTo(DlqStatus.IN_PROCESS);
    }

    public void updateBatchStatus_returnsActualUpdatedCount() {
        OutboxDlqEvent e1 = buildEvent(DlqStatus.MOVED);
        OutboxDlqEvent e2 = buildEvent(DlqStatus.MOVED);
        OutboxDlqEvent e3 = buildEvent(DlqStatus.IN_PROCESS);
        saveBatch(List.of(e1, e2, e3));

        DlqFilter filter = DlqFilter.builder()
                .status(DlqStatus.RESOLVED)
                .ids(Set.of(e1.getId(), e2.getId(), e3.getId()))
                .build();
        int updated = repository.updateBatchStatus(filter, DlqStatus.IN_PROCESS);

        assertThat(updated).isEqualTo(2);
    }

    public void updateBatchStatus_byEventType_returnsActualUpdatedCount() {
        OutboxDlqEvent e1 = buildEvent(DlqStatus.MOVED, "TYPE_A");
        OutboxDlqEvent e2 = buildEvent(DlqStatus.MOVED, "TYPE_A");
        OutboxDlqEvent e3 = buildEvent(DlqStatus.MOVED, "TYPE_B");
        saveBatch(List.of(e1, e2, e3));

        DlqFilter filter = DlqFilter.builder()
                .status(DlqStatus.RESOLVED)
                .eventType("TYPE_A")
                .build();
        int updated = repository.updateBatchStatus(filter, DlqStatus.IN_PROCESS);

        assertThat(updated).isEqualTo(2);
    }

    public void updateBatchStatus_neitherIdsNorEventType_throwsException() {
        DlqFilter filter = DlqFilter.builder().status(DlqStatus.RESOLVED).build();
        assertThatThrownBy(() -> repository.updateBatchStatus(filter, DlqStatus.IN_PROCESS))
                .isInstanceOf(InvalidDlqFilterException.class);
    }

    public void updateBatchStatus_withoutStatus_throwsException() {
        DlqFilter filter = DlqFilter.builder().eventType("ORDER_CREATED").build();
        assertThatThrownBy(() -> repository.updateBatchStatus(filter, DlqStatus.IN_PROCESS))
                .isInstanceOf(InvalidDlqFilterException.class);
    }

    public void deleteBatch_skipsInProcess() {
        OutboxDlqEvent deletable = buildEvent(DlqStatus.MOVED);
        OutboxDlqEvent locked = buildEvent(DlqStatus.IN_PROCESS);
        saveBatch(List.of(deletable, locked));

        DlqFilter filter = DlqFilter.builder().ids(Set.of(deletable.getId(), locked.getId())).build();
        repository.deleteBatch(filter, DlqStatus.IN_PROCESS);

        assertThat(repository.findById(deletable.getId())).isEmpty();
        assertThat(repository.findById(locked.getId())).isPresent();
    }

    public void deleteById_existingEvent_deletedAndReturnsOne() {
        OutboxDlqEvent event = buildEvent(DlqStatus.MOVED);
        saveBatch(List.of(event));

        int deleted = repository.deleteById(event.getId());

        assertThat(deleted).isEqualTo(1);
        assertThat(repository.findById(event.getId())).isEmpty();
    }

    public void deleteBatch_byEventType_deletesOnlyMatchingType() {
        OutboxDlqEvent e1 = buildEvent(DlqStatus.MOVED, "TYPE_A");
        OutboxDlqEvent e2 = buildEvent(DlqStatus.MOVED, "TYPE_B");
        saveBatch(List.of(e1, e2));

        DlqFilter filter = DlqFilter.builder().eventType("TYPE_A").build();
        repository.deleteBatch(filter, DlqStatus.IN_PROCESS);

        assertThat(repository.findById(e1.getId())).isEmpty();
        assertThat(repository.findById(e2.getId())).isPresent();
    }

    public void deleteBatch_byEventType_skipsInProcess() {
        OutboxDlqEvent deletable = buildEvent(DlqStatus.MOVED, "TYPE_A");
        OutboxDlqEvent locked = buildEvent(DlqStatus.IN_PROCESS, "TYPE_A");
        saveBatch(List.of(deletable, locked));

        DlqFilter filter = DlqFilter.builder().eventType("TYPE_A").build();
        repository.deleteBatch(filter, DlqStatus.IN_PROCESS);

        assertThat(repository.findById(deletable.getId())).isEmpty();
        assertThat(repository.findById(locked.getId())).isPresent();
    }

    public void deleteById_notExisting_returnsZero() {
        assertThat(repository.deleteById(UUID.randomUUID())).isEqualTo(0);
    }

    public void deleteBatch_existingIds_deletedAndReturnsCount() {
        OutboxDlqEvent e1 = buildEvent(DlqStatus.MOVED);
        OutboxDlqEvent e2 = buildEvent(DlqStatus.MOVED);
        saveBatch(List.of(e1, e2));

        DlqFilter filter = DlqFilter.builder().ids(Set.of(e1.getId(), e2.getId())).build();
        int deleted = repository.deleteBatch(filter, DlqStatus.IN_PROCESS);

        assertThat(deleted).isEqualTo(2);
        assertThat(repository.findById(e1.getId())).isEmpty();
        assertThat(repository.findById(e2.getId())).isEmpty();
    }

    public void deleteBatch_emptyIds_throwsException() {
        DlqFilter filter = DlqFilter.builder().ids(Set.of()).build();
        assertThatThrownBy(() -> repository.deleteBatch(filter, DlqStatus.IN_PROCESS))
                .isInstanceOf(InvalidDlqFilterException.class);
    }

    public void deleteBatch_doesNotAffectOtherEvents() {
        OutboxDlqEvent target = buildEvent(DlqStatus.MOVED);
        OutboxDlqEvent unrelated = buildEvent(DlqStatus.MOVED);
        saveBatch(List.of(target, unrelated));

        DlqFilter filter = DlqFilter.builder().ids(Set.of(target.getId())).build();
        repository.deleteBatch(filter, DlqStatus.IN_PROCESS);

        assertThat(repository.findById(unrelated.getId())).isPresent();
    }

    public void deleteBatch_returnsActualDeletedCount() {
        OutboxDlqEvent e1 = buildEvent(DlqStatus.MOVED);
        OutboxDlqEvent e2 = buildEvent(DlqStatus.MOVED);
        OutboxDlqEvent e3 = buildEvent(DlqStatus.IN_PROCESS);
        saveBatch(List.of(e1, e2, e3));

        DlqFilter filter = DlqFilter.builder()
                .ids(Set.of(e1.getId(), e2.getId(), e3.getId()))
                .build();
        int deleted = repository.deleteBatch(filter, DlqStatus.IN_PROCESS);

        assertThat(deleted).isEqualTo(2);
    }

    public void deleteBatch_byEventType_returnsActualDeletedCount() {
        OutboxDlqEvent e1 = buildEvent(DlqStatus.MOVED, "TYPE_A");
        OutboxDlqEvent e2 = buildEvent(DlqStatus.MOVED, "TYPE_A");
        OutboxDlqEvent e3 = buildEvent(DlqStatus.MOVED, "TYPE_B");
        saveBatch(List.of(e1, e2, e3));

        DlqFilter filter = DlqFilter.builder().eventType("TYPE_A").build();
        int deleted = repository.deleteBatch(filter, DlqStatus.IN_PROCESS);

        assertThat(deleted).isEqualTo(2);
    }

    public void deleteBatch_neitherIdsNorEventType_throwsException() {
        DlqFilter filter = DlqFilter.builder().build();
        assertThatThrownBy(() -> repository.deleteBatch(filter, DlqStatus.IN_PROCESS))
                .isInstanceOf(InvalidDlqFilterException.class);
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

    public OutboxDlqEvent buildEvent(DlqStatus dlqStatus, String eventType) {
        Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        return new OutboxDlqEvent(
                UUID.randomUUID(),
                EventStatus.FAILED,
                eventType,
                "io.example.OrderCreated",
                "{\"orderId\":\"123\"}",
                3,
                now.plusSeconds(60),
                now,
                now,
                dlqStatus,
                now
        );
    }

    public void saveBatch(List<OutboxDlqEvent> eventBatch) {
        String sql = """
            INSERT INTO outbox_dlq_events
            (id, status, dlq_status, event_type, payload_type, payload, retry_count, next_retry_at, created_at, updated_at, moved_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        jdbcTemplate.batchUpdate(
                sql,
                eventBatch,
                eventBatch.size(),
                (ps, event) -> {
                    idHelper.setIdToPs(ps, 1, event.getId());
                    ps.setString(2, event.getStatus().name());
                    ps.setString(3, event.getDlqStatus().name());
                    ps.setString(4, event.getEventType());
                    ps.setString(5, event.getPayloadType());
                    ps.setString(6, event.getPayload());
                    ps.setInt(7, event.getRetryCount());
                    ps.setTimestamp(8, Timestamp.from(event.getNextRetryAt()));
                    ps.setTimestamp(9, Timestamp.from(event.getCreatedAt()));
                    ps.setTimestamp(10, Timestamp.from(event.getUpdatedAt()));
                    ps.setTimestamp(11, Timestamp.from(event.getMovedAt()));
                });
    }

    public List<OutboxDlqEvent> findBatch(Set<UUID> ids) {
        if (!RepositoryUtils.isIdsValid(ids)) return List.of();
        String sql = """
            SELECT id, status, dlq_status, event_type, payload_type, payload, retry_count, next_retry_at, created_at, updated_at, moved_at
            FROM outbox_dlq_events
            WHERE id IN (%s)
        """.formatted(RepositoryUtils.generateIdsPlaceholders(ids));
        return jdbcTemplate.query(
                sql,
                ps -> {
                    int i = 1;
                    for (UUID id: ids) {
                        idHelper.setIdToPs(ps, i++, id);
                    }
                },
                (rs, rowNum) -> mapper.toDlqEvent(rs)
        );
    }
}
