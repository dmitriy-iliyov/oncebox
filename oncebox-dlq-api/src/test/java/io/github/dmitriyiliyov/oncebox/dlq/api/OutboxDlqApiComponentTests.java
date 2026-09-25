package io.github.dmitriyiliyov.oncebox.dlq.api;

import io.github.dmitriyiliyov.oncebox.core.publisher.dlq.DlqStatus;
import io.github.dmitriyiliyov.oncebox.core.publisher.dlq.OutboxDlqEvent;
import io.github.dmitriyiliyov.oncebox.core.publisher.domain.EventStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.*;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * The DLQ API as a whole - controller, advice and service are the real ones; only the repository, whose
 * implementations live in the dialect modules, is kept in memory.
 */
@WebMvcTest(controllers = OutboxDlqController.class)
@Import(OutboxDlqApiComponentTests.RealService.class)
class OutboxDlqApiComponentTests {

    private static final String BASE = "/api/outbox-dlq/events";
    private static final UUID MOVED_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID IN_PROCESS_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID OTHER_TYPE_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UUID MISSING_ID = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InMemoryOutboxDlqApiRepository repository;

    @BeforeEach
    void setUp() {
        repository.reset(
                event(MOVED_ID, "order-created", DlqStatus.MOVED),
                event(IN_PROCESS_ID, "order-created", DlqStatus.IN_PROCESS),
                event(OTHER_TYPE_ID, "payment-failed", DlqStatus.MOVED)
        );
    }

    @Test
    @DisplayName("CT GET /{id} when the event exists should return it")
    void get_whenEventExists_shouldReturnIt() throws Exception {
        // when / then
        mockMvc.perform(get(BASE + "/{id}", MOVED_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(MOVED_ID.toString()))
                .andExpect(jsonPath("$.dlqStatus").value("MOVED"));
    }

    @Test
    @DisplayName("CT DELETE /{id} when the event does not exist should answer 404 and delete nothing")
    void delete_whenEventMissing_shouldAnswer404() throws Exception {
        // when / then
        mockMvc.perform(delete(BASE + "/{id}", MISSING_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value(ProblemTypes.DLQ_EVENT_NOT_FOUND.toString()));
        assertThat(repository.ids()).hasSize(3);
    }

    @Test
    @DisplayName("CT PATCH /{id} should store the new status")
    void updateStatus_shouldStoreNewStatus() throws Exception {
        // when
        mockMvc.perform(patch(BASE + "/{id}", MOVED_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"TO_RETRY\"}"))
                .andExpect(status().isNoContent());

        // then
        assertThat(repository.status(MOVED_ID)).isEqualTo(DlqStatus.TO_RETRY);
    }

    @Test
    @DisplayName("CT PATCH /{id} when the event is IN_PROCESS should answer 409 and leave it as it is")
    void updateStatus_whenEventInProcess_shouldAnswer409AndLeaveIt() throws Exception {
        // when
        mockMvc.perform(patch(BASE + "/{id}", IN_PROCESS_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"RESOLVED\"}"))
                .andExpect(status().isConflict());

        // then
        assertThat(repository.status(IN_PROCESS_ID)).isEqualTo(DlqStatus.IN_PROCESS);
    }

    @Test
    @DisplayName("CT DELETE /batch by ids when one is IN_PROCESS should delete the rest and report a partial success")
    void deleteBatch_whenOneIdInProcess_shouldDeleteRestAndReportPartialSuccess() throws Exception {
        // when
        mockMvc.perform(delete(BASE + "/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ids\": [\"%s\", \"%s\"]}".formatted(MOVED_ID, IN_PROCESS_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestedCount").value(2))
                .andExpect(jsonPath("$.processedCount").value(1))
                .andExpect(jsonPath("$.status").value("PARTIAL_SUCCESS"));

        // then
        assertThat(repository.ids()).containsExactlyInAnyOrder(IN_PROCESS_ID, OTHER_TYPE_ID);
    }

    @Test
    @DisplayName("CT PATCH /batch by event type should update only that type and skip IN_PROCESS events")
    void updateBatchStatus_byEventType_shouldUpdateOnlyThatTypeSkippingInProcess() throws Exception {
        // when
        mockMvc.perform(patch(BASE + "/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"eventType\": \"order-created\", \"status\": \"RESOLVED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processedCount").value(1))
                .andExpect(jsonPath("$.status").value("POSSIBLE_PARTIAL_SUCCESS"));

        // then
        assertThat(repository.status(MOVED_ID)).isEqualTo(DlqStatus.RESOLVED);
        assertThat(repository.status(IN_PROCESS_ID)).isEqualTo(DlqStatus.IN_PROCESS);
        assertThat(repository.status(OTHER_TYPE_ID)).isEqualTo(DlqStatus.MOVED);
    }

    @Test
    @DisplayName("CT GET /count by status should count only events in that status")
    void getCount_byStatus_shouldCountOnlyThatStatus() throws Exception {
        // when / then
        mockMvc.perform(get(BASE + "/count").param("status", "MOVED"))
                .andExpect(status().isOk())
                .andExpect(content().string("2"));
    }

    @Test
    @DisplayName("CT GET /batch by event type should page through only that type")
    void getBatch_byEventType_shouldPageThroughOnlyThatType() throws Exception {
        // when / then
        mockMvc.perform(get(BASE + "/batch")
                        .param("eventType", "payment-failed")
                        .param("batchNumber", "0")
                        .param("batchSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(OTHER_TYPE_ID.toString()));
    }

    private static OutboxDlqEvent event(UUID id, String eventType, DlqStatus dlqStatus) {
        Instant at = Instant.parse("2026-09-24T10:00:00Z");
        return new OutboxDlqEvent(id, EventStatus.FAILED, eventType, "io.example.Payload", "{}", 3, at, at, at,
                dlqStatus, at);
    }

    @TestConfiguration
    static class RealService {

        @Bean
        InMemoryOutboxDlqApiRepository inMemoryOutboxDlqApiRepository() {
            return new InMemoryOutboxDlqApiRepository();
        }

        @Bean
        OutboxDlqApiService outboxDlqApiService(InMemoryOutboxDlqApiRepository repository) {
            return new DefaultOutboxDlqApiService(repository);
        }
    }

    static class InMemoryOutboxDlqApiRepository implements OutboxDlqApiRepository {

        private final Map<UUID, OutboxDlqEvent> rows = new LinkedHashMap<>();

        void reset(OutboxDlqEvent... events) {
            rows.clear();
            for (OutboxDlqEvent event : events) {
                rows.put(event.getId(), event);
            }
        }

        List<UUID> ids() {
            return List.copyOf(rows.keySet());
        }

        DlqStatus status(UUID id) {
            return rows.get(id).getDlqStatus();
        }

        @Override
        public Optional<OutboxDlqEvent> findById(UUID id) {
            return Optional.ofNullable(rows.get(id));
        }

        @Override
        public Optional<OutboxDlqEvent> findByIdForUpdate(UUID id) {
            return findById(id);
        }

        @Override
        public List<OutboxDlqEvent> findBatch(DlqFilter filter, int batchNumber, int batchSize) {
            return rows.values().stream()
                    .filter(matchesQuery(filter))
                    .sorted(Comparator.comparing(OutboxDlqEvent::getId))
                    .skip((long) batchNumber * batchSize)
                    .limit(batchSize)
                    .toList();
        }

        @Override
        public long count(DlqFilter filter) {
            return rows.values().stream().filter(matchesQuery(filter)).count();
        }

        @Override
        public void updateStatus(UUID id, DlqStatus status) {
            rows.put(id, withDlqStatus(rows.get(id), status));
        }

        @Override
        public int updateBatchStatus(DlqFilter filter, DlqStatus forbiddenStatus) {
            List<OutboxDlqEvent> matching = modifiable(filter, forbiddenStatus);
            matching.forEach(event -> rows.put(event.getId(), withDlqStatus(event, filter.getStatus())));
            return matching.size();
        }

        @Override
        public int deleteById(UUID id) {
            return rows.remove(id) == null ? 0 : 1;
        }

        @Override
        public int deleteBatch(DlqFilter filter, DlqStatus forbiddenStatus) {
            List<OutboxDlqEvent> matching = modifiable(filter, forbiddenStatus);
            matching.forEach(event -> rows.remove(event.getId()));
            return matching.size();
        }

        private Predicate<OutboxDlqEvent> matchesQuery(DlqFilter filter) {
            return event -> (!filter.hasStatus() || event.getDlqStatus() == filter.getStatus())
                    && (!filter.hasEventType() || event.getEventType().equals(filter.getEventType()));
        }

        private List<OutboxDlqEvent> modifiable(DlqFilter filter, DlqStatus forbiddenStatus) {
            return rows.values().stream()
                    .filter(event -> event.getDlqStatus() != forbiddenStatus)
                    .filter(event -> filter.hasEventType()
                            ? event.getEventType().equals(filter.getEventType())
                            : filter.getIds().contains(event.getId()))
                    .toList();
        }

        private OutboxDlqEvent withDlqStatus(OutboxDlqEvent event, DlqStatus status) {
            return new OutboxDlqEvent(event.getId(), event.getStatus(), event.getEventType(), event.getPayloadType(),
                    event.getPayload(), event.getRetryCount(), event.getNextRetryAt(), event.getCreatedAt(),
                    event.getUpdatedAt(), status, event.getMovedAt());
        }
    }
}
