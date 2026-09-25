package io.github.dmitriyiliyov.oncebox.rabbit;

import io.github.dmitriyiliyov.oncebox.core.publisher.domain.EventStatus;
import io.github.dmitriyiliyov.oncebox.core.publisher.domain.OutboxEvent;
import io.github.dmitriyiliyov.oncebox.core.publisher.domain.OutboxHeaders;
import io.github.dmitriyiliyov.oncebox.core.publisher.domain.SenderResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = RabbitOutboxSenderIntegrationTests.RabbitTestConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ActiveProfiles("rabbit-it")
class RabbitOutboxSenderIntegrationTests {

    private static final String EXCHANGE = "integration-test-exchange";
    private static final String QUEUE = "integration-test-queue";
    private static final String ROUTING_KEY = "TEST_EVENT";

    static final RabbitMQContainer rabbit;

    static {
        rabbit = new RabbitMQContainer(DockerImageName.parse("rabbitmq:3.13-management"));
        rabbit.start();
    }

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.rabbitmq.host", rabbit::getHost);
        registry.add("spring.rabbitmq.port", rabbit::getAmqpPort);
        registry.add("spring.rabbitmq.username", rabbit::getAdminUsername);
        registry.add("spring.rabbitmq.password", rabbit::getAdminPassword);
    }

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private RabbitAdmin rabbitAdmin;

    private RabbitOutboxSender sender;

    @BeforeEach
    void setUp() {
        sender = new RabbitOutboxSender(rabbitTemplate, 5_000L);

        DirectExchange exchange = new DirectExchange(EXCHANGE, true, false);
        Queue queue = new Queue(QUEUE, true);
        rabbitAdmin.declareExchange(exchange);
        rabbitAdmin.declareQueue(queue);
        rabbitAdmin.declareBinding(BindingBuilder.bind(queue).to(exchange).with(ROUTING_KEY));
    }

    @AfterEach
    void tearDown() {
        rabbitAdmin.deleteQueue(QUEUE);
        rabbitAdmin.deleteExchange(EXCHANGE);
    }

    @Test
    @DisplayName("IT sendEvents() should successfully send valid event and return it in processedIds")
    void sendEvents_validEvent_successfullySentToRabbit() {
        UUID eventId = UUID.randomUUID();
        OutboxEvent event = createEvent(eventId, ROUTING_KEY, "{\"field\":\"value\"}");

        SenderResult result = sender.sendEvents(EXCHANGE, List.of(event));

        assertThat(result.processedIds()).containsExactly(eventId);
        assertThat(result.failedIds()).isEmpty();

        Message received = rabbitTemplate.receive(QUEUE, 5000);
        assertThat(received).isNotNull();
        assertThat((String) received.getMessageProperties().getHeader(OutboxHeaders.EVENT_ID.getValue()))
                .isEqualTo(eventId.toString());
        assertThat((String) received.getMessageProperties().getHeader(OutboxHeaders.EVENT_TYPE.getValue()))
                .isEqualTo(ROUTING_KEY);
        assertThat(new String(received.getBody(), StandardCharsets.UTF_8))
                .isEqualTo("{\"field\":\"value\"}");
    }

    @Test
    @DisplayName("IT sendEvents() with empty list should return empty result")
    void sendEvents_emptyList_returnsEmpty() {
        SenderResult result = sender.sendEvents(EXCHANGE, List.of());

        assertThat(result.processedIds()).isEmpty();
        assertThat(result.failedIds()).isEmpty();
    }

    @Test
    @DisplayName("IT sendEvents() to non-existent exchange should treat all events as failed")
    void sendEvents_nonExistentExchange_allFailed() {
        UUID eventId = UUID.randomUUID();
        OutboxEvent event = createEvent(eventId, ROUTING_KEY, "{\"field\":\"value\"}");

        SenderResult result = sender.sendEvents("non-existent-exchange", List.of(event));

        assertThat(result.failedIds()).containsExactly(eventId);
        assertThat(result.processedIds()).isEmpty();
    }

    @Test
    @DisplayName("IT sendEvents() should successfully send a large batch and all arrive in processedIds")
    void sendEvents_largeBatch_allProcessed() {
        List<OutboxEvent> events = IntStream.range(0, 50)
                .mapToObj(i -> createEvent(UUID.randomUUID(), ROUTING_KEY, "{\"index\":" + i + "}"))
                .toList();

        SenderResult result = sender.sendEvents(EXCHANGE, events);

        Set<UUID> expectedIds = events.stream().map(OutboxEvent::getId).collect(Collectors.toSet());
        assertThat(result.processedIds()).containsExactlyInAnyOrderElementsOf(expectedIds);
        assertThat(result.failedIds()).isEmpty();
    }

    @Test
    @DisplayName("IT sendEvents() should preserve headers for each event in a batch")
    void sendEvents_batch_headersCorrectForEachMessage() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        List<OutboxEvent> events = List.of(
                createEvent(id1, ROUTING_KEY, "{\"n\":1}"),
                createEvent(id2, ROUTING_KEY, "{\"n\":2}")
        );

        SenderResult result = sender.sendEvents(EXCHANGE, events);

        assertThat(result.processedIds()).containsExactlyInAnyOrder(id1, id2);

        List<String> receivedIds = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            Message msg = rabbitTemplate.receive(QUEUE, 5000);
            assertThat(msg).isNotNull();
            receivedIds.add((String) msg.getMessageProperties().getHeader(OutboxHeaders.EVENT_ID.getValue()));
        }
        assertThat(receivedIds).containsExactlyInAnyOrder(id1.toString(), id2.toString());
    }

    @Test
    @DisplayName("CT sendEvents() concurrent batches from multiple threads should all be processed")
    void sendEvents_concurrentBatches_allProcessed() throws Exception {
        int threads = 5;
        int eventsPerThread = 10;
        ExecutorService pool = Executors.newFixedThreadPool(threads);

        List<Callable<SenderResult>> tasks = IntStream.range(0, threads)
                .mapToObj(i -> (Callable<SenderResult>) () -> {
                    List<OutboxEvent> events = IntStream.range(0, eventsPerThread)
                            .mapToObj(j -> createEvent(UUID.randomUUID(), ROUTING_KEY, "{\"t\":" + i + ",\"j\":" + j + "}"))
                            .toList();
                    return sender.sendEvents(EXCHANGE, events);
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
    @DisplayName("CT sendEvents() concurrent sends to valid exchange should all succeed")
    void sendEvents_concurrentValidSends_allSucceed() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);

        UUID validId1 = UUID.randomUUID();
        UUID validId2 = UUID.randomUUID();

        Future<SenderResult> future1 = pool.submit(() ->
                sender.sendEvents(EXCHANGE, List.of(createEvent(validId1, ROUTING_KEY, "{\"v\":1}")))
        );
        Future<SenderResult> future2 = pool.submit(() ->
                sender.sendEvents(EXCHANGE, List.of(createEvent(validId2, ROUTING_KEY, "{\"v\":2}")))
        );
        pool.shutdown();
        pool.awaitTermination(30, TimeUnit.SECONDS);

        assertThat(future1.get().processedIds()).containsExactly(validId1);
        assertThat(future1.get().failedIds()).isEmpty();
        assertThat(future2.get().processedIds()).containsExactly(validId2);
        assertThat(future2.get().failedIds()).isEmpty();
    }

    @Test
    @DisplayName("CT sendEvents() same sender invoked concurrently multiple times should return consistent results")
    void sendEvents_sameSenderConcurrentInvocations_consistentResults() throws Exception {
        int threads = 8;
        ExecutorService pool = Executors.newFixedThreadPool(threads);

        List<UUID> allIds = new ArrayList<>();
        List<Callable<SenderResult>> tasks = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            UUID id = UUID.randomUUID();
            allIds.add(id);
            OutboxEvent event = createEvent(id, ROUTING_KEY, "{\"i\":" + i + "}");
            tasks.add(() -> sender.sendEvents(EXCHANGE, List.of(event)));
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

    private OutboxEvent createEvent(UUID id, String eventType, String payload) {
        Instant now = Instant.now();
        return new OutboxEvent(id, EventStatus.PENDING, eventType,
                "io.example.Payload", payload, 0, now, now, now);
    }

    @TestConfiguration
    static class RabbitTestConfig {

        @Bean
        public RabbitAdmin rabbitAdmin(CachingConnectionFactory connectionFactory) {
            return new RabbitAdmin(connectionFactory);
        }
    }
}