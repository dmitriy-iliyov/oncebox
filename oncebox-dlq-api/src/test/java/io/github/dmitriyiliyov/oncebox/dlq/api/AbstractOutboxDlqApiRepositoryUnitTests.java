package io.github.dmitriyiliyov.oncebox.dlq.api;

import io.github.dmitriyiliyov.oncebox.core.outbox.dlq.DlqStatus;
import io.github.dmitriyiliyov.oncebox.core.outbox.dlq.OutboxDlqEvent;
import io.github.dmitriyiliyov.oncebox.core.utils.ResultSetMapper;
import io.github.dmitriyiliyov.oncebox.core.utils.SqlUuidHelper;
import io.github.dmitriyiliyov.oncebox.dlq.api.exception.InvalidDlqFilterException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.RowMapper;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AbstractOutboxDlqApiRepositoryUnitTests {

    JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);

    SqlUuidHelper uuidHelper = mock(SqlUuidHelper.class);

    ResultSetMapper mapper = mock(ResultSetMapper.class);

    AbstractOutboxDlqApiRepository tested;

    @BeforeEach
    public void construct() {
        tested = repository(jdbcTemplate, uuidHelper, mapper);
    }

    @Test
    @DisplayName("UT constructor should throw NPE when jdbcTemplate is null")
    void constructor_shouldThrowNPE_whenJdbcTemplateIsNull() {
        assertThatThrownBy(() -> repository(null, uuidHelper, mapper)).isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("UT constructor should throw NPE when uuidHelper is null")
    void constructor_shouldThrowNPE_whenUuidHelperIsNull() {
        assertThatThrownBy(() -> repository(jdbcTemplate, null, mapper)).isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("UT constructor should throw NPE when mapper is null")
    void constructor_shouldThrowNPE_whenMapperIsNull() {
        assertThatThrownBy(() -> repository(jdbcTemplate, uuidHelper, null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("UT findById() should return event when found")
    void findById_shouldReturnEvent() {
        UUID id = UUID.randomUUID();
        OutboxDlqEvent event = mock(OutboxDlqEvent.class);

        when(jdbcTemplate.query(anyString(), any(PreparedStatementSetter.class), any(RowMapper.class)))
                .thenReturn(List.of(event));

        Optional<OutboxDlqEvent> result = tested.findById(id);

        assertThat(result).isPresent().contains(event);
    }

    @Test
    @DisplayName("UT findById() should return empty when not found")
    void findById_shouldReturnEmpty() {
        UUID id = UUID.randomUUID();

        when(jdbcTemplate.query(anyString(), any(PreparedStatementSetter.class), any(RowMapper.class)))
                .thenReturn(List.of());

        Optional<OutboxDlqEvent> result = tested.findById(id);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("UT findByIdForUpdate() should return event when found")
    void findByIdForUpdate_shouldReturnEvent() {
        UUID id = UUID.randomUUID();
        OutboxDlqEvent event = mock(OutboxDlqEvent.class);

        when(jdbcTemplate.query(anyString(), any(PreparedStatementSetter.class), any(RowMapper.class)))
                .thenReturn(List.of(event));

        Optional<OutboxDlqEvent> result = tested.findByIdForUpdate(id);

        assertThat(result).isPresent().contains(event);
    }

    @Test
    @DisplayName("UT findByIdForUpdate() should return empty when not found")
    void findByIdForUpdate_shouldReturnEmpty() {
        UUID id = UUID.randomUUID();

        when(jdbcTemplate.query(anyString(), any(PreparedStatementSetter.class), any(RowMapper.class)))
                .thenReturn(List.of());

        Optional<OutboxDlqEvent> result = tested.findByIdForUpdate(id);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("UT findBatch() with empty filter should call jdbcTemplate")
    void findBatch_withEmptyFilter_shouldCallJdbc() {
        DlqFilter filter = mock(DlqFilter.class);

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
                .thenReturn(List.of());

        List<OutboxDlqEvent> result = tested.findBatch(filter, 0, 10);

        assertThat(result).isEmpty();
        verify(jdbcTemplate).query(anyString(), any(RowMapper.class), any(Object[].class));
    }

    @Test
    @DisplayName("UT findBatch() with status filter should call jdbcTemplate")
    void findBatch_withStatusFilter_shouldCallJdbc() {
        DlqFilter filter = mock(DlqFilter.class);
        when(filter.hasStatus()).thenReturn(true);
        when(filter.getStatus()).thenReturn(DlqStatus.MOVED);

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
                .thenReturn(List.of());

        tested.findBatch(filter, 0, 10);

        verify(jdbcTemplate).query(anyString(), any(RowMapper.class), any(Object[].class));
    }

    @Test
    @DisplayName("UT findBatch() with eventType filter should call jdbcTemplate")
    void findBatch_withEventTypeFilter_shouldCallJdbc() {
        DlqFilter filter = mock(DlqFilter.class);
        when(filter.hasEventType()).thenReturn(true);
        when(filter.getEventType()).thenReturn("ORDER_CREATED");

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
                .thenReturn(List.of());

        tested.findBatch(filter, 1, 20);

        verify(jdbcTemplate).query(anyString(), any(RowMapper.class), any(Object[].class));
    }

    @Test
    @DisplayName("UT count() with empty filter should handle null result")
    void count_withEmptyFilter_shouldHandleNull() {
        DlqFilter filter = mock(DlqFilter.class);

        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
                .thenReturn(null);

        long result = tested.count(filter);

        assertThat(result).isEqualTo(0L);
    }

    @Test
    @DisplayName("UT count() with empty filter should return value")
    void count_withEmptyFilter_shouldReturnValue() {
        DlqFilter filter = mock(DlqFilter.class);

        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
                .thenReturn(5L);

        long result = tested.count(filter);

        assertThat(result).isEqualTo(5L);
    }

    @Test
    @DisplayName("UT count() with status filter should handle null result")
    void count_withStatusFilter_shouldHandleNull() {
        DlqFilter filter = mock(DlqFilter.class);
        when(filter.hasStatus()).thenReturn(true);
        when(filter.getStatus()).thenReturn(DlqStatus.MOVED);

        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
                .thenReturn(null);

        long result = tested.count(filter);

        assertThat(result).isEqualTo(0L);
    }

    @Test
    @DisplayName("UT count() with status filter should return value")
    void count_withStatusFilter_shouldReturnValue() {
        DlqFilter filter = mock(DlqFilter.class);
        when(filter.hasStatus()).thenReturn(true);
        when(filter.getStatus()).thenReturn(DlqStatus.MOVED);

        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
                .thenReturn(8L);

        long result = tested.count(filter);

        assertThat(result).isEqualTo(8L);
    }

    @Test
    @DisplayName("UT count() with eventType filter should handle null result")
    void count_withEventTypeFilter_shouldHandleNull() {
        DlqFilter filter = mock(DlqFilter.class);
        when(filter.hasEventType()).thenReturn(true);
        when(filter.getEventType()).thenReturn("ORDER_CREATED");

        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
                .thenReturn(null);

        long result = tested.count(filter);

        assertThat(result).isEqualTo(0L);
    }

    @Test
    @DisplayName("UT count() with eventType filter should return value")
    void count_withEventTypeFilter_shouldReturnValue() {
        DlqFilter filter = mock(DlqFilter.class);
        when(filter.hasEventType()).thenReturn(true);
        when(filter.getEventType()).thenReturn("ORDER_CREATED");

        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
                .thenReturn(10L);

        long result = tested.count(filter);

        assertThat(result).isEqualTo(10L);
    }

    @Test
    @DisplayName("UT count() with status and eventType filter should handle null result")
    void count_withStatusAndEventTypeFilter_shouldHandleNull() {
        DlqFilter filter = mock(DlqFilter.class);
        when(filter.hasStatus()).thenReturn(true);
        when(filter.getStatus()).thenReturn(DlqStatus.MOVED);
        when(filter.hasEventType()).thenReturn(true);
        when(filter.getEventType()).thenReturn("ORDER_CREATED");

        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
                .thenReturn(null);

        long result = tested.count(filter);

        assertThat(result).isEqualTo(0L);
    }

    @Test
    @DisplayName("UT count() with status and eventType filter should return value")
    void count_withStatusAndEventTypeFilter_shouldReturnValue() {
        DlqFilter filter = mock(DlqFilter.class);
        when(filter.hasStatus()).thenReturn(true);
        when(filter.getStatus()).thenReturn(DlqStatus.MOVED);
        when(filter.hasEventType()).thenReturn(true);
        when(filter.getEventType()).thenReturn("ORDER_CREATED");

        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class)))
                .thenReturn(12L);

        long result = tested.count(filter);

        assertThat(result).isEqualTo(12L);
    }

    @Test
    @DisplayName("UT updateStatus() should set updated_at to the database time and bind only the status and the id")
    void updateStatus_whenCalled_shouldSetUpdatedAtToDatabaseTime() throws Exception {
        // given
        UUID id = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1");

        // when
        tested.updateStatus(id, DlqStatus.MOVED);

        // then
        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<PreparedStatementSetter> setter = ArgumentCaptor.forClass(PreparedStatementSetter.class);
        verify(jdbcTemplate).update(sql.capture(), setter.capture());
        assertThat(sql.getValue()).contains("updated_at = " + DB_NOW);

        java.sql.PreparedStatement ps = mock(java.sql.PreparedStatement.class);
        setter.getValue().setValues(ps);
        verify(ps).setString(1, DlqStatus.MOVED.name());
        verify(uuidHelper).setToPs(ps, 2, id);
        verifyNoMoreInteractions(ps);
    }

    @Test
    @DisplayName("UT updateBatchStatus() when filter has no status should throw")
    void updateBatchStatus_withNoStatus_shouldThrow() {
        DlqFilter filter = mock(DlqFilter.class);
        when(filter.hasStatus()).thenReturn(false);

        assertThatThrownBy(() -> tested.updateBatchStatus(filter, DlqStatus.IN_PROCESS))
                .isInstanceOf(InvalidDlqFilterException.class);

        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    @DisplayName("UT updateBatchStatus() by eventType should execute update")
    void updateBatchStatus_byEventType_shouldExecute() {
        DlqFilter filter = mock(DlqFilter.class);
        when(filter.hasStatus()).thenReturn(true);
        when(filter.getStatus()).thenReturn(DlqStatus.MOVED);
        when(filter.hasEventType()).thenReturn(true);
        when(filter.getEventType()).thenReturn("ORDER_CREATED");

        when(jdbcTemplate.update(anyString(), any(Object[].class))).thenReturn(3);

        int result = tested.updateBatchStatus(filter, DlqStatus.IN_PROCESS);

        assertThat(result).isEqualTo(3);
        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object[]> params = ArgumentCaptor.forClass(Object[].class);
        verify(jdbcTemplate).update(sql.capture(), params.capture());
        assertThat(sql.getValue()).contains("updated_at = " + DB_NOW);
        assertThat(params.getValue()).containsExactly(DlqStatus.MOVED.name(), DlqStatus.IN_PROCESS.name(), "ORDER_CREATED");
    }

    @Test
    @DisplayName("UT updateBatchStatus() by valid ids should execute update")
    void updateBatchStatus_byValidIds_shouldExecute() {
        Set<UUID> ids = Set.of(UUID.randomUUID());
        DlqFilter filter = mock(DlqFilter.class);
        when(filter.hasStatus()).thenReturn(true);
        when(filter.getStatus()).thenReturn(DlqStatus.MOVED);
        when(filter.hasEventType()).thenReturn(false);
        when(filter.hasIds()).thenReturn(true);
        when(filter.getIds()).thenReturn(ids);

        when(jdbcTemplate.update(anyString(), any(Object[].class))).thenReturn(1);

        int result = tested.updateBatchStatus(filter, DlqStatus.IN_PROCESS);

        assertThat(result).isEqualTo(1);
        verify(jdbcTemplate).update(anyString(), any(Object[].class));
    }

    @Test
    @DisplayName("UT updateBatchStatus() by empty ids should throw")
    void updateBatchStatus_byEmptyIds_shouldThrow() {
        DlqFilter filter = mock(DlqFilter.class);
        when(filter.hasStatus()).thenReturn(true);
        when(filter.getStatus()).thenReturn(DlqStatus.MOVED);
        when(filter.hasEventType()).thenReturn(false);
        when(filter.hasIds()).thenReturn(true);
        when(filter.getIds()).thenReturn(Set.of());

        assertThatThrownBy(() -> tested.updateBatchStatus(filter, DlqStatus.IN_PROCESS))
                .isInstanceOf(InvalidDlqFilterException.class);
    }

    @Test
    @DisplayName("UT updateBatchStatus() when filter has neither eventType nor ids should throw")
    void updateBatchStatus_withNoEventTypeOrIds_shouldThrow() {
        DlqFilter filter = mock(DlqFilter.class);
        when(filter.hasStatus()).thenReturn(true);
        when(filter.getStatus()).thenReturn(DlqStatus.MOVED);
        when(filter.hasEventType()).thenReturn(false);
        when(filter.hasIds()).thenReturn(false);

        assertThatThrownBy(() -> tested.updateBatchStatus(filter, DlqStatus.IN_PROCESS))
                .isInstanceOf(InvalidDlqFilterException.class);
    }

    @Test
    @DisplayName("UT deleteById() should call jdbc")
    void deleteById_shouldCallJdbc() {
        when(jdbcTemplate.update(anyString(), any(PreparedStatementSetter.class))).thenReturn(1);

        int result = tested.deleteById(UUID.randomUUID());

        assertThat(result).isEqualTo(1);
        verify(jdbcTemplate).update(anyString(), any(PreparedStatementSetter.class));
    }

    @Test
    @DisplayName("UT deleteBatch() by eventType should execute")
    void deleteBatch_byEventType_shouldExecute() {
        DlqFilter filter = mock(DlqFilter.class);
        when(filter.hasEventType()).thenReturn(true);
        when(filter.getEventType()).thenReturn("ORDER_CREATED");

        when(jdbcTemplate.update(anyString(), any(Object[].class))).thenReturn(4);

        int result = tested.deleteBatch(filter, DlqStatus.IN_PROCESS);

        assertThat(result).isEqualTo(4);
        verify(jdbcTemplate).update(anyString(), any(Object[].class));
    }

    @Test
    @DisplayName("UT deleteBatch() by valid ids should execute")
    void deleteBatch_byValidIds_shouldExecute() {
        Set<UUID> ids = Set.of(UUID.randomUUID());
        DlqFilter filter = mock(DlqFilter.class);
        when(filter.hasEventType()).thenReturn(false);
        when(filter.hasIds()).thenReturn(true);
        when(filter.getIds()).thenReturn(ids);

        when(jdbcTemplate.update(anyString(), any(Object[].class))).thenReturn(2);

        int result = tested.deleteBatch(filter, DlqStatus.IN_PROCESS);

        assertThat(result).isEqualTo(2);
        verify(jdbcTemplate).update(anyString(), any(Object[].class));
    }

    @Test
    @DisplayName("UT deleteBatch() by empty ids should throw")
    void deleteBatch_byEmptyIds_shouldThrow() {
        DlqFilter filter = mock(DlqFilter.class);
        when(filter.hasEventType()).thenReturn(false);
        when(filter.hasIds()).thenReturn(true);
        when(filter.getIds()).thenReturn(Set.of());

        assertThatThrownBy(() -> tested.deleteBatch(filter, DlqStatus.IN_PROCESS))
                .isInstanceOf(InvalidDlqFilterException.class);
    }

    @Test
    @DisplayName("UT deleteBatch() when filter has neither eventType nor ids should throw")
    void deleteBatch_withNoEventTypeOrIds_shouldThrow() {
        DlqFilter filter = mock(DlqFilter.class);
        when(filter.hasEventType()).thenReturn(false);
        when(filter.hasIds()).thenReturn(false);

        assertThatThrownBy(() -> tested.deleteBatch(filter, DlqStatus.IN_PROCESS))
                .isInstanceOf(InvalidDlqFilterException.class);
    }

    private static final String DB_NOW = "db_now()";

    private static AbstractOutboxDlqApiRepository repository(JdbcTemplate jdbcTemplate,
                                                             SqlUuidHelper uuidHelper,
                                                             ResultSetMapper mapper) {
        return new AbstractOutboxDlqApiRepository(jdbcTemplate, uuidHelper, mapper) {
            @Override
            protected Object convertIdParameter(UUID id) {
                return id;
            }

            @Override
            protected String currentTimestampSql() {
                return DB_NOW;
            }
        };
    }
}
