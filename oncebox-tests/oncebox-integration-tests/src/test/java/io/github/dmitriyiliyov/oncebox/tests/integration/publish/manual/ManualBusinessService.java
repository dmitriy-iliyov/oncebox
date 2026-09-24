package io.github.dmitriyiliyov.oncebox.tests.integration.publish.manual;

import io.github.dmitriyiliyov.oncebox.core.publisher.OutboxPublisher;
import io.github.dmitriyiliyov.oncebox.tests.integration.domain.BusinessEntity;
import io.github.dmitriyiliyov.oncebox.tests.integration.domain.BusinessEvent;
import io.github.dmitriyiliyov.oncebox.tests.integration.domain.BusinessFailureException;
import io.github.dmitriyiliyov.oncebox.tests.integration.publish.BusinessRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public class ManualBusinessService {

    public static final String EVENT_TYPE = "business-event";
    private final OutboxPublisher publisher;
    private final BusinessRepository repository;

    public ManualBusinessService(OutboxPublisher publisher, BusinessRepository repository) {
        this.publisher = publisher;
        this.repository = repository;
    }

    @Transactional
    public void successSaveEvent(BusinessEvent event) {
        repository.save(new BusinessEntity(event.verifyId()));
        publisher.publish(EVENT_TYPE, event);
    }

    @Transactional
    public void successSaveEvents(List<BusinessEvent> events) {
        repository.saveAll(
                events.stream().map(event -> new BusinessEntity(event.verifyId())).toList()
        );
        publisher.publish(EVENT_TYPE, events);
    }

    public void exceptionallyWithoutTransaction(BusinessEvent event) {
        repository.save(new BusinessEntity(event.verifyId()));
        publisher.publish(EVENT_TYPE, event);
    }

    public void exceptionallyWithoutTransaction(List<BusinessEvent> events) {
        repository.saveAll(
                events.stream().map(event -> new BusinessEntity(event.verifyId())).toList()
        );
        publisher.publish(EVENT_TYPE, events);
    }

    @Transactional
    public void exceptionallyInBusinessTransaction(BusinessEvent event) {
        repository.save(new BusinessEntity(event.verifyId()));
        publisher.publish(EVENT_TYPE, event);
        throw new BusinessFailureException();
    }

    @Transactional
    public void exceptionallyInBusinessTransaction(List<BusinessEvent> events) {
        repository.saveAll(
                events.stream().map(event -> new BusinessEntity(event.verifyId())).toList()
        );
        publisher.publish(EVENT_TYPE, events);
        throw new BusinessFailureException();
    }
}
