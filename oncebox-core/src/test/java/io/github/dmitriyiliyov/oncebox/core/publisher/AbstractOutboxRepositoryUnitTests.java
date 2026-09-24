package io.github.dmitriyiliyov.oncebox.core.publisher;

import io.github.dmitriyiliyov.oncebox.core.publisher.domain.EventStatus;
import io.github.dmitriyiliyov.oncebox.core.publisher.domain.OutboxEvent;
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
import java.sql.Timestamp;
import java.time.Clock;
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
    Clock clock;

    @Mock
    SqlUuidHelper uuidHelper;

    AbstractOutboxRepository tested;

    @BeforeEach
    void setUp() {
        tested = new TestOutboxRepository(jdbcTemplate, clock, uuidHelper);
    }

    @Test
    @DisplayName("UT constructor when jdbcTemplate is null should throw NullPointerException")
    void constructor_whenJdbcTemplateIsNull_shouldThrowNullPointerException() {
        assertThatThrownBy(() -> new TestOutboxRepository(null, clock, uuidHelper))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("jdbcTemplate cannot be null");
    }

    @Test
    @DisplayName("UT constructor when clock is null should throw NullPointerException")
    void constructor_whenClockIsNull_shouldThrowNullPointerException() {
        assertThatThrownBy(() -> new TestOutboxRepository(jdbcTemplate, null, uuidHelper))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("clock cannot be null");
    }

    @Test
    @DisplayName("UT constructor when uuidHelper is null should throw NullPointerException")
    void constructor_whenUuidHelperIsNull_shouldThrowNullPointerException() {
        assertThatThrownBy(() -> new TestOutboxRepository(jdbcTemplate, clock, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("uuidHelper cannot be null");
    }

    @Test
    @DisplayName("UT updateBatchStatusByLockToken() should update only the events still held by the lock token")
    void updateBatchStatusByLockToken_shouldUpdateOnlyEventsHeldByLockToken() throws SQLException {
        // given
        Set<UUID> ids = Set.of(EVENT_ID, OTHER_EVENT_ID);
        when(clock.instant()).thenReturn(NOW);
        when(jdbcTemplate.update(anyString(), any(PreparedStatementSetter.class))).thenReturn(1);

        // when
        int updated = tested.updateBatchStatusByLockToken(ids, LOCK_TOKEN, EventStatus.PROCESSED);

        // then
        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<PreparedStatementSetter> setter = ArgumentCaptor.forClass(PreparedStatementSetter.class);
        verify(jdbcTemplate).update(sql.capture(), setter.capture());
        assertThat(updated).isEqualTo(1);
        assertThat(sql.getValue()).contains("WHERE lock_token = ? AND id IN (?, ?)");

        PreparedStatement ps = mock(PreparedStatement.class);
        setter.getValue().setValues(ps);
        verify(ps).setString(1, EventStatus.PROCESSED.name());
        verify(ps).setTimestamp(2, Timestamp.from(NOW));
        verify(uuidHelper).setToPs(ps, 3, LOCK_TOKEN);
        verify(uuidHelper).setToPs(ps, 4, ids);
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
        Instant nextRetryAt = NOW.plusSeconds(30);
        OutboxEvent event = new OutboxEvent(
                EVENT_ID, EventStatus.PENDING, "event-type", "payload-type", "{}", 2, nextRetryAt, NOW, NOW
        );
        when(clock.instant()).thenReturn(NOW);
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
        assertThat(sql.getValue()).contains("WHERE id = ? AND lock_token = ?");

        PreparedStatement ps = mock(PreparedStatement.class);
        setter.getValue().setValues(ps, event);
        verify(ps).setInt(1, 2);
        verify(ps).setString(2, EventStatus.PENDING.name());
        verify(ps).setTimestamp(3, Timestamp.from(nextRetryAt));
        verify(ps).setTimestamp(4, Timestamp.from(NOW));
        verify(uuidHelper).setToPs(ps, 5, EVENT_ID);
        verify(uuidHelper).setToPs(ps, 6, LOCK_TOKEN);
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

    static final class TestOutboxRepository extends AbstractOutboxRepository {

        TestOutboxRepository(JdbcTemplate jdbcTemplate, Clock clock, SqlUuidHelper uuidHelper) {
            super(jdbcTemplate, clock, uuidHelper);
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
        public int updateBatchStatusByStatusAndThreshold(EventStatus status,
                                                         Instant threshold,
                                                         int batchSize,
                                                         EventStatus newStatus) {
            return 0;
        }

        @Override
        public int deleteBatchByStatusAndThreshold(EventStatus status, Instant threshold, int batchSize) {
            return 0;
        }
    }
}
