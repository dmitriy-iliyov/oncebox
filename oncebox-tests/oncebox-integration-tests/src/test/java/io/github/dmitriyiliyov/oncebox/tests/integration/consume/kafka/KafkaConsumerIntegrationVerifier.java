package io.github.dmitriyiliyov.oncebox.tests.integration.consume.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.dmitriyiliyov.oncebox.core.publisher.domain.OutboxHeaders;
import io.github.dmitriyiliyov.oncebox.tests.integration.consume.shared.ConsumerVariant;
import io.github.dmitriyiliyov.oncebox.tests.integration.domain.BusinessEvent;
import io.github.dmitriyiliyov.oncebox.tests.integration.utils.IdExtractor;
import org.awaitility.Awaitility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class KafkaConsumerIntegrationVerifier {

    private static final Duration AWAIT_AT_MOST = Duration.ofSeconds(15);
    private static final Duration POLL_INTERVAL = Duration.ofMillis(200);
    private static final String EVENT_TYPE = "test-business-event";
    private static final Logger log = LoggerFactory.getLogger(KafkaConsumerIntegrationVerifier.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final JdbcTemplate jdbcTemplate;
    private final IdExtractor idExtractor;
    private final KafkaConsumerFaultyBusinessService faultyService;
    private final ConsumerVariant variant;
    private final KafkaTestUtils kafkaTestUtils;
    private final ObjectMapper mapper = new ObjectMapper();

    public KafkaConsumerIntegrationVerifier(KafkaTemplate<String, String> kafkaTemplate,
                                            JdbcTemplate jdbcTemplate,
                                            IdExtractor idExtractor,
                                            KafkaConsumerFaultyBusinessService faultyService,
                                            KafkaTestUtils kafkaTestUtils) {
        this.kafkaTemplate = kafkaTemplate;
        this.jdbcTemplate = jdbcTemplate;
        this.idExtractor = idExtractor;
        this.faultyService = faultyService;
        this.variant = faultyService.variant();
        this.kafkaTestUtils = kafkaTestUtils;
    }

    public void consume_shouldSaveToBusinessAndConsumedTables() {
        UUID eventId  = UUID.randomUUID();
        UUID verifyId = UUID.randomUUID();

        sendSingle(eventId, verifyId);

        awaitBusinessCount(1);

        assertThat(selectBusinessVerifyIds()).containsOnly(verifyId);
        assertThat(selectConsumedEventIds()).containsOnly(eventId);
    }

    public void consumeId_shouldSaveToBusinessAndConsumedTables() {
        UUID eventId  = UUID.randomUUID();
        UUID verifyId = UUID.randomUUID();

        sendSingleId(eventId, verifyId);

        awaitBusinessCount(1);

        assertThat(selectBusinessVerifyIds()).containsOnly(verifyId);
        assertThat(selectConsumedEventIds()).containsOnly(eventId);
    }

    public void consume_shouldBeIdempotent_whenSameMessageReceivedTwice() {
        UUID eventId  = UUID.randomUUID();
        UUID verifyId = UUID.randomUUID();

        sendSingle(eventId, verifyId);
        awaitBusinessCount(1);

        sendSingle(eventId, verifyId);
        awaitStableBusinessCount(1);

        assertThat(selectBusinessVerifyIds()).containsOnly(verifyId);
        assertThat(selectConsumedEventIds()).containsOnly(eventId);
    }

    public void consumeId_shouldBeIdempotent_whenSameMessageReceivedTwice() {
        UUID eventId  = UUID.randomUUID();
        UUID verifyId = UUID.randomUUID();

        sendSingleId(eventId, verifyId);
        awaitBusinessCount(1);

        sendSingleId(eventId, verifyId);
        awaitStableBusinessCount(1);

        assertThat(selectBusinessVerifyIds()).containsOnly(verifyId);
        assertThat(selectConsumedEventIds()).containsOnly(eventId);
    }

    public void consume_shouldSaveAllSingleMessages(int count) {
        List<UUID> eventIds  = generateIds(count);
        List<UUID> verifyIds = generateIds(count);

        for (int i = 0; i < count; i++) {
            sendSingle(eventIds.get(i), verifyIds.get(i));
        }

        awaitBusinessCount(count);

        assertThat(selectBusinessVerifyIds())
                .containsExactlyInAnyOrder(verifyIds.toArray(new UUID[0]));
        assertThat(selectConsumedEventIds())
                .containsExactlyInAnyOrder(eventIds.toArray(new UUID[0]));
    }

    public void consumeId_shouldSaveAllSingleMessages(int count) {
        List<UUID> eventIds  = generateIds(count);
        List<UUID> verifyIds = generateIds(count);

        for (int i = 0; i < count; i++) {
            sendSingleId(eventIds.get(i), verifyIds.get(i));
        }

        awaitBusinessCount(count);

        assertThat(selectBusinessVerifyIds())
                .containsExactlyInAnyOrder(verifyIds.toArray(new UUID[0]));
        assertThat(selectConsumedEventIds())
                .containsExactlyInAnyOrder(eventIds.toArray(new UUID[0]));
    }

    public void consume_shouldSaveBatchToBusinessAndConsumedTables(int batchSize) {
        List<UUID> eventIds  = generateIds(batchSize);
        List<UUID> verifyIds = generateIds(batchSize);

        sendBatch(eventIds, verifyIds);

        awaitBusinessCount(batchSize);

        assertThat(selectBusinessVerifyIds())
                .containsExactlyInAnyOrder(verifyIds.toArray(new UUID[0]));
        assertThat(selectConsumedEventIds())
                .containsExactlyInAnyOrder(eventIds.toArray(new UUID[0]));
    }

    public void consumeId_shouldSaveBatchToBusinessAndConsumedTables(int batchSize) {
        List<UUID> eventIds  = generateIds(batchSize);
        List<UUID> verifyIds = generateIds(batchSize);

        sendBatchId(eventIds, verifyIds);

        awaitBusinessCount(batchSize);

        assertThat(selectBusinessVerifyIds())
                .containsExactlyInAnyOrder(verifyIds.toArray(new UUID[0]));
        assertThat(selectConsumedEventIds())
                .containsExactlyInAnyOrder(eventIds.toArray(new UUID[0]));
    }

    public void consume_shouldBeIdempotent_whenSameBatchReceivedTwice(int batchSize) {
        List<UUID> eventIds  = generateIds(batchSize);
        List<UUID> verifyIds = generateIds(batchSize);

        sendBatch(eventIds, verifyIds);
        awaitBusinessCount(batchSize);

        sendBatch(eventIds, verifyIds);
        awaitStableBusinessCount(batchSize);

        assertThat(selectBusinessVerifyIds())
                .containsExactlyInAnyOrder(verifyIds.toArray(new UUID[0]));
        assertThat(selectConsumedEventIds())
                .containsExactlyInAnyOrder(eventIds.toArray(new UUID[0]));
    }

    public void consumeId_shouldBeIdempotent_whenSameBatchReceivedTwice(int batchSize) {
        List<UUID> eventIds  = generateIds(batchSize);
        List<UUID> verifyIds = generateIds(batchSize);

        sendBatchId(eventIds, verifyIds);
        awaitBusinessCount(batchSize);

        sendBatchId(eventIds, verifyIds);
        awaitStableBusinessCount(batchSize);

        assertThat(selectBusinessVerifyIds())
                .containsExactlyInAnyOrder(verifyIds.toArray(new UUID[0]));
        assertThat(selectConsumedEventIds())
                .containsExactlyInAnyOrder(eventIds.toArray(new UUID[0]));
    }

    public void consume_shouldProcessOnlyNewMessages_whenBatchContainsDuplicates() {
        List<UUID> firstEventIds  = generateIds(3);
        List<UUID> firstVerifyIds = generateIds(3);
        sendBatch(firstEventIds, firstVerifyIds);
        awaitBusinessCount(3);

        List<UUID> newEventIds  = generateIds(3);
        List<UUID> newVerifyIds = generateIds(3);

        List<UUID> mixedEventIds  = new ArrayList<>(firstEventIds);
        mixedEventIds.addAll(newEventIds);
        List<UUID> mixedVerifyIds = new ArrayList<>(firstVerifyIds);
        mixedVerifyIds.addAll(newVerifyIds);

        sendBatch(mixedEventIds, mixedVerifyIds);
        awaitBusinessCount(6);

        List<UUID> allVerifyIds = new ArrayList<>(firstVerifyIds);
        allVerifyIds.addAll(newVerifyIds);

        assertThat(selectBusinessVerifyIds())
                .containsExactlyInAnyOrder(allVerifyIds.toArray(new UUID[0]));

        List<UUID> allEventIds = new ArrayList<>(firstEventIds);
        allEventIds.addAll(newEventIds);

        assertThat(selectConsumedEventIds())
                .containsExactlyInAnyOrder(allEventIds.toArray(new UUID[0]));
    }

    public void consumeId_shouldProcessOnlyNewMessages_whenBatchContainsDuplicates() {
        List<UUID> firstEventIds  = generateIds(3);
        List<UUID> firstVerifyIds = generateIds(3);
        sendBatchId(firstEventIds, firstVerifyIds);
        awaitBusinessCount(3);

        List<UUID> newEventIds  = generateIds(3);
        List<UUID> newVerifyIds = generateIds(3);

        List<UUID> mixedEventIds  = new ArrayList<>(firstEventIds);
        mixedEventIds.addAll(newEventIds);
        List<UUID> mixedVerifyIds = new ArrayList<>(firstVerifyIds);
        mixedVerifyIds.addAll(newVerifyIds);

        sendBatchId(mixedEventIds, mixedVerifyIds);
        awaitBusinessCount(6);

        List<UUID> allVerifyIds = new ArrayList<>(firstVerifyIds);
        allVerifyIds.addAll(newVerifyIds);

        assertThat(selectBusinessVerifyIds())
                .containsExactlyInAnyOrder(allVerifyIds.toArray(new UUID[0]));

        List<UUID> allEventIds = new ArrayList<>(firstEventIds);
        allEventIds.addAll(newEventIds);

        assertThat(selectConsumedEventIds())
                .containsExactlyInAnyOrder(allEventIds.toArray(new UUID[0]));
    }

    public void consume_shouldRollbackBothTables_whenBusinessOperationFails() {
        faultyService.setShouldFail(true);
        int failuresBefore = faultyService.failures();

        UUID eventId  = UUID.randomUUID();
        UUID verifyId = UUID.randomUUID();

        sendToFailingTopic(eventId, verifyId);

        awaitFailureSince(failuresBefore);
        awaitStableBusinessCount(0);

        assertThat(selectBusinessVerifyIds()).isEmpty();
        assertThat(selectConsumedEventIds()).isEmpty();
    }

    public void consumeId_shouldRollbackBothTables_whenBusinessOperationFails() {
        faultyService.setShouldFail(true);
        int failuresBefore = faultyService.failures();

        UUID eventId  = UUID.randomUUID();
        UUID verifyId = UUID.randomUUID();

        sendToIdFailingTopic(eventId, verifyId);

        awaitFailureSince(failuresBefore);
        awaitStableBusinessCount(0);

        assertThat(selectBusinessVerifyIds()).isEmpty();
        assertThat(selectConsumedEventIds()).isEmpty();
    }

    public void consume_shouldRollbackBothTables_whenBatchOperationFails(int batchSize) {
        faultyService.setShouldFail(true);
        int failuresBefore = faultyService.failures();

        List<UUID> eventIds  = generateIds(batchSize);
        List<UUID> verifyIds = generateIds(batchSize);

        sendBatchToFailingTopic(eventIds, verifyIds);

        awaitFailureSince(failuresBefore);
        awaitStableBusinessCount(0);

        assertThat(selectBusinessVerifyIds()).isEmpty();
        assertThat(selectConsumedEventIds()).isEmpty();
    }

    public void consumeId_shouldRollbackBothTables_whenBatchOperationFails(int batchSize) {
        faultyService.setShouldFail(true);
        int failuresBefore = faultyService.failures();

        List<UUID> eventIds  = generateIds(batchSize);
        List<UUID> verifyIds = generateIds(batchSize);

        sendBatchToIdFailingTopic(eventIds, verifyIds);

        awaitFailureSince(failuresBefore);
        awaitStableBusinessCount(0);

        assertThat(selectBusinessVerifyIds()).isEmpty();
        assertThat(selectConsumedEventIds()).isEmpty();
    }

    public void consume_shouldBeRetryable_afterTransactionRollback() {
        faultyService.setShouldFail(true);
        int failuresBefore = faultyService.failures();

        UUID eventId  = UUID.randomUUID();
        UUID verifyId = UUID.randomUUID();

        sendToFailingTopic(eventId, verifyId);
        awaitFailureSince(failuresBefore);
        awaitStableBusinessCount(0);

        assertThat(selectBusinessVerifyIds()).isEmpty();
        assertThat(selectConsumedEventIds()).isEmpty();

        faultyService.setShouldFail(false);
        sendToFailingTopic(eventId, verifyId);

        awaitBusinessCount(1);

        assertThat(selectBusinessVerifyIds()).containsOnly(verifyId);
        assertThat(selectConsumedEventIds()).containsOnly(eventId);
    }

    public void consumeId_shouldBeRetryable_afterTransactionRollback() {
        faultyService.setShouldFail(true);
        int failuresBefore = faultyService.failures();

        UUID eventId  = UUID.randomUUID();
        UUID verifyId = UUID.randomUUID();

        sendToIdFailingTopic(eventId, verifyId);
        awaitFailureSince(failuresBefore);
        awaitStableBusinessCount(0);

        assertThat(selectBusinessVerifyIds()).isEmpty();
        assertThat(selectConsumedEventIds()).isEmpty();

        faultyService.setShouldFail(false);
        sendToIdFailingTopic(eventId, verifyId);

        awaitBusinessCount(1);

        assertThat(selectBusinessVerifyIds()).containsOnly(verifyId);
        assertThat(selectConsumedEventIds()).containsOnly(eventId);
    }

    public void consume_shouldBeBatchRetryable_afterTransactionRollback(int batchSize) {
        faultyService.setShouldFail(true);
        int failuresBefore = faultyService.failures();

        List<UUID> eventIds  = generateIds(batchSize);
        List<UUID> verifyIds = generateIds(batchSize);

        sendBatchToFailingTopic(eventIds, verifyIds);
        awaitFailureSince(failuresBefore);
        awaitStableBusinessCount(0);

        assertThat(selectBusinessVerifyIds()).isEmpty();
        assertThat(selectConsumedEventIds()).isEmpty();

        faultyService.setShouldFail(false);
        sendBatchToFailingTopic(eventIds, verifyIds);

        awaitBusinessCount(batchSize);

        assertThat(selectBusinessVerifyIds())
                .containsExactlyInAnyOrder(verifyIds.toArray(new UUID[0]));
        assertThat(selectConsumedEventIds())
                .containsExactlyInAnyOrder(eventIds.toArray(new UUID[0]));
    }

    public void consumeId_shouldBeBatchRetryable_afterTransactionRollback(int batchSize) {
        faultyService.setShouldFail(true);
        int failuresBefore = faultyService.failures();

        List<UUID> eventIds  = generateIds(batchSize);
        List<UUID> verifyIds = generateIds(batchSize);

        sendBatchToIdFailingTopic(eventIds, verifyIds);
        awaitFailureSince(failuresBefore);
        awaitStableBusinessCount(0);

        assertThat(selectBusinessVerifyIds()).isEmpty();
        assertThat(selectConsumedEventIds()).isEmpty();

        faultyService.setShouldFail(false);
        sendBatchToIdFailingTopic(eventIds, verifyIds);

        awaitBusinessCount(batchSize);

        assertThat(selectBusinessVerifyIds())
                .containsExactlyInAnyOrder(verifyIds.toArray(new UUID[0]));
        assertThat(selectConsumedEventIds())
                .containsExactlyInAnyOrder(eventIds.toArray(new UUID[0]));
    }

    private void sendSingle(UUID eventId, UUID verifyId) {
        kafkaTemplate.send(buildRecord(variant.of(KafkaConsumerBusinessService.SINGLE_TOPIC), eventId, verifyId)).join();
    }

    private void sendSingleId(UUID eventId, UUID verifyId) {
        kafkaTemplate.send(buildRecord(variant.of(KafkaConsumerBusinessService.SINGLE_ID_TOPIC), eventId, verifyId)).join();
    }

    private void sendBatch(List<UUID> eventIds, List<UUID> verifyIds) {
        for (int i = 0; i < eventIds.size(); i++) {
            kafkaTemplate.send(buildRecord(variant.of(KafkaConsumerBusinessService.BATCH_TOPIC), eventIds.get(i), verifyIds.get(i))).join();
        }
    }

    private void sendBatchId(List<UUID> eventIds, List<UUID> verifyIds) {
        for (int i = 0; i < eventIds.size(); i++) {
            kafkaTemplate.send(buildRecord(variant.of(KafkaConsumerBusinessService.BATCH_ID_TOPIC), eventIds.get(i), verifyIds.get(i))).join();
        }
    }

    private Message<String> buildRecord(String topic, UUID eventId, UUID verifyId) {
        try {
            return MessageBuilder
                    .withPayload(mapper.writeValueAsString(BusinessEvent.of(verifyId)))
                    .setHeader(KafkaHeaders.TOPIC, topic)
                    .setHeader(OutboxHeaders.EVENT_ID.getValue(), eventId.toString())
                    .setHeader(OutboxHeaders.EVENT_TYPE.getValue(), EVENT_TYPE)
                    .setHeader(OutboxHeaders.EVENT_PAYLOAD_TYPE.getValue(), BusinessEvent.class.getName())
                    .build();
        } catch (JsonProcessingException e) {
            log.info("Error when build record", e);
            throw new RuntimeException(e);
        }
    }

    private void awaitBusinessCount(int expectedCount) {
        Awaitility.await()
                .atMost(AWAIT_AT_MOST)
                .pollInterval(POLL_INTERVAL)
                .untilAsserted(() ->
                        assertThat(countBusinessEvents()).isEqualTo(expectedCount)
                );
    }

    private void awaitFailureSince(int failuresBefore) {
        Awaitility.await()
                .atMost(AWAIT_AT_MOST)
                .pollInterval(POLL_INTERVAL)
                .untilAsserted(() -> assertThat(faultyService.failures()).isGreaterThan(failuresBefore));
    }

    private void awaitStableBusinessCount(int stableCount) {
        Awaitility.await()
                .atMost(AWAIT_AT_MOST)
                .pollInterval(POLL_INTERVAL)
                .during(Duration.ofSeconds(2))
                .untilAsserted(() ->
                        assertThat(countBusinessEvents()).isEqualTo(stableCount)
                );
    }

    private int countBusinessEvents() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM business_events", Integer.class);
        return count != null ? count : 0;
    }

    private List<UUID> selectBusinessVerifyIds() {
        return jdbcTemplate.query(
                "SELECT verify_id FROM business_events",
                (rs, n) -> idExtractor.extract("verify_id", rs)
        );
    }

    private List<UUID> selectConsumedEventIds() {
        return jdbcTemplate.query(
                "SELECT id FROM outbox_consumed_events",
                (rs, n) -> idExtractor.extract("id", rs)
        );
    }

    private static List<UUID> generateIds(int count) {
        List<UUID> ids = new ArrayList<>(count);
        for (int i = 0; i < count; i++) ids.add(UUID.randomUUID());
        return ids;
    }

    private void sendToFailingTopic(UUID eventId, UUID verifyId) {
        kafkaTemplate.send(buildRecord(variant.of(KafkaConsumerFaultyBusinessService.SINGLE_FAILING_TOPIC), eventId, verifyId)).join();
    }

    private void sendToIdFailingTopic(UUID eventId, UUID verifyId) {
        kafkaTemplate.send(buildRecord(variant.of(KafkaConsumerFaultyBusinessService.SINGLE_ID_FAILING_TOPIC), eventId, verifyId)).join();
    }

    private void sendBatchToFailingTopic(List<UUID> eventIds, List<UUID> verifyIds) {
        for (int i = 0; i < eventIds.size(); i++) {
            kafkaTemplate.send(buildRecord(variant.of(KafkaConsumerFaultyBusinessService.BATCH_FAILING_TOPIC), eventIds.get(i), verifyIds.get(i))).join();
        }
    }

    private void sendBatchToIdFailingTopic(List<UUID> eventIds, List<UUID> verifyIds) {
        for (int i = 0; i < eventIds.size(); i++) {
            kafkaTemplate.send(buildRecord(variant.of(KafkaConsumerFaultyBusinessService.BATCH_ID_FAILING_TOPIC), eventIds.get(i), verifyIds.get(i))).join();
        }
    }

    public void cleanUpQueries() {
        // Back to failing: a message of the previous rollback case may still be redelivered, and it must not
        // succeed now and leave its row in the next case's tables.
        faultyService.setShouldFail(true);
        jdbcTemplate.execute("DELETE FROM business_events");
        jdbcTemplate.execute("DELETE FROM outbox_consumed_events");
        kafkaTestUtils.resetKafkaTopics(List.of(
                variant.of(KafkaConsumerBusinessService.SINGLE_TOPIC),
                variant.of(KafkaConsumerBusinessService.BATCH_TOPIC),
                variant.of(KafkaConsumerBusinessService.SINGLE_ID_TOPIC),
                variant.of(KafkaConsumerBusinessService.BATCH_ID_TOPIC),
                variant.of(KafkaConsumerFaultyBusinessService.SINGLE_FAILING_TOPIC),
                variant.of(KafkaConsumerFaultyBusinessService.BATCH_FAILING_TOPIC),
                variant.of(KafkaConsumerFaultyBusinessService.SINGLE_ID_FAILING_TOPIC),
                variant.of(KafkaConsumerFaultyBusinessService.BATCH_ID_FAILING_TOPIC)
        ));
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}