package io.github.dmitriyiliyov.oncebox.tests.integration.consume.rabbit;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.dmitriyiliyov.oncebox.core.OnceboxHeaders;
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

/**
 * At-most-once cases of the non-transactional consumer: the operation fails after the IDs were recorded, the event
 * is sent once more - the containers reject a failed message without requeueing it - and the operation is not run
 * again.
 */
public class RabbitAtMostOnceConsumerIntegrationVerifier {

    private static final Duration AWAIT_AT_MOST = Duration.ofSeconds(15);
    private static final Duration POLL_INTERVAL = Duration.ofMillis(200);
    private static final String EVENT_TYPE = "test-business-event";

    private final RabbitTemplate rabbitTemplate;
    private final JdbcTemplate jdbcTemplate;
    private final IdExtractor idExtractor;
    private final RabbitAtMostOnceBusinessService service;
    private final ConsumerVariant variant;
    private final RabbitAdmin rabbitAdmin;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RabbitAtMostOnceConsumerIntegrationVerifier(RabbitTemplate rabbitTemplate,
                                                       JdbcTemplate jdbcTemplate,
                                                       IdExtractor idExtractor,
                                                       RabbitAtMostOnceBusinessService service) {
        this.rabbitTemplate = rabbitTemplate;
        this.jdbcTemplate = jdbcTemplate;
        this.idExtractor = idExtractor;
        this.service = service;
        this.variant = service.variant();
        this.rabbitAdmin = new RabbitAdmin(rabbitTemplate.getConnectionFactory());
    }

    public void consume_whenOperationFailsAndEventIsRedelivered_shouldNotRunOperationAgain() {
        verifyNotRunAgain(RabbitAtMostOnceBusinessService.SINGLE_QUEUE, 1);
    }

    public void consumeId_whenOperationFailsAndEventIsRedelivered_shouldNotRunOperationAgain() {
        verifyNotRunAgain(RabbitAtMostOnceBusinessService.SINGLE_ID_QUEUE, 1);
    }

    public void consume_whenBatchOperationFailsAndBatchIsRedelivered_shouldNotRunOperationAgain(int batchSize) {
        verifyNotRunAgain(RabbitAtMostOnceBusinessService.BATCH_QUEUE, batchSize);
    }

    public void consumeId_whenBatchOperationFailsAndBatchIsRedelivered_shouldNotRunOperationAgain(int batchSize) {
        verifyNotRunAgain(RabbitAtMostOnceBusinessService.BATCH_ID_QUEUE, batchSize);
    }

    public void cleanUp() {
        for (String queue : List.of(
                RabbitAtMostOnceBusinessService.SINGLE_QUEUE,
                RabbitAtMostOnceBusinessService.BATCH_QUEUE,
                RabbitAtMostOnceBusinessService.SINGLE_ID_QUEUE,
                RabbitAtMostOnceBusinessService.BATCH_ID_QUEUE)) {
            rabbitAdmin.purgeQueue(variant.of(queue), true);
        }
        service.runs().clear();
    }

    private void verifyNotRunAgain(String baseQueue, int count) {
        List<UUID> eventIds = generateIds(count);
        List<UUID> verifyIds = generateIds(count);

        send(baseQueue, eventIds, verifyIds);
        awaitEveryEventRan(eventIds);

        send(baseQueue, eventIds, verifyIds);
        awaitEveryEventRanOnceOnly(eventIds);

        assertThat(selectConsumedEventIds()).containsExactlyInAnyOrderElementsOf(eventIds);
        assertThat(selectBusinessVerifyIds()).containsExactlyInAnyOrderElementsOf(verifyIds);
    }

    private void send(String baseQueue, List<UUID> eventIds, List<UUID> verifyIds) {
        for (int i = 0; i < eventIds.size(); i++) {
            rabbitTemplate.send(variant.of(baseQueue), buildMessage(eventIds.get(i), verifyIds.get(i)));
        }
    }

    private Message buildMessage(UUID eventId, UUID verifyId) {
        try {
            byte [] body = objectMapper.writeValueAsBytes(BusinessEvent.of(verifyId));
            MessageProperties props = new MessageProperties();
            props.setContentType(MessageProperties.CONTENT_TYPE_JSON);
            props.setHeader(OnceboxHeaders.EVENT_ID.getValue(), eventId.toString());
            props.setHeader(OnceboxHeaders.EVENT_TYPE.getValue(), EVENT_TYPE);
            return new Message(body, props);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize BusinessEvent", e);
        }
    }

    private void awaitEveryEventRan(List<UUID> eventIds) {
        Awaitility.await()
                .atMost(AWAIT_AT_MOST)
                .pollInterval(POLL_INTERVAL)
                .untilAsserted(() -> assertThat(service.runs().of(eventIds)).hasSize(eventIds.size()));
    }

    private void awaitEveryEventRanOnceOnly(List<UUID> eventIds) {
        Awaitility.await()
                .atMost(AWAIT_AT_MOST)
                .pollInterval(POLL_INTERVAL)
                .during(Duration.ofSeconds(2))
                .untilAsserted(() -> assertThat(service.runs().of(eventIds))
                        .hasSize(eventIds.size())
                        .allSatisfy((id, runs) -> assertThat(runs).as("runs of %s", id).isEqualTo(1)));
    }

    private List<UUID> selectBusinessVerifyIds() {
        return jdbcTemplate.query(
                "SELECT verify_id FROM business_events",
                (rs, n) -> idExtractor.extract("verify_id", rs)
        );
    }

    private List<UUID> selectConsumedEventIds() {
        return jdbcTemplate.query(
                "SELECT id FROM inbox_events",
                (rs, n) -> idExtractor.extract("id", rs)
        );
    }

    private static List<UUID> generateIds(int count) {
        List<UUID> ids = new ArrayList<>(count);
        for (int i = 0; i < count; i++) ids.add(UUID.randomUUID());
        return ids;
    }
}
