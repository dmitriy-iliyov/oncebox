package io.github.dmitriyiliyov.oncebox.metrics.outbox;

import io.github.dmitriyiliyov.oncebox.core.outbox.domain.EventStatus;
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
class MultiDialectOutboxMetricsRepositoryUnitTests {

    @Mock
    JdbcTemplate jdbcTemplate;

    MultiDialectOutboxMetricsRepository repository;

    @BeforeEach
    void setUp() {
        repository = new MultiDialectOutboxMetricsRepository(jdbcTemplate);
    }

    @Test
    @DisplayName("UT constructor should throw NPE when jdbcTemplate is null")
    void constructor_shouldThrowNPE_whenJdbcTemplateIsNull() {
        assertThrows(NullPointerException.class, () -> new MultiDialectOutboxMetricsRepository(null));
    }

    @Test
    @DisplayName("UT count() should execute correct SQL and return count")
    void count_shouldExecuteCorrectSqlAndReturnCount() {
        // given
        long expectedCount = 100L;
        when(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM outbox_events",
                Long.class)
        ).thenReturn(expectedCount);

        // when
        long result = repository.count();

        // then
        assertThat(result).isEqualTo(expectedCount);
    }

    @Test
    @DisplayName("UT count() should handle null result")
    void count_shouldHandleNullResult() {
        // given
        when(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM outbox_events",
                Long.class)
        ).thenReturn(null);

        // when
        long result = repository.count();

        // then
        assertThat(result).isEqualTo(0L);
    }

    @Test
    @DisplayName("UT countByStatus() should execute correct SQL and return count")
    void countByStatus_shouldExecuteCorrectSqlAndReturnCount() {
        // given
        EventStatus status = EventStatus.PENDING;
        long expectedCount = 50L;
        when(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM outbox_events WHERE status = ?",
                Long.class,
                status.name())
        ).thenReturn(expectedCount);

        // when
        long result = repository.countByStatus(status);

        // then
        assertThat(result).isEqualTo(expectedCount);
    }

    @Test
    @DisplayName("UT countByEventTypeAndStatus() should execute correct SQL and return count")
    void countByEventTypeAndStatus_shouldExecuteCorrectSqlAndReturnCount() {
        // given
        String eventType = "test-event";
        EventStatus status = EventStatus.PENDING;
        long expectedCount = 20L;
        when(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM outbox_events WHERE event_type = ? AND status = ?",
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
    @DisplayName("UT countByStatus() should handle null result")
    void countByStatus_shouldHandleNullResult() {
        EventStatus status = EventStatus.PENDING;
        when(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM outbox_events WHERE status = ?",
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
        EventStatus status = EventStatus.PENDING;
        when(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM outbox_events WHERE event_type = ? AND status = ?",
                Long.class,
                eventType,
                status.name())
        ).thenReturn(null);

        long result = repository.countByEventTypeAndStatus(eventType, status);

        assertThat(result).isEqualTo(0L);
    }
}
