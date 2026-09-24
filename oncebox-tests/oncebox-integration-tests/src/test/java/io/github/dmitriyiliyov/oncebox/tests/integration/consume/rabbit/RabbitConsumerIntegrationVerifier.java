package io.github.dmitriyiliyov.oncebox.tests.integration.consume.rabbit;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.dmitriyiliyov.oncebox.core.publisher.domain.OutboxHeaders;
import io.github.dmitriyiliyov.oncebox.tests.integration.consume.shared.ConsumerVariant;
import io.github.dmitriyiliyov.oncebox.tests.integration.domain.BusinessEvent;
import io.github.dmitriyiliyov.oncebox.tests.integration.utils.IdExtractor;
import org.awaitility.Awaitility;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class RabbitConsumerIntegrationVerifier {

    private static final Duration AWAIT_AT_MOST = Duration.ofSeconds(15);
    private static final Duration POLL_INTERVAL = Duration.ofMillis(200);
    private static final String EVENT_TYPE = "test-business-event";

    private final RabbitTemplate rabbitTemplate;
    private final JdbcTemplate jdbcTemplate;
    private final IdExtractor idExtractor;
    private final RabbitConsumerFaultyBusinessService faultyService;
    private final ConsumerVariant variant;
    private final ObjectMapper objectMapper;
    private final RabbitAdmin rabbitAdmin;

    public RabbitConsumerIntegrationVerifier(RabbitTemplate rabbitTemplate,
                                             JdbcTemplate jdbcTemplate,
                                             IdExtractor idExtractor,
                                             RabbitConsumerFaultyBusinessService faultyService) {
        this.rabbitTemplate = rabbitTemplate;
        this.jdbcTemplate = jdbcTemplate;
        this.idExtractor = idExtractor;
        this.faultyService = faultyService;
        this.variant = faultyService.variant();
        this.objectMapper = new ObjectMapper();
        this.rabbitAdmin = new RabbitAdmin(rabbitTemplate.getConnectionFactory());
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

        sendToFailingQueue(eventId, verifyId);

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

        sendToIdFailingQueue(eventId, verifyId);

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

        sendBatchToFailingQueue(eventIds, verifyIds);

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

        sendBatchToIdFailingQueue(eventIds, verifyIds);

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

        sendToFailingQueue(eventId, verifyId);
        awaitFailureSince(failuresBefore);
        awaitStableBusinessCount(0);

        assertThat(selectBusinessVerifyIds()).isEmpty();
        assertThat(selectConsumedEventIds()).isEmpty();

        try {
            Thread.sleep(2000);

            rabbitAdmin.purgeQueue(variant.of(RabbitConsumerFaultyBusinessService.SINGLE_FAILING_QUEUE), true);
            Thread.sleep(1000);
        } catch (InterruptedException ie) {
            throw new RuntimeException(ie);
        }

        faultyService.setShouldFail(false);
        sendToFailingQueue(eventId, verifyId);

        awaitBusinessCount(1);

        assertThat(selectBusinessVerifyIds()).containsOnly(verifyId);
        assertThat(selectConsumedEventIds()).containsOnly(eventId);
    }

    public void consumeId_shouldBeRetryable_afterTransactionRollback() {
        faultyService.setShouldFail(true);
        int failuresBefore = faultyService.failures();

        UUID eventId  = UUID.randomUUID();
        UUID verifyId = UUID.randomUUID();

        sendToIdFailingQueue(eventId, verifyId);
        awaitFailureSince(failuresBefore);
        awaitStableBusinessCount(0);

        assertThat(selectBusinessVerifyIds()).isEmpty();
        assertThat(selectConsumedEventIds()).isEmpty();

        try {
            Thread.sleep(2000);

            rabbitAdmin.purgeQueue(variant.of(RabbitConsumerFaultyBusinessService.SINGLE_ID_FAILING_QUEUE), true);
            Thread.sleep(1000);
        } catch (InterruptedException ie) {
            throw new RuntimeException(ie);
        }

        faultyService.setShouldFail(false);
        sendToIdFailingQueue(eventId, verifyId);

        awaitBusinessCount(1);

        assertThat(selectBusinessVerifyIds()).containsOnly(verifyId);
        assertThat(selectConsumedEventIds()).containsOnly(eventId);
    }

    public void consume_shouldBeBatchRetryable_afterTransactionRollback(int batchSize) {
        faultyService.setShouldFail(true);
        int failuresBefore = faultyService.failures();

        List<UUID> eventIds  = generateIds(batchSize);
        List<UUID> verifyIds = generateIds(batchSize);

        sendBatchToFailingQueue(eventIds, verifyIds);
        awaitFailureSince(failuresBefore);
        awaitStableBusinessCount(0);

        assertThat(selectBusinessVerifyIds()).isEmpty();
        assertThat(selectConsumedEventIds()).isEmpty();

        try {
            Thread.sleep(2000);

            rabbitAdmin.purgeQueue(variant.of(RabbitConsumerFaultyBusinessService.BATCH_FAILING_QUEUE), true);
            Thread.sleep(1000);
        } catch (InterruptedException ie) {
            throw new RuntimeException(ie);
        }

        faultyService.setShouldFail(false);
        sendBatchToFailingQueue(eventIds, verifyIds);

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

        sendBatchToIdFailingQueue(eventIds, verifyIds);
        awaitFailureSince(failuresBefore);
        awaitStableBusinessCount(0);

        assertThat(selectBusinessVerifyIds()).isEmpty();
        assertThat(selectConsumedEventIds()).isEmpty();

        try {
            Thread.sleep(2000);

            rabbitAdmin.purgeQueue(variant.of(RabbitConsumerFaultyBusinessService.BATCH_ID_FAILING_QUEUE), true);
            Thread.sleep(1000);
        } catch (InterruptedException ie) {
            throw new RuntimeException(ie);
        }

        faultyService.setShouldFail(false);
        sendBatchToIdFailingQueue(eventIds, verifyIds);

        awaitBusinessCount(batchSize);

        assertThat(selectBusinessVerifyIds())
                .containsExactlyInAnyOrder(verifyIds.toArray(new UUID[0]));
        assertThat(selectConsumedEventIds())
                .containsExactlyInAnyOrder(eventIds.toArray(new UUID[0]));
    }

    private void sendSingle(UUID eventId, UUID verifyId) {
        rabbitTemplate.send(variant.of(RabbitConsumerBusinessService.SINGLE_QUEUE), buildMessage(eventId, verifyId));
    }

    private void sendSingleId(UUID eventId, UUID verifyId) {
        rabbitTemplate.send(variant.of(RabbitConsumerBusinessService.SINGLE_ID_QUEUE), buildMessage(eventId, verifyId));
    }

    private void sendBatch(List<UUID> eventIds, List<UUID> verifyIds) {
        for (int i = 0; i < eventIds.size(); i++) {
            rabbitTemplate.send(variant.of(RabbitConsumerBusinessService.BATCH_QUEUE), buildMessage(eventIds.get(i), verifyIds.get(i)));
        }
    }

    private void sendBatchId(List<UUID> eventIds, List<UUID> verifyIds) {
        for (int i = 0; i < eventIds.size(); i++) {
            rabbitTemplate.send(variant.of(RabbitConsumerBusinessService.BATCH_ID_QUEUE), buildMessage(eventIds.get(i), verifyIds.get(i)));
        }
    }

    private void sendToFailingQueue(UUID eventId, UUID verifyId) {
        rabbitTemplate.send(variant.of(RabbitConsumerFaultyBusinessService.SINGLE_FAILING_QUEUE), buildMessage(eventId, verifyId));
    }

    private void sendToIdFailingQueue(UUID eventId, UUID verifyId) {
        rabbitTemplate.send(variant.of(RabbitConsumerFaultyBusinessService.SINGLE_ID_FAILING_QUEUE), buildMessage(eventId, verifyId));
    }

    private void sendBatchToFailingQueue(List<UUID> eventIds, List<UUID> verifyIds) {
        for (int i = 0; i < eventIds.size(); i++) {
            rabbitTemplate.send(variant.of(RabbitConsumerFaultyBusinessService.BATCH_FAILING_QUEUE), buildMessage(eventIds.get(i), verifyIds.get(i)));
        }
    }

    private void sendBatchToIdFailingQueue(List<UUID> eventIds, List<UUID> verifyIds) {
        for (int i = 0; i < eventIds.size(); i++) {
            rabbitTemplate.send(variant.of(RabbitConsumerFaultyBusinessService.BATCH_ID_FAILING_QUEUE), buildMessage(eventIds.get(i), verifyIds.get(i)));
        }
    }

    private Message buildMessage(UUID eventId, UUID verifyId) {
        try {
            byte[] body = objectMapper.writeValueAsBytes(BusinessEvent.of(verifyId));
            MessageProperties props = new MessageProperties();
            props.setContentType(MessageProperties.CONTENT_TYPE_JSON);
            props.setHeader(OutboxHeaders.EVENT_ID.getValue(), eventId.toString());
            props.setHeader(OutboxHeaders.EVENT_TYPE.getValue(), EVENT_TYPE);
            return new Message(body, props);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize BusinessEvent", e);
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

    public void cleanUpQueries() {

        String[] queuesToPurge = {
                variant.of(RabbitConsumerBusinessService.SINGLE_QUEUE),
                variant.of(RabbitConsumerBusinessService.SINGLE_ID_QUEUE),
                variant.of(RabbitConsumerBusinessService.BATCH_QUEUE),
                variant.of(RabbitConsumerBusinessService.BATCH_ID_QUEUE),
                variant.of(RabbitConsumerFaultyBusinessService.SINGLE_FAILING_QUEUE),
                variant.of(RabbitConsumerFaultyBusinessService.SINGLE_ID_FAILING_QUEUE),
                variant.of(RabbitConsumerFaultyBusinessService.BATCH_FAILING_QUEUE),
                variant.of(RabbitConsumerFaultyBusinessService.BATCH_ID_FAILING_QUEUE)
        };

        for (String queue : queuesToPurge) {
            try {
                rabbitAdmin.purgeQueue(queue, true);
            } catch (Exception e) { }
        }

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        jdbcTemplate.execute("DELETE FROM business_events");
        jdbcTemplate.execute("DELETE FROM outbox_consumed_events");

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}