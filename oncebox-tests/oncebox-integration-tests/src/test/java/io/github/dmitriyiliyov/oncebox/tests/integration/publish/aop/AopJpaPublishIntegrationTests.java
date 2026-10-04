package io.github.dmitriyiliyov.oncebox.tests.integration.publish.aop;

import io.github.dmitriyiliyov.oncebox.tests.integration.publish.config.BaseOutboxIntegrationTests;
import io.github.dmitriyiliyov.oncebox.tests.integration.utils.IdExtractor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.stream.Stream;

public class AopJpaPublishIntegrationTests extends BaseOutboxIntegrationTests {

    private final AopPublishIntegrationVerifier verifier;

    static Stream<Arguments> arguments() {
        return Stream.of(
                Arguments.of(100),
                Arguments.of(1000)
        );
    }

    public AopJpaPublishIntegrationTests(
            @Qualifier("aopJpaBusinessService") AopBusinessService service,
            @Qualifier("onceboxJdbcTemplate") JdbcTemplate jdbcTemplate,
            IdExtractor idExtractor
    ) {
        this.verifier = new AopPublishIntegrationVerifier(
                service, jdbcTemplate, idExtractor
        );
    }

    @BeforeEach
    void cleanUpBefore() {
        verifier.cleanUpQueries();
    }

    @AfterEach
    void cleanUpAfter() {
        verifier.cleanUpQueries();
    }

    @Test
    @DisplayName("IT publish() should save single event to both business and outbox tables")
    void publish_shouldSaveEvent() {
        verifier.publish_shouldSaveEvent();
    }

    @MethodSource("arguments")
    @ParameterizedTest
    @DisplayName("IT publish() should save multiple events to both business and outbox tables")
    void publish_shouldSaveEvents(int eventCount) {
        verifier.publish_shouldSaveEvents(eventCount);
    }

    @Test
    @DisplayName("IT publish() should save single returned event to both business and outbox tables")
    void publish_successSaveReturnedEvent() {
        verifier.publish_successSaveReturnedEvent();
    }

    @MethodSource("arguments")
    @ParameterizedTest
    @DisplayName("IT publish() should save multiple returned events to both business and outbox tables")
    void publish_successSaveReturnedEvents(int eventCount) {
        verifier.publish_successSaveReturnedEvents(eventCount);
    }

    @Test
    @DisplayName("IT publish() should rollback both business and outbox when business transaction fails (single event)")
    void publishEvent_shouldThrows_whenBusinessTransactionFailed() {
        verifier.publishEvent_shouldThrows_whenBusinessTransactionFailed();
    }

    @Test
    @DisplayName("IT publish() should rollback both business and outbox when business transaction fails (multiple events)")
    void publishEvents_shouldThrows_whenBusinessTransactionFailed() {
        verifier.publishEvents_shouldThrows_whenBusinessTransactionFailed();
    }

    @Test
    @DisplayName("IT publish() should rollback both business and outbox when business transaction fails (single returned event)")
    void publishEventWithReturnedEvent_shouldThrows_whenBusinessTransactionFailed() {
        verifier.publishEventWithReturnedEvent_shouldThrows_whenBusinessTransactionFailed();
    }

    @Test
    @DisplayName("IT publish() should rollback both business and outbox when business transaction fails (multiple returned events)")
    void publishEventWithReturnedEvents_shouldThrows_whenBusinessTransactionFailed() {
        verifier.publishEventsWithReturnedEvents_shouldThrows_whenBusinessTransactionFailed();
    }
}
