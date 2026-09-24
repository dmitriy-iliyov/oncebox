package io.github.dmitriyiliyov.oncebox.tests.integration.publish.aop;

import io.github.dmitriyiliyov.oncebox.aop.OutboxPublish;
import io.github.dmitriyiliyov.oncebox.tests.integration.domain.BusinessEntity;
import io.github.dmitriyiliyov.oncebox.tests.integration.domain.BusinessEvent;
import io.github.dmitriyiliyov.oncebox.tests.integration.domain.BusinessFailureException;
import io.github.dmitriyiliyov.oncebox.tests.integration.publish.BusinessRepository;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

public class AopBusinessService {

    public static final String EVENT_TYPE = "business-event";
    private final BusinessRepository repository;

    public AopBusinessService(BusinessRepository repository) {
        this.repository = repository;
    }

    @Transactional
    @OutboxPublish(eventType = EVENT_TYPE, payload = "#event")
    public void successSaveEvent(BusinessEvent event) {
        repository.save(new BusinessEntity(event.verifyId()));
    }

    @Transactional
    @OutboxPublish(eventType = EVENT_TYPE, payload = "#events")
    public void successSaveEvents(List<BusinessEvent> events) {
        repository.saveAll(
                events.stream().map(event -> new BusinessEntity(event.verifyId())).toList()
        );
    }

    @Transactional
    @OutboxPublish(eventType = EVENT_TYPE)
    public BusinessEvent successSaveReturnedEvent(BusinessEvent event) {
        BusinessEntity entity = repository.save(new BusinessEntity(event.verifyId()));
        return resultOf(BusinessEvent.of(entity.getVerifyId()));
    }

    @Transactional
    @OutboxPublish(eventType = EVENT_TYPE)
    public List<BusinessEvent> successSaveReturnedEvents(List<BusinessEvent> events) {
        List<BusinessEntity> entities = repository.saveAll(
                events.stream().map(event -> new BusinessEntity(event.verifyId())).toList()
        );
        return entities.stream().map(entity -> resultOf(BusinessEvent.of(entity.getVerifyId()))).toList();
    }

    /**
     * The event a method returns differs from the one it was given, so a test sees which of the two the aspect
     * published.
     */
    public static BusinessEvent resultOf(BusinessEvent input) {
        return BusinessEvent.of(UUID.nameUUIDFromBytes(("result:" + input.verifyId()).getBytes(StandardCharsets.UTF_8)));
    }

    @Transactional
    @OutboxPublish(eventType = EVENT_TYPE, payload = "#event")
    public void exceptionallyInBusinessTransaction(BusinessEvent event) {
        repository.save(new BusinessEntity(event.verifyId()));
        throw new BusinessFailureException();
    }

    @Transactional
    @OutboxPublish(eventType = EVENT_TYPE, payload = "#events")
    public void exceptionallyInBusinessTransaction(List<BusinessEvent> events) {
        repository.saveAll(events.stream().map(event -> new BusinessEntity(event.verifyId())).toList());
        throw new BusinessFailureException();
    }

    @Transactional
    @OutboxPublish(eventType = EVENT_TYPE)
    public BusinessEvent exceptionallyInBusinessTransactionWithReturnedEvent(BusinessEvent event) {
        repository.save(new BusinessEntity(event.verifyId()));
        throw new BusinessFailureException();
    }

    @Transactional
    @OutboxPublish(eventType = EVENT_TYPE)
    public List<BusinessEvent> exceptionallyInBusinessTransactionWithReturnedEvents(List<BusinessEvent> events) {
        repository.saveAll(events.stream().map(event -> new BusinessEntity(event.verifyId())).toList());
        throw new BusinessFailureException();
    }
}
