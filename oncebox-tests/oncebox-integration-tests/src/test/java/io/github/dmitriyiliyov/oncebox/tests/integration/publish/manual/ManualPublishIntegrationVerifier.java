package io.github.dmitriyiliyov.oncebox.tests.integration.publish.manual;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.dmitriyiliyov.oncebox.tests.integration.domain.BusinessEvent;
import io.github.dmitriyiliyov.oncebox.tests.integration.domain.BusinessFailureException;
import io.github.dmitriyiliyov.oncebox.tests.integration.utils.IdExtractor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.IllegalTransactionStateException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ManualPublishIntegrationVerifier {

    private final ManualBusinessService service;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final IdExtractor idExtractor;

    public ManualPublishIntegrationVerifier(ManualBusinessService service, JdbcTemplate jdbcTemplate, IdExtractor idExtractor) {
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

    public void publishEvent_shouldThrows_whenNoTransaction() {
        assertThrows(
                IllegalTransactionStateException.class,
                () -> service.exceptionallyWithoutTransaction(BusinessEvent.of())
        );
    }

    public void publishEvents_shouldThrows_whenNoTransaction() {
        assertThrows(
                IllegalTransactionStateException.class,
                () -> service.exceptionallyWithoutTransaction(List.of(BusinessEvent.of(), BusinessEvent.of()))
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
        jdbcTemplate.execute("DELETE FROM outbox_events WHERE 1=1");
        jdbcTemplate.execute("DELETE FROM business_events WHERE 1=1");
    }
}
