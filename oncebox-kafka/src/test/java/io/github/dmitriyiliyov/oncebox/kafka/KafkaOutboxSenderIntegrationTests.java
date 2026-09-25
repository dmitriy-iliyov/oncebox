package io.github.dmitriyiliyov.oncebox.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.dmitriyiliyov.oncebox.core.publisher.domain.EventStatus;
import io.github.dmitriyiliyov.oncebox.core.publisher.domain.OutboxEvent;
import io.github.dmitriyiliyov.oncebox.core.publisher.domain.OutboxHeaders;
import io.github.dmitriyiliyov.oncebox.core.publisher.domain.SenderResult;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(classes = KafkaOutboxSenderIntegrationTests.KafkaTestConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ActiveProfiles("kafka-it")
class KafkaOutboxSenderIntegrationTests {

    private static final String TOPIC = "integration-test-topic";

    static final KafkaContainer kafka;

    static {
        kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.0"));
        kafka.start();
    }

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.kafka.consumer.auto-offset-reset", () -> "earliest");
        registry.add("spring.kafka.consumer.group-id", () -> "test-group");
        registry.add("spring.kafka.producer.value-serializer",
                () -> "org.springframework.kafka.support.serializer.JsonSerializer");
        registry.add("spring.kafka.consumer.value-deserializer",
                () -> "org.springframework.kafka.support.serializer.JsonDeserializer");
        registry.add("spring.kafka.consumer.properties.spring.json.trusted.packages", () -> "*");
    }

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private KafkaMessageReceiver receiver;

    private KafkaOutboxSender sender;

    @BeforeEach
    void setUp() {
        receiver.clear();
        sender = new KafkaOutboxSender(kafkaTemplate, 5_000L);
    }

    @Test
    @DisplayName("IT sendEvents() should successfully send valid event to Kafka and return it in processedIds")
    void sendEvents_validEvent_successfullySentToKafka() throws InterruptedException {
        UUID eventId = UUID.randomUUID();
        String payloadJson = "{\"testField\":\"Hello Kafka\"}";
        String payloadType = DummyPayload.class.getName();

        OutboxEvent event = createEvent(eventId, "TEST_EVENT", payloadType, payloadJson);

        SenderResult result = sender.sendEvents(TOPIC, List.of(event));

        assertThat(result.processedIds()).containsExactly(eventId);
        assertThat(result.failedIds()).isEmpty();

        ConsumerRecord<String, Object> received = receiver.pollForEventId(eventId, 10, TimeUnit.SECONDS);

        assertThat(received).isNotNull();

        byte[] eventIdHeader = received.headers().lastHeader(OutboxHeaders.EVENT_ID.getValue()).value();
        assertThat(new String(eventIdHeader)).isEqualTo(eventId.toString());

        byte[] eventTypeHeader = received.headers().lastHeader(OutboxHeaders.EVENT_TYPE.getValue()).value();
        assertThat(new String(eventTypeHeader)).isEqualTo("TEST_EVENT");

        assertThat(received.value()).isInstanceOf(String.class);
        assertThat(received.value()).isEqualTo(payloadJson);
    }

    @Test
    @DisplayName("CT sendEvents() concurrent batches from multiple threads should all be processed")
    void sendEvents_concurrentBatches_allProcessed() throws Exception {
        int threads = 10;
        int eventsPerThread = 100;
        ExecutorService pool = Executors.newFixedThreadPool(threads);

        List<Callable<SenderResult>> tasks = IntStream.range(0, threads)
                .mapToObj(i -> (Callable<SenderResult>) () -> {
                    List<OutboxEvent> events = IntStream.range(0, eventsPerThread)
                            .mapToObj(j -> createEvent(
                                    UUID.randomUUID(),
                                    "TEST_EVENT",
                                    DummyPayload.class.getName(),
                                    "{\"testField\":\"t" + i + "j" + j + "\"}"
                            ))
                            .toList();
                    return sender.sendEvents(TOPIC, events);
                })
                .toList();

        List<Future<SenderResult>> futures = pool.invokeAll(tasks);
        pool.shutdown();

        int totalProcessed = 0;
        int totalFailed = 0;
        for (Future<SenderResult> f : futures) {
            SenderResult r = f.get();
            totalProcessed += r.processedIds().size();
            totalFailed += r.failedIds().size();
        }

        assertThat(totalProcessed).isEqualTo(threads * eventsPerThread);
        assertThat(totalFailed).isZero();
    }

    @Test
    @DisplayName("CT sendEvents() same sender invoked concurrently should return consistent results")
    void sendEvents_sameSenderConcurrentInvocations_consistentResults() throws Exception {
        int threads = 10;
        ExecutorService pool = Executors.newFixedThreadPool(threads);

        List<UUID> allIds = new ArrayList<>();
        List<Callable<SenderResult>> tasks = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            UUID id = UUID.randomUUID();
            allIds.add(id);
            OutboxEvent event = createEvent(id, "TEST_EVENT", DummyPayload.class.getName(),
                    "{\"testField\":\"msg" + i + "\"}");
            tasks.add(() -> sender.sendEvents(TOPIC, List.of(event)));
        }

        List<Future<SenderResult>> futures = pool.invokeAll(tasks);
        pool.shutdown();

        Set<UUID> allProcessed = futures.stream()
                .map(f -> {
                    try { return f.get(); } catch (Exception e) { throw new RuntimeException(e); }
                })
                .flatMap(r -> r.processedIds().stream())
                .collect(Collectors.toSet());

        assertThat(allProcessed).containsExactlyInAnyOrderElementsOf(allIds);
    }

    private OutboxEvent createEvent(UUID id, String eventType, String payloadType, String payload) {
        Instant now = Instant.now();
        return new OutboxEvent(id, EventStatus.PENDING, eventType, payloadType, payload, 0, now, now, now);
    }

    @TestConfiguration
    @EnableKafka
    static class KafkaTestConfig {

        @Bean
        public ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @Bean
        public KafkaMessageReceiver kafkaMessageReceiver() {
            return new KafkaMessageReceiver();
        }
    }

    static class KafkaMessageReceiver {
        private final BlockingQueue<ConsumerRecord<String, Object>> records = new LinkedBlockingQueue<>();

        @KafkaListener(topics = TOPIC)
        public void listen(ConsumerRecord<String, Object> record) {
            records.add(record);
        }

        public ConsumerRecord<String, Object> pollForEventId(UUID targetEventId, long timeout, TimeUnit unit) throws InterruptedException {
            long endTime = System.currentTimeMillis() + unit.toMillis(timeout);

            while (System.currentTimeMillis() < endTime) {
                ConsumerRecord<String, Object> record = records.poll(1, TimeUnit.SECONDS);
                if (record != null) {
                    var header = record.headers().lastHeader(OutboxHeaders.EVENT_ID.getValue());
                    if (header != null && targetEventId.toString().equals(new String(header.value()))) {
                        return record;
                    }
                }
            }
            return null;
        }

        public void clear() {
            records.clear();
        }
    }

    public static class DummyPayload {
        public DummyPayload() {}
    }
}