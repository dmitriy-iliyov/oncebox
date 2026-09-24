package io.github.dmitriyiliyov.oncebox.tests.e2e;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.dmitriyiliyov.oncebox.core.publisher.domain.EventStatus;
import io.github.dmitriyiliyov.oncebox.tests.e2e.config.BrokerFaultControl;
import io.github.dmitriyiliyov.oncebox.tests.e2e.domain.BusinessEvent;
import io.github.dmitriyiliyov.oncebox.tests.e2e.domain.E2eEvents;
import io.github.dmitriyiliyov.oncebox.tests.e2e.publish.RawEventResender;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultEventLifeCycleTests extends BaseE2eTests {

    @Autowired
    RawEventResender rawEventResender;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    @DisplayName("CT publish() when a single event should be delivered and consumed once")
    void publish_whenSingleEvent_shouldBeDeliveredAndConsumedOnce() {
        BusinessEvent event = publisherService.saveAndPublish(E2eEvents.DEFAULT_EVENT);

        awaitState().untilAsserted(() -> {
            assertThat(outboxRepository.countConsumedBusiness(event.verifyId())).isEqualTo(1);
            assertThat(outboxRepository.countEventsByStatus(EventStatus.PROCESSED)).isEqualTo(1);
        });

        UUID eventId = outboxRepository.findEventIds().getFirst();
        assertThat(outboxRepository.isConsumed(eventId)).isTrue();
    }

    @Test
    @DisplayName("CT publish() when a batch of events should deliver each of them once")
    void publish_whenBatchOfEvents_shouldDeliverEachOnce() {
        List<BusinessEvent> events = publisherService.saveBatchAndPublish(E2eEvents.DEFAULT_EVENT, 50);

        awaitState().untilAsserted(() ->
                assertThat(outboxRepository.countEventsByStatus(EventStatus.PROCESSED)).isEqualTo(50)
        );
        awaitState().untilAsserted(() ->
                events.forEach(event ->
                        assertThat(outboxRepository.countConsumedBusiness(event.verifyId())).isEqualTo(1)
                )
        );
    }

    @Test
    @DisplayName("CT @OutboxPublish when the method completes should deliver its event")
    void outboxPublish_whenMethodCompletes_shouldDeliverItsEvent() {
        BusinessEvent event = publisherService.saveAndPublishWithAop();

        awaitState().untilAsserted(() -> {
            assertThat(outboxRepository.countConsumedBusiness(event.verifyId())).isEqualTo(1);
            assertThat(outboxRepository.countEventsByStatus(EventStatus.PROCESSED)).isEqualTo(1);
        });
    }

    @Test
    @DisplayName("CT publish() when the business transaction rolls back should publish nothing")
    void publish_whenBusinessTransactionRollsBack_shouldPublishNothing() {
        assertThatThrownBy(() -> publisherService.saveAndFail(E2eEvents.DEFAULT_EVENT))
                .isInstanceOf(IllegalStateException.class);

        awaitAtMost(Duration.ofSeconds(5)).during(Duration.ofSeconds(3)).untilAsserted(() -> {
            assertThat(outboxRepository.countEvents()).isZero();
            assertThat(outboxRepository.countConsumedBusiness()).isZero();
        });
    }

    @Test
    @DisplayName("CT consume() when the same event is delivered twice should process it once")
    void consume_whenSameEventDeliveredTwice_shouldProcessItOnce() throws Exception {
        BusinessEvent event = publisherService.saveAndPublish(E2eEvents.DEFAULT_EVENT);

        awaitState().untilAsserted(() ->
                assertThat(outboxRepository.countConsumedBusiness(event.verifyId())).isEqualTo(1)
        );

        UUID eventId = outboxRepository.findEventIds().getFirst();
        rawEventResender.resend(
                eventId, E2eEvents.DEFAULT_EVENT, BusinessEvent.class.getName(), objectMapper.writeValueAsString(event)
        );

        awaitAtMost(Duration.ofSeconds(10)).during(Duration.ofSeconds(5)).untilAsserted(() ->
                assertThat(outboxRepository.countConsumedBusiness(event.verifyId())).isEqualTo(1)
        );
    }

    @Test
    @DisplayName("CT event when stuck IN_PROCESS should be recovered without counting a retry")
    void event_whenStuckInProcess_shouldBeRecoveredWithoutCountingARetry() throws Exception {
        BusinessEvent event = BusinessEvent.of();
        UUID eventId = outboxRepository.insertEvent(
                E2eEvents.DEFAULT_EVENT,
                EventStatus.IN_PROCESS,
                BusinessEvent.class.getName(),
                objectMapper.writeValueAsString(event),
                Duration.ofHours(1)
        );

        awaitState().untilAsserted(() -> {
            assertThat(outboxRepository.countConsumedBusiness(event.verifyId())).isEqualTo(1);
            assertThat(outboxRepository.countEventsByStatus(EventStatus.PROCESSED)).isEqualTo(1);
        });
        assertThat(outboxRepository.findRetryCount(eventId)).isZero();
    }

    @Test
    @DisplayName("CT event when the broker is down should be retried and delivered after it recovers")
    void event_whenBrokerIsDown_shouldBeRetriedAndDeliveredAfterRecovery() {
        BrokerFaultControl.stopBroker();
        try {
            BusinessEvent event = publisherService.saveAndPublish(E2eEvents.RETRY_EVENT);

            awaitAtMost(Duration.ofSeconds(60)).untilAsserted(() -> {
                UUID eventId = outboxRepository.findEventIds().getFirst();
                assertThat(outboxRepository.findRetryCount(eventId)).isGreaterThanOrEqualTo(1);
            });
            assertThat(outboxRepository.countConsumedBusiness(event.verifyId())).isZero();

            BrokerFaultControl.startBroker();

            awaitAtMost(Duration.ofSeconds(60)).untilAsserted(() -> {
                assertThat(outboxRepository.countConsumedBusiness(event.verifyId())).isEqualTo(1);
                assertThat(outboxRepository.countEventsByStatus(EventStatus.PROCESSED)).isEqualTo(1);
            });
        } finally {
            BrokerFaultControl.startBroker();
        }
    }

    @Test
    @DisplayName("CT processed event when past its ttl should be cleaned up")
    void processedEvent_whenPastTtl_shouldBeCleanedUp() {
        BusinessEvent event = publisherService.saveAndPublish(E2eEvents.DEFAULT_EVENT);

        awaitState().untilAsserted(() ->
                assertThat(outboxRepository.countEventsByStatus(EventStatus.PROCESSED)).isEqualTo(1)
        );

        UUID eventId = outboxRepository.findEventIds().getFirst();
        outboxRepository.shiftEventTimestamps(eventId, Duration.ofHours(1));

        awaitState().untilAsserted(() ->
                assertThat(outboxRepository.countEvents()).isZero()
        );
        assertThat(outboxRepository.countConsumedBusiness(event.verifyId())).isEqualTo(1);
    }

    @Test
    @DisplayName("CT consumed event id when past its ttl should be cleaned up")
    void consumedEventId_whenPastTtl_shouldBeCleanedUp() {
        publisherService.saveAndPublish(E2eEvents.DEFAULT_EVENT);

        awaitState().untilAsserted(() ->
                assertThat(outboxRepository.countEventsByStatus(EventStatus.PROCESSED)).isEqualTo(1)
        );
        UUID eventId = outboxRepository.findEventIds().getFirst();
        awaitState().untilAsserted(() ->
                assertThat(outboxRepository.isConsumed(eventId)).isTrue()
        );

        outboxRepository.shiftConsumedEventTimestamp(eventId, Duration.ofHours(1));

        awaitState().untilAsserted(() ->
                assertThat(outboxRepository.isConsumed(eventId)).isFalse()
        );
    }
}
