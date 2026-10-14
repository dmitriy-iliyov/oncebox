package io.github.dmitriyiliyov.oncebox.core.outbox.dlq;

import io.github.dmitriyiliyov.oncebox.core.outbox.EventStatus;
import io.github.dmitriyiliyov.oncebox.core.utils.ResultSetMapper;
import io.github.dmitriyiliyov.oncebox.core.utils.SqlUuidHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ParameterizedPreparedStatementSetter;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AbstractOutboxDlqRepositoryUnitTests {

    private static final UUID ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1");
    private static final Instant NEXT_RETRY_AT = Instant.parse("2026-01-01T10:05:00Z");
    private static final Instant CREATED_AT = Instant.parse("2026-01-01T10:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-01-01T10:01:00Z");

    @Mock
    JdbcTemplate jdbcTemplate;

    @Mock
    SqlUuidHelper uuidHelper;

    @Mock
    ResultSetMapper mapper;

    AbstractOutboxDlqRepository tested;

    @BeforeEach
    void setUp() {
        tested = Mockito.spy(
                new AbstractOutboxDlqRepository(jdbcTemplate, uuidHelper, mapper) {
                    @Override
                    public List<OutboxDlqEvent> findAndLockBatchByStatus(DlqStatus status, int batchSize, DlqStatus lockStatus) {
                        return List.of();
                    }

                    @Override
                    public int deleteBatchByStatusOlderThan(DlqStatus status, Duration age, int batchSize) {
                        return 0;
                    }
                }
        );
    }

    @Test
    @DisplayName("UT constructor when jdbcTemplate is null should throw NullPointerException")
    void constructor_whenJdbcTemplateIsNull_shouldThrowNullPointerException() {
        assertThatThrownBy(() -> new AbstractOutboxDlqRepository(null, uuidHelper, mapper) {
            @Override
            public List<OutboxDlqEvent> findAndLockBatchByStatus(DlqStatus status, int batchSize, DlqStatus lockStatus) { return null; }

            @Override
            public int deleteBatchByStatusOlderThan(DlqStatus status, Duration age, int batchSize) { return 0; }
        }).isInstanceOf(NullPointerException.class).hasMessageContaining("jdbcTemplate cannot be null");
    }

    @Test
    @DisplayName("UT constructor when uuidHelper is null should throw NullPointerException")
    void constructor_whenUuidHelperIsNull_shouldThrowNullPointerException() {
        assertThatThrownBy(() -> new AbstractOutboxDlqRepository(jdbcTemplate, null, mapper) {
            @Override
            public List<OutboxDlqEvent> findAndLockBatchByStatus(DlqStatus status, int batchSize, DlqStatus lockStatus) { return null; }

            @Override
            public int deleteBatchByStatusOlderThan(DlqStatus status, Duration age, int batchSize) { return 0; }
        }).isInstanceOf(NullPointerException.class).hasMessageContaining("uuidHelper cannot be null");
    }

    @Test
    @DisplayName("UT constructor when mapper is null should throw NullPointerException")
    void constructor_whenMapperIsNull_shouldThrowNullPointerException() {
        assertThatThrownBy(() -> new AbstractOutboxDlqRepository(jdbcTemplate, uuidHelper, null) {
            @Override
            public List<OutboxDlqEvent> findAndLockBatchByStatus(DlqStatus status, int batchSize, DlqStatus lockStatus) { return null; }

            @Override
            public int deleteBatchByStatusOlderThan(DlqStatus status, Duration age, int batchSize) { return 0; }
        }).isInstanceOf(NullPointerException.class).hasMessageContaining("mapper cannot be null");
    }

    @Test
    @DisplayName("UT deleteBatch() when ids is empty should early return")
    void deleteBatch_whenIdsIsEmpty_shouldEarlyReturn() {
        // given
        Set<UUID> ids = Set.of();

        // when
        tested.deleteBatch(ids);

        // then
        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    @DisplayName("UT deleteBatch() when ids is null should throw NPE")
    void deleteBatch_whenIdsIsNull_shouldThrow() {
        // given
        Set<UUID> ids = null;

        // when + then
        assertThatThrownBy(() -> tested.deleteBatch(ids))
                .isInstanceOf(NullPointerException.class);
        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    @DisplayName("UT saveBatch() should write the times the event carries as UTC moments and leave moved_at to the database")
    @SuppressWarnings("unchecked")
    void saveBatch_whenCalled_shouldWriteEventTimesAndLeaveMovedAtToDatabase() throws SQLException {
        // given
        OutboxDlqEvent event = OutboxDlqEvent.moved(new io.github.dmitriyiliyov.oncebox.core.outbox.OutboxEvent(
                ID, EventStatus.FAILED, "type", "payloadType", "{}", 3, NEXT_RETRY_AT, CREATED_AT, UPDATED_AT));

        // when
        tested.saveBatch(List.of(event));

        // then
        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<ParameterizedPreparedStatementSetter<OutboxDlqEvent>> setter =
                ArgumentCaptor.forClass(ParameterizedPreparedStatementSetter.class);
        verify(jdbcTemplate).batchUpdate(sql.capture(), eq(List.of(event)), eq(1), setter.capture());
        assertThat(sql.getValue())
                .contains("retry_count, next_retry_at, created_at, updated_at)")
                .doesNotContain("moved_at");

        PreparedStatement ps = mock(PreparedStatement.class);
        setter.getValue().setValues(ps, event);
        verify(uuidHelper).setToPs(ps, 1, ID);
        verify(ps).setString(2, EventStatus.FAILED.name());
        verify(ps).setString(3, DlqStatus.MOVED.name());
        verify(ps).setString(4, "type");
        verify(ps).setString(5, "payloadType");
        verify(ps).setString(6, "{}");
        verify(ps).setInt(7, 3);
        verify(ps).setObject(8, OffsetDateTime.ofInstant(NEXT_RETRY_AT, ZoneOffset.UTC));
        verify(ps).setObject(9, OffsetDateTime.ofInstant(CREATED_AT, ZoneOffset.UTC));
        verify(ps).setObject(10, OffsetDateTime.ofInstant(UPDATED_AT, ZoneOffset.UTC));
        verifyNoMoreInteractions(ps);
    }
}
