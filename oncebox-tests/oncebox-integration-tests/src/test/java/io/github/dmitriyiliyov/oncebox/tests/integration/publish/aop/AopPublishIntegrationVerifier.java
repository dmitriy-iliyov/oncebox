package io.github.dmitriyiliyov.oncebox.tests.integration.publish.aop;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.dmitriyiliyov.oncebox.tests.integration.domain.BusinessEvent;
import io.github.dmitriyiliyov.oncebox.tests.integration.domain.BusinessFailureException;
import io.github.dmitriyiliyov.oncebox.tests.integration.utils.IdExtractor;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class AopPublishIntegrationVerifier {

    private final AopBusinessService service;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final IdExtractor idExtractor;

    public AopPublishIntegrationVerifier(AopBusinessService service, JdbcTemplate jdbcTemplate, IdExtractor idExtractor) {
        this.service = service;
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = new ObjectMapper();
        this.idExtractor = idExtractor;
    }

    public void publish_shouldSaveEvent() {
        BusinessEvent event = BusinessEvent.of();

        service.successSaveEvent(event);

        List<UUID> outboxIds = selectOutboxIdsQuery();
        assertThat(outboxIds).containsOnly(event.verifyId());

        List<UUID> businessIds = selectBusinessIdsQuery();
        assertThat(businessIds).containsOnly(event.verifyId());

        assertEquals(outboxIds.getFirst(), businessIds.getFirst());
    }

    public void publish_shouldSaveEvents(int eventCount) {
        List<BusinessEvent> events = new ArrayList<>();
        for (int i = 0; i < eventCount; i++) {
            events.add(BusinessEvent.of());
        }

        service.successSaveEvents(events);

        List<UUID> ids = events.stream().map(BusinessEvent::verifyId).toList();
        List<UUID> outboxIds = selectOutboxIdsQuery();
        assertThat(outboxIds).containsExactlyInAnyOrder(ids.toArray(new UUID[0]));

        List<UUID> businessIds = selectBusinessIdsQuery();
        assertThat(businessIds).containsExactlyInAnyOrder(ids.toArray(new UUID[0]));
    }

    public void publish_successSaveReturnedEvent() {
        BusinessEvent event = BusinessEvent.of();

        service.successSaveReturnedEvent(event);

        assertThat(selectOutboxIdsQuery()).containsOnly(AopBusinessService.resultOf(event).verifyId());
        assertThat(selectBusinessIdsQuery()).containsOnly(event.verifyId());
    }

    public void publish_successSaveReturnedEvents(int eventCount) {
        List<BusinessEvent> events = new ArrayList<>();
        for (int i = 0; i < eventCount; i++) {
            events.add(BusinessEvent.of());
        }

        service.successSaveReturnedEvents(events);

        assertThat(selectOutboxIdsQuery()).containsExactlyInAnyOrderElementsOf(
                events.stream().map(event -> AopBusinessService.resultOf(event).verifyId()).toList()
        );
        assertThat(selectBusinessIdsQuery()).containsExactlyInAnyOrderElementsOf(
                events.stream().map(BusinessEvent::verifyId).toList()
        );
    }

    public void publishEvent_shouldThrows_whenBusinessTransactionFailed() {
        assertThrows(
                BusinessFailureException.class,
                () -> service.exceptionallyInBusinessTransaction(BusinessEvent.of())
        );

        assertThat(selectOutboxIdsQuery()).isEmpty();
        assertThat(selectBusinessIdsQuery()).isEmpty();
    }

    public void publishEvents_shouldThrows_whenBusinessTransactionFailed() {
        assertThrows(
                BusinessFailureException.class,
                () -> service.exceptionallyInBusinessTransaction(List.of(BusinessEvent.of(), BusinessEvent.of()))
        );

        assertThat(selectOutboxIdsQuery()).isEmpty();
        assertThat(selectBusinessIdsQuery()).isEmpty();
    }

    public void publishEventWithReturnedEvent_shouldThrows_whenBusinessTransactionFailed() {
        assertThrows(
                BusinessFailureException.class,
                () -> service.exceptionallyInBusinessTransactionWithReturnedEvent(BusinessEvent.of())
        );

        assertThat(selectOutboxIdsQuery()).isEmpty();
        assertThat(selectBusinessIdsQuery()).isEmpty();
    }

    public void publishEventsWithReturnedEvents_shouldThrows_whenBusinessTransactionFailed() {
        assertThrows(
                BusinessFailureException.class,
                () -> service.exceptionallyInBusinessTransactionWithReturnedEvents(List.of(BusinessEvent.of(), BusinessEvent.of()))
        );

        assertThat(selectOutboxIdsQuery()).isEmpty();
        assertThat(selectBusinessIdsQuery()).isEmpty();
    }

    private List<UUID> selectOutboxIdsQuery() {
        return jdbcTemplate.query(
                "SELECT payload FROM outbox_events",
                (rs, n) -> {
                    JsonNode node = null;
                    try {
                        node = objectMapper.readTree(rs.getString("payload"));
                        return UUID.fromString(node.get("verifyId").asText());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
        );
    }

    private List<UUID> selectBusinessIdsQuery() {
        return jdbcTemplate.query(
                "SELECT * FROM business_events",
                (rs, n) -> idExtractor.extract("verify_id", rs)
        );
    }

    public void cleanUpQueries() {
        jdbcTemplate.execute("DELETE FROM outbox_events");
        jdbcTemplate.execute("DELETE FROM business_events");
    }
}
