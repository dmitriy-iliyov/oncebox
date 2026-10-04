package io.github.dmitriyiliyov.oncebox.tests.e2e.publish;

import io.github.dmitriyiliyov.oncebox.aop.OutboxPublish;
import io.github.dmitriyiliyov.oncebox.core.outbox.OutboxPublisher;
import io.github.dmitriyiliyov.oncebox.tests.e2e.domain.BusinessEvent;
import io.github.dmitriyiliyov.oncebox.tests.e2e.domain.E2eEvents;
import io.github.dmitriyiliyov.oncebox.tests.e2e.repository.TestOutboxRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Stream;

public class PublisherBusinessService {

    private final OutboxPublisher publisher;
    private final TestOutboxRepository repository;

    public PublisherBusinessService(OutboxPublisher publisher, TestOutboxRepository repository) {
        this.publisher = publisher;
        this.repository = repository;
    }

    @Transactional
    public BusinessEvent saveAndPublish(String eventType) {
        BusinessEvent event = BusinessEvent.of();
        repository.saveProducedBusiness(event.verifyId());
        publisher.publish(eventType, event);
        return event;
    }

    @Transactional
    public List<BusinessEvent> saveBatchAndPublish(String eventType, int count) {
        List<BusinessEvent> events = Stream.generate(BusinessEvent::of).limit(count).toList();
        events.forEach(event -> repository.saveProducedBusiness(event.verifyId()));
        publisher.publish(eventType, events);
        return events;
    }

    @Transactional
    @OutboxPublish(eventType = E2eEvents.AOP_EVENT)
    public BusinessEvent saveAndPublishWithAop() {
        BusinessEvent event = BusinessEvent.of();
        repository.saveProducedBusiness(event.verifyId());
        return event;
    }

    @Transactional
    public BusinessEvent saveAndFail(String eventType) {
        BusinessEvent event = BusinessEvent.of();
        repository.saveProducedBusiness(event.verifyId());
        publisher.publish(eventType, event);
        throw new IllegalStateException("Intentional rollback to verify atomic persistence");
    }
}
