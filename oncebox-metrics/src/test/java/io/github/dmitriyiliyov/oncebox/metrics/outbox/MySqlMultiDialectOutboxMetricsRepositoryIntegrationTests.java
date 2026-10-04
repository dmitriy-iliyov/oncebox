package io.github.dmitriyiliyov.oncebox.metrics.outbox;

import io.github.dmitriyiliyov.oncebox.metrics.it_config.BaseMySqlIntegrationTests;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class MySqlMultiDialectOutboxMetricsRepositoryIntegrationTests extends BaseMySqlIntegrationTests {

    private final MySqlOutboxMetricsRepositoryVerifier verifier;

    MySqlMultiDialectOutboxMetricsRepositoryIntegrationTests(
            @Qualifier("mysqlMetricsRepo") MultiDialectOutboxMetricsRepository repository,
            @Qualifier("mysqlJdbcTemplate") JdbcTemplate jdbcTemplate
    ) {
        this.verifier = new MySqlOutboxMetricsRepositoryVerifier(repository, jdbcTemplate);
    }

    @BeforeEach
    void cleanUp() { verifier.cleanUp(); }

    @Test
    @DisplayName("IT count() when no events should return zero")
    void count_noEvents_returnsZero() { verifier.count_noEvents_returnsZero(); }

    @Test @DisplayName("IT count() with events should return total")
    void count_withEvents_returnsTotal() { verifier.count_withEvents_returnsTotal(); }

    @Test @DisplayName("IT countByStatus() when no events should return zero")
    void countByStatus_noEvents_returnsZero() { verifier.countByStatus_noEvents_returnsZero(); }

    @Test @DisplayName("IT countByStatus() should return only matching status")
    void countByStatus_returnsOnlyMatchingStatus() { verifier.countByStatus_returnsOnlyMatchingStatus(); }

    @Test @DisplayName("IT countByStatus() should not count other statuses")
    void countByStatus_doesNotCountOtherStatuses() { verifier.countByStatus_doesNotCountOtherStatuses(); }

    @Test @DisplayName("IT countByEventTypeAndStatus() when no events should return zero")
    void countByEventTypeAndStatus_noEvents_returnsZero() { verifier.countByEventTypeAndStatus_noEvents_returnsZero(); }

    @Test @DisplayName("IT countByEventTypeAndStatus() should return only matching type and status")
    void countByEventTypeAndStatus_returnsOnlyMatchingTypeAndStatus() { verifier.countByEventTypeAndStatus_returnsOnlyMatchingTypeAndStatus(); }

    @Test @DisplayName("IT countByEventTypeAndStatus() with wrong event type should return zero")
    void countByEventTypeAndStatus_wrongEventType_returnsZero() { verifier.countByEventTypeAndStatus_wrongEventType_returnsZero(); }

    @Test @DisplayName("IT countByEventTypeAndStatus() with wrong status should return zero")
    void countByEventTypeAndStatus_wrongStatus_returnsZero() { verifier.countByEventTypeAndStatus_wrongStatus_returnsZero(); }
}
