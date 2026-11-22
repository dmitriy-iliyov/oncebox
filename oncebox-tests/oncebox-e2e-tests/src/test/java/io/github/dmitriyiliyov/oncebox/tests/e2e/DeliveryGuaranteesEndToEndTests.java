package io.github.dmitriyiliyov.oncebox.tests.e2e;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.dmitriyiliyov.oncebox.core.outbox.EventStatus;
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

// The guarantees as a consumer sees them: at-least-once through a publisher crash, deduplication bounded by the
// inbox ttl, and what each idempotent consumer does with an operation that fails once.
class DeliveryGuaranteesEndToEndTests extends BaseEndToEndTests {

    private static final int CRASHED_EVENTS = 1000;

    @Autowired
    RawEventResender rawEventResender;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    @DisplayName("E2E publish() when the publisher dies after the broker ack but before marking should resend every event and the consumer skip every duplicate")
    void publish_whenPublisherDiesAfterBrokerAckBeforeMarking_shouldResendAndConsumerSkipEveryDuplicate() {
        // given
        crashingSender.crashAfterEverySend();
        List<BusinessEvent> events = publisherService.saveBatchAndPublish(E2eEvents.DEFAULT_EVENT, CRASHED_EVENTS);

        awaitState().untilAsserted(() -> {
            assertThat(outboxRepository.countEventsByStatus(EventStatus.IN_PROCESS))
                    .as("events left captured by the crashed cycles")
                    .isEqualTo(CRASHED_EVENTS);
            assertThat(outboxRepository.countConsumedBusiness())
                    .as("business rows of the first delivery")
                    .isEqualTo(CRASHED_EVENTS);
        });

        // when - the publisher is back, and the stuck batches are aged past the stuck-recovery threshold
        crashingSender.stopCrashing();
        outboxRepository.shiftEventTimestamps(EventStatus.IN_PROCESS, Duration.ofHours(1));

        // then
        awaitAtMost(Duration.ofSeconds(60)).untilAsserted(() -> {
            assertThat(outboxRepository.countEventsByStatus(EventStatus.PROCESSED)).isEqualTo(CRASHED_EVENTS);
            assertThat(operation.deliveries()).as("deliveries to the consumer").isEqualTo(2 * CRASHED_EVENTS);
        });
        assertThat(crashingSender.acknowledgedSends()).as("sends the broker acknowledged").isEqualTo(2 * CRASHED_EVENTS);
        assertThat(operation.deliveries() - operation.runs()).as("duplicates skipped").isEqualTo(CRASHED_EVENTS);
        assertThat(outboxRepository.countConsumedBusiness()).as("business rows").isEqualTo(CRASHED_EVENTS);
        assertThat(events).allSatisfy(event ->
                assertThat(outboxRepository.countConsumedBusiness(event.verifyId()))
                        .as("business rows of %s", event.verifyId())
                        .isEqualTo(1)
        );
    }

    @Test
    @DisplayName("E2E consume() when an event is redelivered after its inbox record outlived the ttl should process it again")
    void consume_whenRedeliveredAfterInboxRecordOutlivedTtl_shouldProcessItAgain() throws Exception {
        // given
        BusinessEvent event = publisherService.saveAndPublish(E2eEvents.DEFAULT_EVENT);
        UUID eventId = awaitConsumedEventId(event);

        outboxRepository.shiftConsumedEventTimestamp(eventId, Duration.ofHours(1));
        awaitState().untilAsserted(() -> assertThat(outboxRepository.isConsumed(eventId)).isFalse());

        // when
        rawEventResender.resend(
                eventId, E2eEvents.DEFAULT_EVENT, BusinessEvent.class.getName(), objectMapper.writeValueAsString(event)
        );

        // then
        awaitState().untilAsserted(() -> {
            assertThat(outboxRepository.countConsumedBusiness(event.verifyId())).isEqualTo(2);
            assertThat(outboxRepository.isConsumed(eventId)).isTrue();
        });
    }

    @Test
    @DisplayName("E2E consume() on the transactional consumer when the operation fails once should run it again on redelivery and keep one effect")
    void consume_whenTransactionalOperationFailsOnce_shouldRunItAgainOnRedeliveryAndKeepOneEffect() {
        // given
        BusinessEvent event = BusinessEvent.of();
        operation.failFirstRunOf(event.verifyId());

        // when
        publisherService.saveAndPublish(E2eEvents.DEFAULT_EVENT, event);

        // then
        UUID eventId = awaitConsumedEventId(event);
        awaitAtMost(Duration.ofSeconds(10)).during(Duration.ofSeconds(3)).untilAsserted(() -> {
            assertThat(operation.deliveries(event.verifyId())).as("deliveries").isEqualTo(2);
            assertThat(operation.runs(event.verifyId())).as("runs of the operation").isEqualTo(2);
            assertThat(outboxRepository.countConsumedBusiness(event.verifyId()))
                    .as("business rows, the one written by the failed run rolled back")
                    .isEqualTo(1);
        });
        assertThat(outboxRepository.isConsumed(eventId)).isTrue();
    }

    @Test
    @DisplayName("E2E consume() on the non-transactional consumer when the operation fails once should not run it again on redelivery")
    void consume_whenNonTransactionalOperationFailsOnce_shouldNotRunItAgainOnRedelivery() {
        // given
        BusinessEvent event = BusinessEvent.of();
        operation.failFirstRunOf(event.verifyId());

        // when
        publisherService.saveAndPublish(E2eEvents.AT_MOST_ONCE_EVENT, event);

        // then
        awaitState().untilAsserted(() ->
                assertThat(operation.deliveries(event.verifyId())).as("deliveries").isEqualTo(2)
        );
        awaitAtMost(Duration.ofSeconds(10)).during(Duration.ofSeconds(3)).untilAsserted(() -> {
            assertThat(operation.deliveries(event.verifyId())).as("deliveries").isEqualTo(2);
            assertThat(operation.runs(event.verifyId())).as("runs of the operation").isEqualTo(1);
        });
        UUID eventId = outboxRepository.findEventIds().getFirst();
        assertThat(outboxRepository.isConsumed(eventId)).isTrue();
        // The listener runs without a transaction, so the row the failed run wrote before throwing stays
        assertThat(outboxRepository.countConsumedBusiness(event.verifyId())).isEqualTo(1);
    }

    private UUID awaitConsumedEventId(BusinessEvent event) {
        awaitState().untilAsserted(() -> {
            assertThat(outboxRepository.countConsumedBusiness(event.verifyId())).isGreaterThanOrEqualTo(1);
            assertThat(outboxRepository.findEventIds()).hasSize(1);
            assertThat(outboxRepository.isConsumed(outboxRepository.findEventIds().getFirst())).isTrue();
        });
        return outboxRepository.findEventIds().getFirst();
    }
}
