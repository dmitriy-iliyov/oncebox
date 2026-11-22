package io.github.dmitriyiliyov.oncebox.starter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.SQLSyntaxErrorException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Properties are bound from the keys a user writes, then defaulted as the context does it, so a value the check sees
// is the one the jobs get. The database answers the probe for the IN list limit through a stubbed JdbcTemplate.
@ExtendWith(MockitoExtension.class)
class OnceboxOracleBatchSizeCheckUnitTests {

    private static final String OUTBOX = "oncebox.outbox.";
    private static final String DLQ = OUTBOX + "dlq.";
    private static final String INBOX = "oncebox.inbox.";

    @Mock
    JdbcTemplate jdbcTemplate;

    @ParameterizedTest
    @ValueSource(strings = {
            OUTBOX + "events.order-created.batch-size",
            OUTBOX + "stuck-recovery.batch-size",
            OUTBOX + "clean-up.batch-size",
            DLQ + "transfer-to.batch-size",
            DLQ + "transfer-from.batch-size",
            DLQ + "clean-up.batch-size",
            INBOX + "clean-up.batch-size"
    })
    @DisplayName("UT check() when Oracle limits IN lists to 1000 and a batch size is above it should throw naming that property")
    void check_whenInListLimitedAndBatchSizeIsAboveLimit_shouldThrowNamingProperty(String key) {
        // given
        givenInListLimitedTo1000();
        Map<String, String> config = outboxWithDlqAndInbox();
        config.put(key, "1001");
        OnceboxProperties properties = properties(config);

        // when / then
        assertThatThrownBy(() -> OnceboxOracleBatchSizeCheck.check(jdbcTemplate, properties))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageStartingWith("'%s' must not be greater than 1000 on Oracle".formatted(key));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            OUTBOX + "events.order-created.batch-size",
            OUTBOX + "stuck-recovery.batch-size",
            OUTBOX + "clean-up.batch-size",
            DLQ + "transfer-to.batch-size",
            DLQ + "transfer-from.batch-size",
            DLQ + "clean-up.batch-size",
            INBOX + "clean-up.batch-size"
    })
    @DisplayName("UT check() when Oracle limits IN lists to 1000 and a batch size is exactly 1000 should accept it")
    void check_whenInListLimitedAndBatchSizeIsAtLimit_shouldAccept(String key) {
        // given
        givenInListLimitedTo1000();
        Map<String, String> config = outboxWithDlqAndInbox();
        config.put(key, "1000");
        OnceboxProperties properties = properties(config);

        // when / then
        assertThatCode(() -> OnceboxOracleBatchSizeCheck.check(jdbcTemplate, properties)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("UT check() when Oracle limits IN lists to 1000 and every batch size is default should accept the configuration")
    void check_whenInListLimitedAndEveryBatchSizeIsDefault_shouldAccept() {
        // given
        givenInListLimitedTo1000();
        OnceboxProperties properties = properties(outboxWithDlqAndInbox());

        // when / then
        assertThatCode(() -> OnceboxOracleBatchSizeCheck.check(jdbcTemplate, properties)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("UT check() when the defaults batch size is above 1000 should reject the event that inherits it")
    void check_whenDefaultsBatchSizeIsAboveLimitAndInherited_shouldThrowNamingEvent() {
        // given
        givenInListLimitedTo1000();
        Map<String, String> config = outboxWithDlqAndInbox();
        config.put(OUTBOX + "defaults.batch-size", "1001");
        OnceboxProperties properties = properties(config);

        // when / then
        assertThatThrownBy(() -> OnceboxOracleBatchSizeCheck.check(jdbcTemplate, properties))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageStartingWith("'%s' must not be greater than 1000 on Oracle"
                        .formatted(OUTBOX + "events.order-created.batch-size"));
    }

    @Test
    @DisplayName("UT check() when the defaults batch size is above 1000 but every event sets its own should accept it")
    void check_whenDefaultsBatchSizeIsAboveLimitButOverridden_shouldAccept() {
        // given
        givenInListLimitedTo1000();
        Map<String, String> config = outboxWithDlqAndInbox();
        config.put(OUTBOX + "defaults.batch-size", "2000");
        config.put(OUTBOX + "events.order-created.batch-size", "1000");
        OnceboxProperties properties = properties(config);

        // when / then
        assertThatCode(() -> OnceboxOracleBatchSizeCheck.check(jdbcTemplate, properties)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("UT check() when the dlq batch size is above 1000 should reject the transfer that inherits it")
    void check_whenDlqBatchSizeIsAboveLimitAndInherited_shouldThrowNamingTransfer() {
        // given
        givenInListLimitedTo1000();
        Map<String, String> config = outboxWithDlqAndInbox();
        config.put(DLQ + "batch-size", "1001");
        config.put(DLQ + "transfer-from.batch-size", "1000");
        OnceboxProperties properties = properties(config);

        // when / then
        assertThatThrownBy(() -> OnceboxOracleBatchSizeCheck.check(jdbcTemplate, properties))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageStartingWith("'%s' must not be greater than 1000 on Oracle"
                        .formatted(DLQ + "transfer-to.batch-size"));
    }

    @Test
    @DisplayName("UT check() when the dlq batch size is above 1000 but both transfers set their own should accept it")
    void check_whenDlqBatchSizeIsAboveLimitButOverridden_shouldAccept() {
        // given
        givenInListLimitedTo1000();
        Map<String, String> config = outboxWithDlqAndInbox();
        config.put(DLQ + "batch-size", "2000");
        config.put(DLQ + "transfer-to.batch-size", "1000");
        config.put(DLQ + "transfer-from.batch-size", "1000");
        OnceboxProperties properties = properties(config);

        // when / then
        assertThatCode(() -> OnceboxOracleBatchSizeCheck.check(jdbcTemplate, properties)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("UT check() when the outbox is disabled should ignore its batch sizes")
    void check_whenOutboxIsDisabled_shouldIgnoreItsBatchSizes() {
        // given
        givenInListLimitedTo1000();
        Map<String, String> config = outboxWithDlqAndInbox();
        config.put(OUTBOX + "enabled", "false");
        // the one batch size a disabled outbox keeps as bound, events and the dlq are reset
        config.put(OUTBOX + "stuck-recovery.batch-size", "5000");
        OnceboxProperties properties = properties(config);

        // when / then
        assertThatCode(() -> OnceboxOracleBatchSizeCheck.check(jdbcTemplate, properties)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("UT check() when the dlq is disabled should accept its uninitialized transfers")
    void check_whenDlqIsDisabled_shouldAcceptUninitializedTransfers() {
        // given
        givenInListLimitedTo1000();
        Map<String, String> config = outboxWithDlqAndInbox();
        config.put(DLQ + "enabled", "false");
        OnceboxProperties properties = properties(config);

        // when / then
        assertThatCode(() -> OnceboxOracleBatchSizeCheck.check(jdbcTemplate, properties)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("UT check() when Oracle accepts an IN list above 1000 should accept a batch size above it")
    void check_whenInListNotLimited_shouldAcceptBatchSizeAboveLimit() {
        // given
        givenNoInListLimit();
        Map<String, String> config = outboxWithDlqAndInbox();
        config.put(OUTBOX + "events.order-created.batch-size", "5000");
        OnceboxProperties properties = properties(config);

        // when / then
        assertThatCode(() -> OnceboxOracleBatchSizeCheck.check(jdbcTemplate, properties)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("UT check() should probe the limit with an IN list of 1001 expressions")
    void check_shouldProbeWithInListOneAboveLimit() {
        // given
        givenNoInListLimit();
        OnceboxProperties properties = properties(outboxWithDlqAndInbox());

        // when
        OnceboxOracleBatchSizeCheck.check(jdbcTemplate, properties);

        // then
        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object[]> args = ArgumentCaptor.forClass(Object[].class);
        verify(jdbcTemplate).queryForList(sql.capture(), args.capture());
        assertThat(args.getValue()).hasSize(1001);
        assertThat(sql.getValue().chars().filter(c -> c == '?').count()).isEqualTo(1001);
    }

    @Test
    @DisplayName("UT check() when the probe fails with another database error should rethrow it")
    void check_whenProbeFailsWithAnotherError_shouldRethrowIt() {
        // given
        BadSqlGrammarException error = new BadSqlGrammarException(
                "probe", "SELECT", new SQLSyntaxErrorException("ORA-00942: table or view does not exist", "42000", 942)
        );
        when(jdbcTemplate.queryForList(anyString(), any(Object[].class))).thenThrow(error);
        OnceboxProperties properties = properties(outboxWithDlqAndInbox());

        // when / then
        assertThatThrownBy(() -> OnceboxOracleBatchSizeCheck.check(jdbcTemplate, properties)).isSameAs(error);
    }

    private void givenInListLimitedTo1000() {
        when(jdbcTemplate.queryForList(anyString(), any(Object[].class))).thenThrow(new BadSqlGrammarException(
                "probe",
                "SELECT",
                new SQLSyntaxErrorException("ORA-01795: maximum number of expressions in a list is 1000", "42000", 1795)
        ));
    }

    private void givenNoInListLimit() {
        when(jdbcTemplate.queryForList(anyString(), any(Object[].class))).thenReturn(List.of(Map.of("1", 1)));
    }

    private static Map<String, String> outboxWithDlqAndInbox() {
        Map<String, String> config = new HashMap<>();
        config.put(OUTBOX + "enabled", "true");
        config.put(OUTBOX + "sender.type", "kafka");
        config.put(OUTBOX + "events.order-created.topic", "orders");
        config.put(DLQ + "enabled", "true");
        config.put(INBOX + "enabled", "true");
        config.put(INBOX + "source.type", "kafka");
        return config;
    }

    private static OnceboxProperties properties(Map<String, String> config) {
        OnceboxProperties properties = new Binder(new MapConfigurationPropertySource(config))
                .bind("oncebox", OnceboxProperties.class)
                .get();
        properties.applyDefaults();
        return properties;
    }
}
