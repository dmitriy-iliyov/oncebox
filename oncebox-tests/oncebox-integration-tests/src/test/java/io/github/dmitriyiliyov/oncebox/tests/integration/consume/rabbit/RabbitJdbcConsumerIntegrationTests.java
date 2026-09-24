package io.github.dmitriyiliyov.oncebox.tests.integration.consume.rabbit;

import io.github.dmitriyiliyov.oncebox.tests.integration.utils.IdExtractor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.stream.Stream;

public class RabbitJdbcConsumerIntegrationTests extends BaseRabbitIntegrationTests {

    private final RabbitConsumerIntegrationVerifier verifier;

    static Stream<Arguments> batchSizeArguments() {
        return Stream.of(
                Arguments.of(100, 1000)
        );
    }

    public RabbitJdbcConsumerIntegrationTests(
            RabbitTemplate testRabbitTemplate,
            @Qualifier("outboxJdbcTemplate") JdbcTemplate jdbcTemplate,
            @Qualifier("rabbitMqJdbcFaultyConsumerBusinessService") RabbitConsumerFaultyBusinessService rabbitMqFaultyJdbcConsumerBusinessService,
            IdExtractor idExtractor
    ) {
        this.verifier = new RabbitConsumerIntegrationVerifier(
                testRabbitTemplate,
                jdbcTemplate,
                idExtractor,
                rabbitMqFaultyJdbcConsumerBusinessService
        );
    }

    @BeforeEach
    void cleanUpBefore() { verifier.cleanUpQueries(); }

    @Test
    @DisplayName("IT consume() should save event id to consumed table and run business operation")
    void consume_shouldSaveToBusinessAndConsumedTables() {
        verifier.consume_shouldSaveToBusinessAndConsumedTables();
    }

    @Test
    @DisplayName("IT consume() should be idempotent: same message received twice -> business op runs once")
    void consume_shouldBeIdempotent_whenSameMessageReceivedTwice() {
        verifier.consume_shouldBeIdempotent_whenSameMessageReceivedTwice();
    }

    @MethodSource("batchSizeArguments")
    @ParameterizedTest
    @DisplayName("IT consume() should process all distinct single messages")
    void consume_shouldSaveAllSingleMessages(int count) {
        verifier.consume_shouldSaveAllSingleMessages(count);
    }

    @MethodSource("batchSizeArguments")
    @ParameterizedTest
    @DisplayName("IT consume() batch: all new records saved to both tables")
    void consume_shouldSaveBatchToBusinessAndConsumedTables(int batchSize) {
        verifier.consume_shouldSaveBatchToBusinessAndConsumedTables(batchSize);
    }

    @MethodSource("batchSizeArguments")
    @ParameterizedTest
    @DisplayName("IT consume() batch: same batch delivered twice -> each record processed once")
    void consume_shouldBeIdempotent_whenSameBatchReceivedTwice(int batchSize) {
        verifier.consume_shouldBeIdempotent_whenSameBatchReceivedTwice(batchSize);
    }

    @Test
    @DisplayName("IT consume() batch: only new records processed when batch contains duplicates")
    void consume_shouldProcessOnlyNewMessages_whenBatchContainsDuplicates() {
        verifier.consume_shouldProcessOnlyNewMessages_whenBatchContainsDuplicates();
    }

    @Test
    @DisplayName("IT consume() should rollback both tables when business operation throws (single)")
    void consume_shouldRollbackBothTables_whenBusinessOperationFails() {
        verifier.consume_shouldRollbackBothTables_whenBusinessOperationFails();
    }

    @MethodSource("batchSizeArguments")
    @ParameterizedTest
    @DisplayName("IT consume() should rollback both tables when batch operation throws")
    void consume_shouldRollbackBothTables_whenBatchOperationFails(int batchSize) {
        verifier.consume_shouldRollbackBothTables_whenBatchOperationFails(batchSize);
    }

    @Test
    @DisplayName("IT consume() should allow retry after rollback: event id not burned on failure (single)")
    void consume_shouldBeRetryable_afterTransactionRollback() {
        verifier.consume_shouldBeRetryable_afterTransactionRollback();
    }

    @MethodSource("batchSizeArguments")
    @ParameterizedTest
    @DisplayName("IT consume() should allow retry after rollback: event id not burned on failure (batch)")
    void consume_shouldBeBatchRetryable_afterTransactionRollback(int batchSize) {
        verifier.consume_shouldBeBatchRetryable_afterTransactionRollback(batchSize);
    }

    @Test
    @DisplayName("IT consume() ID should save event id to consumed table and run business operation")
    void consumeId_shouldSaveToBusinessAndConsumedTables() {
        verifier.consumeId_shouldSaveToBusinessAndConsumedTables();
    }

    @Test
    @DisplayName("IT consume() ID should be idempotent: same message received twice -> business op runs once")
    void consumeId_shouldBeIdempotent_whenSameMessageReceivedTwice() {
        verifier.consumeId_shouldBeIdempotent_whenSameMessageReceivedTwice();
    }

    @MethodSource("batchSizeArguments")
    @ParameterizedTest
    @DisplayName("IT consume() ID should process all distinct single messages")
    void consumeId_shouldSaveAllSingleMessages(int count) {
        verifier.consumeId_shouldSaveAllSingleMessages(count);
    }

    @MethodSource("batchSizeArguments")
    @ParameterizedTest
    @DisplayName("IT consume() ID batch: all new records saved to both tables")
    void consumeId_shouldSaveBatchToBusinessAndConsumedTables(int batchSize) {
        verifier.consumeId_shouldSaveBatchToBusinessAndConsumedTables(batchSize);
    }

    @MethodSource("batchSizeArguments")
    @ParameterizedTest
    @DisplayName("IT consume() ID batch: same batch delivered twice -> each record processed once")
    void consumeId_shouldBeIdempotent_whenSameBatchReceivedTwice(int batchSize) {
        verifier.consumeId_shouldBeIdempotent_whenSameBatchReceivedTwice(batchSize);
    }

    @Test
    @DisplayName("IT consume() ID batch: only new records processed when batch contains duplicates")
    void consumeId_shouldProcessOnlyNewMessages_whenBatchContainsDuplicates() {
        verifier.consumeId_shouldProcessOnlyNewMessages_whenBatchContainsDuplicates();
    }

    @Test
    @DisplayName("IT consume() ID should rollback both tables when business operation throws (single)")
    void consumeId_shouldRollbackBothTables_whenBusinessOperationFails() {
        verifier.consumeId_shouldRollbackBothTables_whenBusinessOperationFails();
    }

    @MethodSource("batchSizeArguments")
    @ParameterizedTest
    @DisplayName("IT consume() ID should rollback both tables when batch operation throws")
    void consumeId_shouldRollbackBothTables_whenBatchOperationFails(int batchSize) {
        verifier.consumeId_shouldRollbackBothTables_whenBatchOperationFails(batchSize);
    }

    @Test
    @DisplayName("IT consume() ID should allow retry after rollback: event id not burned on failure (single)")
    void consumeId_shouldBeRetryable_afterTransactionRollback() {
        verifier.consumeId_shouldBeRetryable_afterTransactionRollback();
    }

    @MethodSource("batchSizeArguments")
    @ParameterizedTest
    @DisplayName("IT consume() ID should allow retry after rollback: event id not burned on failure (batch)")
    void consumeId_shouldBeBatchRetryable_afterTransactionRollback(int batchSize) {
        verifier.consumeId_shouldBeBatchRetryable_afterTransactionRollback(batchSize);
    }
}
