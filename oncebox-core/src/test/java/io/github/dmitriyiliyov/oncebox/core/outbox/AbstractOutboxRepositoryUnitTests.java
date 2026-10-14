package io.github.dmitriyiliyov.oncebox.core.outbox;

import io.github.dmitriyiliyov.oncebox.core.utils.SqlUuidHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ParameterizedPreparedStatementSetter;
import org.springframework.jdbc.core.PreparedStatementSetter;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AbstractOutboxRepositoryUnitTests {

    private static final UUID EVENT_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID OTHER_EVENT_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UUID LOCK_TOKEN = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final Instant NOW = Instant.parse("2026-09-25T10:00:00Z");

    @Mock
    JdbcTemplate jdbcTemplate;

    @Mock
    SqlUuidHelper uuidHelper;

    AbstractOutboxRepository tested;

    @BeforeEach
    void setUp() {
        tested = new TestOutboxRepository(jdbcTemplate, uuidHelper);
    }

    @Test
    @DisplayName("UT constructor when jdbcTemplate is null should throw NullPointerException")
    void constructor_whenJdbcTemplateIsNull_shouldThrowNullPointerException() {
        assertThatThrownBy(() -> new TestOutboxRepository(null, uuidHelper))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("jdbcTemplate cannot be null");
    }

    @Test
    @DisplayName("UT constructor when uuidHelper is null should throw NullPointerException")
    void constructor_whenUuidHelperIsNull_shouldThrowNullPointerException() {
        assertThatThrownBy(() -> new TestOutboxRepository(jdbcTemplate, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("uuidHelper cannot be null");
    }

    @Test
    @DisplayName("UT save() should write the event without any time, leaving the times to the database")
    void save_whenCalled_shouldWriteEventWithoutTimes() throws SQLException {
        // given
        OutboxEvent event = new OutboxEvent(EVENT_ID, "event-type", "payload-type", "{}");

        // when
        tested.save(event);

        // then
        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<PreparedStatementSetter> setter = ArgumentCaptor.forClass(PreparedStatementSetter.class);
        verify(jdbcTemplate).update(sql.capture(), setter.capture());
        assertThat(sql.getValue()).contains("(id, status, event_type, payload_type, payload, retry_count)")
                .contains("VALUES (?, ?, ?, ?, ?, ?)");

        PreparedStatement ps = mock(PreparedStatement.class);
        setter.getValue().setValues(ps);
        verify(uuidHelper).setToPs(ps, 1, EVENT_ID);
        verify(ps).setString(2, EventStatus.PENDING.name());
        verify(ps).setString(3, "event-type");
        verify(ps).setString(4, "payload-type");
        verify(ps).setString(5, "{}");
        verify(ps).setInt(6, -1);
        verifyNoMoreInteractions(ps);
    }

    @Test
    @DisplayName("UT saveBatch() should write every event without any time")
    @SuppressWarnings("unchecked")
    void saveBatch_whenCalled_shouldWriteEventsWithoutTimes() throws SQLException {
        // given
        OutboxEvent event = new OutboxEvent(EVENT_ID, "event-type", "payload-type", "{}");

        // when
        tested.saveBatch(List.of(event));

        // then
        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<ParameterizedPreparedStatementSetter<OutboxEvent>> setter =
                ArgumentCaptor.forClass(ParameterizedPreparedStatementSetter.class);
        verify(jdbcTemplate).batchUpdate(sql.capture(), eq(List.of(event)), eq(1), setter.capture());
        assertThat(sql.getValue()).contains("VALUES (?, ?, ?, ?, ?, ?)");

        PreparedStatement ps = mock(PreparedStatement.class);
        setter.getValue().setValues(ps, event);
        verify(ps).setInt(6, -1);
        verify(ps, never()).setTimestamp(anyInt(), any());
        verify(ps, never()).setObject(anyInt(), any());
    }

    @Test
    @DisplayName("UT saveBatchWithCreatedAt() should write the creation time of each event and leave the other times to the database")
    @SuppressWarnings("unchecked")
    void saveBatchWithCreatedAt_whenEventHasCreatedAt_shouldWriteOnlyItAmongTimes() throws SQLException {
        // given
        OutboxEvent event = event(null);

        // when
        tested.saveBatchWithCreatedAt(List.of(event));

        // then
        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<ParameterizedPreparedStatementSetter<OutboxEvent>> setter =
                ArgumentCaptor.forClass(ParameterizedPreparedStatementSetter.class);
        verify(jdbcTemplate).batchUpdate(sql.capture(), eq(List.of(event)), eq(1), setter.capture());
        assertThat(sql.getValue())
                .contains("(id, status, event_type, payload_type, payload, retry_count, created_at)")
                .contains("VALUES (?, ?, ?, ?, ?, ?, ?)")
                .doesNotContain("next_retry_at")
                .doesNotContain("updated_at");

        PreparedStatement ps = mock(PreparedStatement.class);
        setter.getValue().setValues(ps, event);
        verify(uuidHelper).setToPs(ps, 1, EVENT_ID);
        verify(ps).setString(2, EventStatus.PENDING.name());
        verify(ps).setInt(6, 2);
        assertThat(((TestOutboxRepository) tested).createdAtBindings).containsExactly(7 + "=" + NOW);
    }

    @Test
    @DisplayName("UT saveBatchWithCreatedAt() when an event has no creation time should throw NullPointerException")
    @SuppressWarnings("unchecked")
    void saveBatchWithCreatedAt_whenEventHasNoCreatedAt_shouldThrowNullPointerException() {
        // given
        OutboxEvent event = new OutboxEvent(EVENT_ID, "event-type", "payload-type", "{}");
        tested.saveBatchWithCreatedAt(List.of(event));
        ArgumentCaptor<ParameterizedPreparedStatementSetter<OutboxEvent>> setter =
                ArgumentCaptor.forClass(ParameterizedPreparedStatementSetter.class);
        verify(jdbcTemplate).batchUpdate(anyString(), eq(List.of(event)), eq(1), setter.capture());

        // when / then
        assertThatThrownBy(() -> setter.getValue().setValues(mock(PreparedStatement.class), event))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("createdAt cannot be null");
    }

    @Test
    @DisplayName("UT updateBatchStatusByLockToken() should update only the events still held by the lock token")
    void updateBatchStatusByLockToken_shouldUpdateOnlyEventsHeldByLockToken() throws SQLException {
        // given
        Set<UUID> ids = Set.of(EVENT_ID, OTHER_EVENT_ID);
        when(jdbcTemplate.update(anyString(), any(PreparedStatementSetter.class))).thenReturn(1);

        // when
        int updated = tested.updateBatchStatusByLockToken(ids, LOCK_TOKEN, EventStatus.PROCESSED);

        // then
        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<PreparedStatementSetter> setter = ArgumentCaptor.forClass(PreparedStatementSetter.class);
        verify(jdbcTemplate).update(sql.capture(), setter.capture());
        assertThat(updated).isEqualTo(1);
        assertThat(sql.getValue()).isEqualTo(TestOutboxRepository.updateByLockTokenSql(ids));

        PreparedStatement ps = mock(PreparedStatement.class);
        setter.getValue().setValues(ps);
        verify(ps).setString(1, EventStatus.PROCESSED.name());
        verify(uuidHelper).setToPs(ps, 2, LOCK_TOKEN);
        verify(uuidHelper).setToPs(ps, 3, ids);
        verifyNoMoreInteractions(ps);
    }

    @Test
    @DisplayName("UT updateBatchStatusByLockToken() when new status is FAILED should throw and touch nothing")
    void updateBatchStatusByLockToken_whenNewStatusIsFailed_shouldThrowAndTouchNothing() {
        // when / then
        assertThatThrownBy(() -> tested.updateBatchStatusByLockToken(Set.of(EVENT_ID), LOCK_TOKEN, EventStatus.FAILED))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Use partiallyUpdateBatchByLockToken() for update FAILED batch");
        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    @DisplayName("UT updateBatchStatusByLockToken() when ids is empty should return 0 without reaching the database")
    void updateBatchStatusByLockToken_whenIdsIsEmpty_shouldReturnZeroWithoutReachingDatabase() {
        // when
        int updated = tested.updateBatchStatusByLockToken(Set.of(), LOCK_TOKEN, EventStatus.PROCESSED);

        // then
        assertThat(updated).isZero();
        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    @DisplayName("UT updateBatchStatusByLockToken() when ids is null should throw NullPointerException")
    void updateBatchStatusByLockToken_whenIdsIsNull_shouldThrowNullPointerException() {
        // when / then
        assertThatThrownBy(() -> tested.updateBatchStatusByLockToken(null, LOCK_TOKEN, EventStatus.PROCESSED))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("ids cannot be null");
        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    @DisplayName("UT partiallyUpdateBatchByLockToken() should write each event's state only while the lock token holds")
    @SuppressWarnings("unchecked")
    void partiallyUpdateBatchByLockToken_shouldWriteStateOnlyWhileLockTokenHolds() throws SQLException {
        // given
        OutboxEvent event = event(Duration.ofSeconds(30));
        when(jdbcTemplate.batchUpdate(anyString(), anyList(), anyInt(), any(ParameterizedPreparedStatementSetter.class)))
                .thenReturn(new int[][]{{1}});

        // when
        int updated = tested.partiallyUpdateBatchByLockToken(List.of(event), LOCK_TOKEN);

        // then
        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<ParameterizedPreparedStatementSetter<OutboxEvent>> setter =
                ArgumentCaptor.forClass(ParameterizedPreparedStatementSetter.class);
        verify(jdbcTemplate).batchUpdate(sql.capture(), eq(List.of(event)), eq(1), setter.capture());
        assertThat(updated).isEqualTo(1);
        assertThat(sql.getValue()).isEqualTo(TestOutboxRepository.PARTIAL_UPDATE_SQL);

        PreparedStatement ps = mock(PreparedStatement.class);
        setter.getValue().setValues(ps, event);
        verify(ps).setInt(1, 2);
        verify(ps).setString(2, EventStatus.PENDING.name());
        verify(ps).setLong(3, 30_000L);
        verify(uuidHelper).setToPs(ps, 4, EVENT_ID);
        verify(uuidHelper).setToPs(ps, 5, LOCK_TOKEN);
        verifyNoMoreInteractions(ps);
    }

    @Test
    @DisplayName("UT partiallyUpdateBatchByLockToken() when the retry delay is null should bind the delay as SQL NULL")
    @SuppressWarnings("unchecked")
    void partiallyUpdateBatchByLockToken_whenRetryDelayIsNull_shouldBindDelayAsSqlNull() throws SQLException {
        // given
        OutboxEvent event = event(null);
        when(jdbcTemplate.batchUpdate(anyString(), anyList(), anyInt(), any(ParameterizedPreparedStatementSetter.class)))
                .thenReturn(new int[][]{{1}});

        // when
        tested.partiallyUpdateBatchByLockToken(List.of(event), LOCK_TOKEN);

        // then
        ArgumentCaptor<ParameterizedPreparedStatementSetter<OutboxEvent>> setter =
                ArgumentCaptor.forClass(ParameterizedPreparedStatementSetter.class);
        verify(jdbcTemplate).batchUpdate(anyString(), eq(List.of(event)), eq(1), setter.capture());
        PreparedStatement ps = mock(PreparedStatement.class);
        setter.getValue().setValues(ps, event);
        verify(ps).setNull(3, Types.BIGINT);
        verify(ps, never()).setLong(eq(3), anyLong());
    }

    @Test
    @DisplayName("UT partiallyUpdateBatchByLockToken() when events is empty should return 0 without reaching the database")
    void partiallyUpdateBatchByLockToken_whenEventsIsEmpty_shouldReturnZeroWithoutReachingDatabase() {
        // when
        int updated = tested.partiallyUpdateBatchByLockToken(List.of(), LOCK_TOKEN);

        // then
        assertThat(updated).isZero();
        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    @DisplayName("UT partiallyUpdateBatchByLockToken() when events is null should return 0 without reaching the database")
    void partiallyUpdateBatchByLockToken_whenEventsIsNull_shouldReturnZeroWithoutReachingDatabase() {
        // when
        int updated = tested.partiallyUpdateBatchByLockToken(null, LOCK_TOKEN);

        // then
        assertThat(updated).isZero();
        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    @DisplayName("UT deleteBatch() when ids is empty should return 0 without reaching the database")
    void deleteBatch_whenIdsIsEmpty_shouldReturnZeroWithoutReachingDatabase() {
        // when
        int deleted = tested.deleteBatch(Set.of());

        // then
        assertThat(deleted).isZero();
        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    @DisplayName("UT deleteBatch() when ids is null should throw NullPointerException")
    void deleteBatch_whenIdsIsNull_shouldThrowNullPointerException() {
        // when / then
        assertThatThrownBy(() -> tested.deleteBatch(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("ids cannot be null");
        verifyNoInteractions(jdbcTemplate);
    }

    private static OutboxEvent event(Duration retryDelay) {
        OutboxEvent event = new OutboxEvent(
                EVENT_ID, EventStatus.PENDING, "event-type", "payload-type", "{}", 2, NOW, NOW, NOW
        );
        event.setRetryDelay(retryDelay);
        return event;
    }

    static final class TestOutboxRepository extends AbstractOutboxRepository {

        static final String PARTIAL_UPDATE_SQL = "partial update by lock token";

        final List<String> createdAtBindings = new java.util.ArrayList<>();

        TestOutboxRepository(JdbcTemplate jdbcTemplate, SqlUuidHelper uuidHelper) {
            super(jdbcTemplate, uuidHelper);
        }

        static String updateByLockTokenSql(Set<UUID> ids) {
            return "update by lock token of " + ids.size();
        }

        @Override
        protected String updateBatchStatusByLockTokenSql(Set<UUID> ids) {
            return updateByLockTokenSql(ids);
        }

        @Override
        protected String partiallyUpdateBatchByLockTokenSql() {
            return PARTIAL_UPDATE_SQL;
        }

        @Override
        protected void setCreatedAt(PreparedStatement ps, int parameterIndex, Instant createdAt) {
            createdAtBindings.add(parameterIndex + "=" + createdAt);
        }

        @Override
        public List<OutboxEvent> findAndLockBatchByEventTypeAndStatus(String eventType,
                                                                      EventStatus status,
                                                                      int batchSize,
                                                                      UUID lockToken,
                                                                      EventStatus lockStatus) {
            return List.of();
        }

        @Override
        public List<OutboxEvent> findAndLockBatchByStatus(EventStatus status, int batchSize, EventStatus lockStatus) {
            return List.of();
        }

        @Override
        public int updateBatchStatusByStatusOlderThan(EventStatus status,
                                                      Duration age,
                                                      int batchSize,
                                                      EventStatus newStatus) {
            return 0;
        }

        @Override
        public int deleteBatchByStatusOlderThan(EventStatus status, Duration age, int batchSize) {
            return 0;
        }
    }
}
