package io.github.dmitriyiliyov.oncebox.metrics.outbox.dlq;

import io.github.dmitriyiliyov.oncebox.core.outbox.dlq.DlqStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MultiDialectOutboxDlqMetricsRepositoryUnitTests {

    @Mock
    JdbcTemplate jdbcTemplate;

    MultiDialectOutboxDlqMetricsRepository repository;

    @BeforeEach
    void setUp() {
        repository = new MultiDialectOutboxDlqMetricsRepository(jdbcTemplate);
    }

    @Test
    @DisplayName("UT constructor should throw NPE when jdbcTemplate is null")
    void constructor_shouldThrowNPE_whenJdbcTemplateIsNull() {
        assertThrows(NullPointerException.class, () -> new MultiDialectOutboxDlqMetricsRepository(null));
    }

    @Test
    @DisplayName("UT count() should execute correct SQL")
    void count_shouldExecuteCorrectSql() {
        // given
        long expectedCount = 100L;
        when(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM outbox_dlq_events",
                Long.class)
        ).thenReturn(expectedCount);

        // when
        long result = repository.count();

        // then
        assertThat(result).isEqualTo(expectedCount);
    }

    @Test
    @DisplayName("UT countByStatus() should execute correct SQL")
    void countByStatus_shouldExecuteCorrectSql() {
        // given
        DlqStatus status = DlqStatus.MOVED;
        long expectedCount = 50L;
        when(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM outbox_dlq_events WHERE dlq_status = ?",
                Long.class,
                status.name())
        ).thenReturn(expectedCount);

        // when
        long result = repository.countByStatus(status);

        // then
        assertThat(result).isEqualTo(expectedCount);
    }

    @Test
    @DisplayName("UT countByEventTypeAndStatus() should execute correct SQL")
    void countByEventTypeAndStatus_shouldExecuteCorrectSql() {
        // given
        String eventType = "test-event";
        DlqStatus status = DlqStatus.MOVED;
        long expectedCount = 20L;
        when(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM outbox_dlq_events WHERE event_type = ? AND dlq_status = ?",
                Long.class,
                eventType,
                status.name())
        ).thenReturn(expectedCount);

        // when
        long result = repository.countByEventTypeAndStatus(eventType, status);

        // then
        assertThat(result).isEqualTo(expectedCount);
    }

    @Test
    @DisplayName("UT count() should handle null result")
    void count_shouldHandleNullResult() {
        when(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM outbox_dlq_events",
                Long.class)
        ).thenReturn(null);

        long result = repository.count();

        assertThat(result).isEqualTo(0L);
    }

    @Test
    @DisplayName("UT countByStatus() should handle null result")
    void countByStatus_shouldHandleNullResult() {
        DlqStatus status = DlqStatus.MOVED;
        when(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM outbox_dlq_events WHERE dlq_status = ?",
                Long.class,
                status.name())
        ).thenReturn(null);

        long result = repository.countByStatus(status);

        assertThat(result).isEqualTo(0L);
    }

    @Test
    @DisplayName("UT countByEventTypeAndStatus() should handle null result")
    void countByEventTypeAndStatus_shouldHandleNullResult() {
        String eventType = "test-event";
        DlqStatus status = DlqStatus.MOVED;
        when(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM outbox_dlq_events WHERE event_type = ? AND dlq_status = ?",
                Long.class,
                eventType,
                status.name())
        ).thenReturn(null);

        long result = repository.countByEventTypeAndStatus(eventType, status);

        assertThat(result).isEqualTo(0L);
    }
}
