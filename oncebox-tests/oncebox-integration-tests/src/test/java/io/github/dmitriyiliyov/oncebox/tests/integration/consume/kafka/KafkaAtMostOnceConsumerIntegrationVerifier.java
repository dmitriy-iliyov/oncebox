package io.github.dmitriyiliyov.oncebox.tests.integration.consume.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.dmitriyiliyov.oncebox.core.OnceboxHeaders;
import io.github.dmitriyiliyov.oncebox.tests.integration.consume.shared.ConsumerVariant;
import io.github.dmitriyiliyov.oncebox.tests.integration.domain.BusinessEvent;
import io.github.dmitriyiliyov.oncebox.tests.integration.utils.IdExtractor;
import org.awaitility.Awaitility;
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

/**
 * At-most-once cases of the non-transactional consumer: the operation fails after the IDs were recorded, the event
 * comes back - first by the retries of the container's error handler, then sent once more - and the operation is
 * not run again.
 */
public class KafkaAtMostOnceConsumerIntegrationVerifier {

    private static final Duration AWAIT_AT_MOST = Duration.ofSeconds(15);
    private static final Duration POLL_INTERVAL = Duration.ofMillis(200);
    private static final String EVENT_TYPE = "test-business-event";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final JdbcTemplate jdbcTemplate;
    private final IdExtractor idExtractor;
    private final KafkaAtMostOnceBusinessService service;
    private final ConsumerVariant variant;
    private final KafkaTestUtils kafkaTestUtils;
    private final ObjectMapper mapper = new ObjectMapper();

    public KafkaAtMostOnceConsumerIntegrationVerifier(KafkaTemplate<String, String> kafkaTemplate,
                                                      JdbcTemplate jdbcTemplate,
                                                      IdExtractor idExtractor,
                                                      KafkaAtMostOnceBusinessService service,
                                                      KafkaTestUtils kafkaTestUtils) {
        this.kafkaTemplate = kafkaTemplate;
        this.jdbcTemplate = jdbcTemplate;
        this.idExtractor = idExtractor;
        this.service = service;
        this.variant = service.variant();
        this.kafkaTestUtils = kafkaTestUtils;
    }

    public void consume_whenOperationFailsAndEventIsRedelivered_shouldNotRunOperationAgain() {
        verifyNotRunAgain(KafkaAtMostOnceBusinessService.SINGLE_TOPIC, 1);
    }

    public void consumeId_whenOperationFailsAndEventIsRedelivered_shouldNotRunOperationAgain() {
        verifyNotRunAgain(KafkaAtMostOnceBusinessService.SINGLE_ID_TOPIC, 1);
    }

    public void consume_whenBatchOperationFailsAndBatchIsRedelivered_shouldNotRunOperationAgain(int batchSize) {
        verifyNotRunAgain(KafkaAtMostOnceBusinessService.BATCH_TOPIC, batchSize);
    }

    public void consumeId_whenBatchOperationFailsAndBatchIsRedelivered_shouldNotRunOperationAgain(int batchSize) {
        verifyNotRunAgain(KafkaAtMostOnceBusinessService.BATCH_ID_TOPIC, batchSize);
    }

    public void cleanUp() {
        kafkaTestUtils.resetKafkaTopics(List.of(
                variant.of(KafkaAtMostOnceBusinessService.SINGLE_TOPIC),
                variant.of(KafkaAtMostOnceBusinessService.BATCH_TOPIC),
                variant.of(KafkaAtMostOnceBusinessService.SINGLE_ID_TOPIC),
                variant.of(KafkaAtMostOnceBusinessService.BATCH_ID_TOPIC)
        ));
        service.runs().clear();
    }

    private void verifyNotRunAgain(String baseTopic, int count) {
        List<UUID> eventIds = generateIds(count);
        List<UUID> verifyIds = generateIds(count);

        send(baseTopic, eventIds, verifyIds);
        awaitEveryEventRan(eventIds);

        send(baseTopic, eventIds, verifyIds);
        awaitEveryEventRanOnceOnly(eventIds);

        assertThat(selectConsumedEventIds()).containsExactlyInAnyOrderElementsOf(eventIds);
        assertThat(selectBusinessVerifyIds()).containsExactlyInAnyOrderElementsOf(verifyIds);
    }

    private void send(String baseTopic, List<UUID> eventIds, List<UUID> verifyIds) {
        for (int i = 0; i < eventIds.size(); i++) {
            kafkaTemplate.send(buildRecord(variant.of(baseTopic), eventIds.get(i), verifyIds.get(i))).join();
        }
    }

    private Message<String> buildRecord(String topic, UUID eventId, UUID verifyId) {
        try {
            return MessageBuilder
                    .withPayload(mapper.writeValueAsString(BusinessEvent.of(verifyId)))
                    .setHeader(KafkaHeaders.TOPIC, topic)
                    .setHeader(OnceboxHeaders.EVENT_ID.getValue(), eventId.toString())
                    .setHeader(OnceboxHeaders.EVENT_TYPE.getValue(), EVENT_TYPE)
                    .setHeader(OnceboxHeaders.EVENT_PAYLOAD_TYPE.getValue(), BusinessEvent.class.getName())
                    .build();
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
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
