package io.github.dmitriyiliyov.oncebox.dlq.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.dmitriyiliyov.oncebox.core.publisher.dlq.DlqStatus;
import io.github.dmitriyiliyov.oncebox.core.publisher.dlq.OutboxDlqEvent;
import io.github.dmitriyiliyov.oncebox.core.publisher.domain.EventStatus;
import io.github.dmitriyiliyov.oncebox.dlq.api.exception.OutboxDlqEventInProcessException;
import io.github.dmitriyiliyov.oncebox.dlq.api.exception.OutboxDlqEventNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = OutboxDlqController.class)
class OutboxDlqControllerIntegrationTests {

    private static final UUID EVENT_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OutboxDlqApiService manager;

    @Test
    @DisplayName("IT GET /{id} should return 200 with event")
    void get_existingId_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        OutboxDlqEvent event = buildEvent(id);
        when(manager.findById(id)).thenReturn(event);

        mockMvc.perform(get("/api/outbox-dlq/events/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists());
    }

    @Test
    @DisplayName("IT GET /{id} when not found should return 404")
    void get_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(manager.findById(id)).thenThrow(new OutboxDlqEventNotFoundException("Event not found"));

        mockMvc.perform(get("/api/outbox-dlq/events/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("DLQ event not found"))
                .andExpect(jsonPath("$.detail").value("Event not found"))
                .andExpect(jsonPath("$.type").value("https://oncebox.io/errors/dlq-event-not-found"));
    }

    @Test
    @DisplayName("IT GET /{id} when unexpected error should return 500")
    void get_unexpectedError_returns500() throws Exception {
        UUID id = UUID.randomUUID();
        when(manager.findById(id)).thenThrow(new RuntimeException("Unexpected"));

        mockMvc.perform(get("/api/outbox-dlq/events/{id}", id))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.title").value("Internal server error"))
                .andExpect(jsonPath("$.type").value("https://oncebox.io/errors/internal-error"));
    }

    @Test
    @DisplayName("IT GET /{id} when database error should return 500")
    void get_databaseError_returns500() throws Exception {
        UUID id = UUID.randomUUID();
        when(manager.findById(id)).thenThrow(new DataAccessException("DB error") {});

        mockMvc.perform(get("/api/outbox-dlq/events/{id}", id))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.type").value("https://oncebox.io/errors/internal-error"))
                .andExpect(jsonPath("$.title").value("Internal server error"));
    }

    @Test
    @DisplayName("IT GET /batch should return 200 with batch")
    void getBatch_validRequest_returns200() throws Exception {
        when(manager.findBatch(any(BatchRequest.class))).thenReturn(List.of(buildEvent(UUID.randomUUID())));

        mockMvc.perform(get("/api/outbox-dlq/events/batch")
                        .param("status", DlqStatus.RESOLVED.name())
                        .param("batchNumber", "1")
                        .param("batchSize", "10"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("IT GET /batch when database error should return 500")
    void getBatch_databaseError_returns500() throws Exception {
        when(manager.findBatch(any(BatchRequest.class)))
                .thenThrow(new DataAccessException("DB error") {});

        mockMvc.perform(get("/api/outbox-dlq/events/batch")
                        .param("status", DlqStatus.RESOLVED.name())
                        .param("batchNumber", "1")
                        .param("batchSize", "10"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.type").value("https://oncebox.io/errors/internal-error"));
    }

    @Test
    @DisplayName("IT GET /count with status only should return 200")
    void getCount_statusOnly_returns200() throws Exception {
        when(manager.count(DlqStatus.RESOLVED, null)).thenReturn(42L);

        mockMvc.perform(get("/api/outbox-dlq/events/count")
                        .param("status", DlqStatus.RESOLVED.name()))
                .andExpect(status().isOk())
                .andExpect(content().string("42"));
    }

    @Test
    @DisplayName("IT GET /count with eventType only should return 200")
    void getCount_eventTypeOnly_returns200() throws Exception {
        when(manager.count(null, "ORDER_CREATED")).thenReturn(7L);

        mockMvc.perform(get("/api/outbox-dlq/events/count")
                        .param("eventType", "ORDER_CREATED"))
                .andExpect(status().isOk())
                .andExpect(content().string("7"));
    }

    @Test
    @DisplayName("IT GET /count with both status and eventType should return 200")
    void getCount_statusAndEventType_returns200() throws Exception {
        when(manager.count(DlqStatus.RESOLVED, "ORDER_CREATED")).thenReturn(3L);

        mockMvc.perform(get("/api/outbox-dlq/events/count")
                        .param("status", DlqStatus.RESOLVED.name())
                        .param("eventType", "ORDER_CREATED"))
                .andExpect(status().isOk())
                .andExpect(content().string("3"));
    }

    @Test
    @DisplayName("IT GET /count without parameters should return 200")
    void getCount_noParams_returns200() throws Exception {
        when(manager.count(null, null)).thenReturn(100L);

        mockMvc.perform(get("/api/outbox-dlq/events/count"))
                .andExpect(status().isOk())
                .andExpect(content().string("100"));
    }

    @Test
    @DisplayName("IT GET /count with invalid status should return 400")
    void getCount_invalidStatus_returns400() throws Exception {
        mockMvc.perform(get("/api/outbox-dlq/events/count")
                        .param("status", "INVALID_ENUM_VALUE"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("IT GET /count when database error should return 500")
    void getCount_databaseError_returns500() throws Exception {
        when(manager.count(DlqStatus.RESOLVED, null))
                .thenThrow(new DataAccessException("DB error") {});

        mockMvc.perform(get("/api/outbox-dlq/events/count")
                        .param("status", DlqStatus.RESOLVED.name()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.type").value("https://oncebox.io/errors/internal-error"));
    }

    @Test
    @DisplayName("IT GET /count should return zero when no events match")
    void getCount_noMatchingEvents_returnsZero() throws Exception {
        when(manager.count(DlqStatus.RESOLVED, "NON_EXISTENT_TYPE")).thenReturn(0L);

        mockMvc.perform(get("/api/outbox-dlq/events/count")
                        .param("status", DlqStatus.RESOLVED.name())
                        .param("eventType", "NON_EXISTENT_TYPE"))
                .andExpect(status().isOk())
                .andExpect(content().string("0"));
    }

    @Test
    @DisplayName("IT GET /batch with eventType only should return 200")
    void getBatch_eventTypeOnly_returns200() throws Exception {
        when(manager.findBatch(any(BatchRequest.class)))
                .thenReturn(List.of(buildEvent(UUID.randomUUID())));

        mockMvc.perform(get("/api/outbox-dlq/events/batch")
                        .param("eventType", "ORDER_CREATED")
                        .param("batchNumber", "0")
                        .param("batchSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("IT GET /batch with both status and eventType should return 200")
    void getBatch_statusAndEventType_returns200() throws Exception {
        when(manager.findBatch(any(BatchRequest.class)))
                .thenReturn(List.of(buildEvent(UUID.randomUUID())));

        mockMvc.perform(get("/api/outbox-dlq/events/batch")
                        .param("status", DlqStatus.RESOLVED.name())
                        .param("eventType", "ORDER_CREATED")
                        .param("batchNumber", "0")
                        .param("batchSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("IT GET /batch without filters should return 200")
    void getBatch_noFilters_returns200() throws Exception {
        when(manager.findBatch(any(BatchRequest.class)))
                .thenReturn(List.of(buildEvent(UUID.randomUUID())));

        mockMvc.perform(get("/api/outbox-dlq/events/batch")
                        .param("batchNumber", "0")
                        .param("batchSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("IT GET /batch should return empty array when no events match")
    void getBatch_noMatchingEvents_returnsEmptyArray() throws Exception {
        when(manager.findBatch(any(BatchRequest.class)))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/outbox-dlq/events/batch")
                        .param("status", DlqStatus.RESOLVED.name())
                        .param("batchNumber", "0")
                        .param("batchSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @DisplayName("IT PATCH /{id} should return 204")
    void updateStatus_validRequest_returns204() throws Exception {
        UUID id = UUID.randomUUID();
        DlqStatusDto dto = new DlqStatusDto(DlqStatus.RESOLVED);
        doNothing().when(manager).updateStatus(eq(id), any(DlqStatus.class));

        mockMvc.perform(patch("/api/outbox-dlq/events/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("IT PATCH /{id} with null status should return 400")
    void updateStatus_nullStatus_returns400() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(patch("/api/outbox-dlq/events/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": null}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("https://oncebox.io/errors/validation-failed"))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    @DisplayName("IT PATCH /{id} when not found should return 404")
    void updateStatus_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        DlqStatusDto dto = new DlqStatusDto(DlqStatus.RESOLVED);
        doThrow(new OutboxDlqEventNotFoundException("Not found")).when(manager).updateStatus(eq(id), any(DlqStatus.class));

        mockMvc.perform(patch("/api/outbox-dlq/events/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("https://oncebox.io/errors/dlq-event-not-found"));
    }

    @Test
    @DisplayName("IT PATCH /{id} when event is IN_PROCESS should return 409")
    void updateStatus_whenEventInProcess_shouldReturn409() throws Exception {
        UUID id = UUID.randomUUID();
        DlqStatusDto dto = new DlqStatusDto(DlqStatus.RESOLVED);
        doThrow(new OutboxDlqEventInProcessException(id))
                .when(manager).updateStatus(eq(id), any(DlqStatus.class));

        mockMvc.perform(patch("/api/outbox-dlq/events/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("https://oncebox.io/errors/dlq-event-in-process"))
                .andExpect(jsonPath("$.detail").value("Outbox DLQ event with id=%s is IN_PROCESS, interaction impossible".formatted(id)));
    }

    @Test
    @DisplayName("IT PATCH /batch when database error should return 500")
    void updateBatchStatus_databaseError_returns500() throws Exception {
        BatchUpdateRequest request = new BatchUpdateRequest(
                Set.of(UUID.randomUUID()),
                null,
                DlqStatus.RESOLVED
        );
        when(manager.updateBatchStatus(any(BatchUpdateRequest.class)))
                .thenThrow(new DataAccessException("DB error") {});

        mockMvc.perform(patch("/api/outbox-dlq/events/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.type").value("https://oncebox.io/errors/internal-error"));
    }

    @Test
    @DisplayName("IT PATCH /batch when all events are IN_PROCESS should return 200 with zero updated")
    void updateBatchStatus_allInProcess_returnsZeroUpdated() throws Exception {
        BatchUpdateRequest request = new BatchUpdateRequest(
                Set.of(UUID.randomUUID(), UUID.randomUUID()),
                null,
                DlqStatus.RESOLVED
        );
        when(manager.updateBatchStatus(any(BatchUpdateRequest.class)))
                .thenReturn(BatchModificationResponse.ofUpdate(2, 0));

        mockMvc.perform(patch("/api/outbox-dlq/events/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestedCount").value(2))
                .andExpect(jsonPath("$.processedCount").value(0));
    }

    @Test
    @DisplayName("IT PATCH /batch with partial update should return actual count")
    void updateBatchStatus_partialUpdate_returnsActualCount() throws Exception {
        BatchUpdateRequest request = new BatchUpdateRequest(
                Set.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()),
                null,
                DlqStatus.RESOLVED
        );
        when(manager.updateBatchStatus(any(BatchUpdateRequest.class)))
                .thenReturn(BatchModificationResponse.ofUpdate(3, 2));

        mockMvc.perform(patch("/api/outbox-dlq/events/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestedCount").value(3))
                .andExpect(jsonPath("$.processedCount").value(2));
    }

    @Test
    @DisplayName("IT PATCH /batch with eventType only should return 200")
    void updateBatchStatus_eventTypeOnly_returns200() throws Exception {
        BatchUpdateRequest request = new BatchUpdateRequest(
                null,
                "ORDER_CREATED",
                DlqStatus.RESOLVED
        );
        when(manager.updateBatchStatus(any(BatchUpdateRequest.class)))
                .thenReturn(BatchModificationResponse.ofUpdate(5));

        mockMvc.perform(patch("/api/outbox-dlq/events/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processedCount").value(5));
    }

    @Test
    @DisplayName("IT PATCH /batch should return 200")
    void updateBatchStatus_validRequest_returns200() throws Exception {
        BatchUpdateRequest request = new BatchUpdateRequest(
                Set.of(UUID.randomUUID(), UUID.randomUUID()),
                null,
                DlqStatus.RESOLVED
        );
        when(manager.updateBatchStatus(any(BatchUpdateRequest.class)))
                .thenReturn(BatchModificationResponse.ofUpdate(2, 2));

        mockMvc.perform(patch("/api/outbox-dlq/events/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("IT PATCH /batch with blank event type and empty ids should return 400")
    void updateBatchStatus_blankEventTypeEmptyIds_returns400() throws Exception {
        BatchUpdateRequest request = new BatchUpdateRequest(Set.of(), "  ", DlqStatus.RESOLVED);

        mockMvc.perform(patch("/api/outbox-dlq/events/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("https://oncebox.io/errors/validation-failed"))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    @DisplayName("IT PATCH /batch with null event type and null ids should return 400")
    void updateBatchStatus_nullEventTypeNullIds_returns400() throws Exception {
        BatchUpdateRequest request = new BatchUpdateRequest(null, null, DlqStatus.RESOLVED);

        mockMvc.perform(patch("/api/outbox-dlq/events/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("https://oncebox.io/errors/validation-failed"))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    @DisplayName("IT PATCH /batch with blank event and ids should return 200")
    void updateBatchStatus_blankEventTypeAndGoodIds_returns200() throws Exception {
        BatchUpdateRequest request = new BatchUpdateRequest(Set.of(UUID.randomUUID()), "  ", DlqStatus.RESOLVED);
        when(manager.updateBatchStatus(any(BatchUpdateRequest.class)))
                .thenReturn(BatchModificationResponse.ofUpdate(1, 1));

        mockMvc.perform(patch("/api/outbox-dlq/events/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("IT PATCH /batch with event and empty ids should return 200")
    void updateBatchStatus_goodEventTypeAndEmptyIds_returns200() throws Exception {
        BatchUpdateRequest request = new BatchUpdateRequest(Set.of(), "event-type", DlqStatus.RESOLVED);
        when(manager.updateBatchStatus(any(BatchUpdateRequest.class)))
                .thenReturn(BatchModificationResponse.ofUpdate(0, 0));

        mockMvc.perform(patch("/api/outbox-dlq/events/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("IT PATCH /batch with null status should return 400")
    void updateBatchStatus_nullStatus_returns400() throws Exception {
        mockMvc.perform(patch("/api/outbox-dlq/events/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ids\": [\"" + UUID.randomUUID() + "\"], \"status\": null}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    @DisplayName("IT PATCH /batch when both ids and eventType provided should return 400")
    void updateBatchStatus_bothParamsProvided_returns400() throws Exception {
        BatchUpdateRequest request = new BatchUpdateRequest(
                Set.of(UUID.randomUUID()),
                "event-type",
                DlqStatus.RESOLVED
        );

        mockMvc.perform(patch("/api/outbox-dlq/events/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("https://oncebox.io/errors/validation-failed"))
                .andExpect(jsonPath("$.errors[0].message")
                        .value("exactly one of ids and eventType must be provided"));
    }

    @Test
    @DisplayName("IT PATCH /batch when neither ids nor eventType provided should return 400")
    void updateBatchStatus_noParams_returns400() throws Exception {
        BatchUpdateRequest request = new BatchUpdateRequest(
                Set.of(),
                "   ",
                DlqStatus.RESOLVED
        );

        mockMvc.perform(patch("/api/outbox-dlq/events/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].message")
                        .value("exactly one of ids and eventType must be provided"));
    }

    @Test
    @DisplayName("IT PATCH /batch when ids size exceeds limit should return 400")
    void updateBatchStatus_idsTooLarge_returns400() throws Exception {
        Set<UUID> ids = java.util.stream.IntStream.range(0, 1001)
                .mapToObj(i -> UUID.randomUUID())
                .collect(java.util.stream.Collectors.toSet());

        BatchUpdateRequest request = new BatchUpdateRequest(
                ids,
                null,
                DlqStatus.RESOLVED
        );

        mockMvc.perform(patch("/api/outbox-dlq/events/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].message")
                        .value("ids must contain at most 1000 elements"));
    }

    @Test
    @DisplayName("IT DELETE /{id} should return 204")
    void delete_existingId_returns204() throws Exception {
        UUID id = UUID.randomUUID();
        when(manager.deleteById(id)).thenReturn(1);

        mockMvc.perform(delete("/api/outbox-dlq/events/{id}", id))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("IT DELETE /{id} when not found should return 404")
    void delete_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(manager.deleteById(id)).thenThrow(new OutboxDlqEventNotFoundException("Not found"));

        mockMvc.perform(delete("/api/outbox-dlq/events/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("https://oncebox.io/errors/dlq-event-not-found"));
    }

    @Test
    @DisplayName("IT DELETE /{id} when unexpected error should return 500")
    void delete_unexpectedError_returns500() throws Exception {
        UUID id = UUID.randomUUID();
        when(manager.deleteById(id)).thenThrow(new RuntimeException("Unexpected"));

        mockMvc.perform(delete("/api/outbox-dlq/events/{id}", id))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.type").value("https://oncebox.io/errors/internal-error"));
    }

    @Test
    @DisplayName("IT DELETE /{id} when database error should return 500")
    void delete_databaseError_returns500() throws Exception {
        UUID id = UUID.randomUUID();
        when(manager.deleteById(id)).thenThrow(new DataAccessException("DB error") {});

        mockMvc.perform(delete("/api/outbox-dlq/events/{id}", id))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.type").value("https://oncebox.io/errors/internal-error"));
    }

    @Test
    @DisplayName("IT DELETE /batch should return 200")
    void deleteBatch_validRequest_returns204() throws Exception {
        BatchDeleteRequest request = new BatchDeleteRequest(
                Set.of(UUID.randomUUID(), UUID.randomUUID()),
                null
        );
        when(manager.deleteBatch(any())).thenReturn(BatchModificationResponse.ofDelete(2, 2));

        mockMvc.perform(delete("/api/outbox-dlq/events/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("IT DELETE /batch with blank event type and empty ids should return 400")
    void deleteBatch_emptyIds_returns400() throws Exception {
        BatchDeleteRequest request = new BatchDeleteRequest(Set.of(), "  ");

        mockMvc.perform(delete("/api/outbox-dlq/events/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("https://oncebox.io/errors/validation-failed"))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    @DisplayName("IT DELETE /batch with null ids should return 400")
    void deleteBatch_nullIds_returns400() throws Exception {
        mockMvc.perform(delete("/api/outbox-dlq/events/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ids\": null, \"eventType\": null}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("IT DELETE /batch when database error should return 500")
    void deleteBatch_databaseError_returns500() throws Exception {
        BatchDeleteRequest request = new BatchDeleteRequest(Set.of(UUID.randomUUID()), null);
        when(manager.deleteBatch(any())).thenThrow(new DataAccessException("DB error") {});

        mockMvc.perform(delete("/api/outbox-dlq/events/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.type").value("https://oncebox.io/errors/internal-error"));
    }

    @Test
    @DisplayName("IT DELETE /batch when both ids and eventType provided should return 400")
    void deleteBatch_bothParamsProvided_returns400() throws Exception {
        BatchDeleteRequest request = new BatchDeleteRequest(
                Set.of(UUID.randomUUID()),
                "event-type"
        );

        mockMvc.perform(delete("/api/outbox-dlq/events/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("https://oncebox.io/errors/validation-failed"))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors[0].message")
                        .value("exactly one of ids and eventType must be provided"));
    }

    @Test
    @DisplayName("IT DELETE /batch when ids size exceeds limit should return 400")
    void deleteBatch_idsTooLarge_returns400() throws Exception {
        Set<UUID> ids = java.util.stream.IntStream.range(0, 1001)
                .mapToObj(i -> UUID.randomUUID())
                .collect(java.util.stream.Collectors.toSet());

        BatchDeleteRequest request = new BatchDeleteRequest(ids, null);

        mockMvc.perform(delete("/api/outbox-dlq/events/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].message")
                        .value("ids must contain at most 1000 elements"));
    }

    @Test
    @DisplayName("IT error response contains timestamp and path properties")
    void errorResponse_containsTimestampAndPath() throws Exception {
        UUID id = UUID.randomUUID();
        when(manager.findById(id)).thenThrow(new OutboxDlqEventNotFoundException("Not found"));

        mockMvc.perform(get("/api/outbox-dlq/events/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.path").value("/api/outbox-dlq/events/" + id))
                .andExpect(jsonPath("$.instance").value("/api/outbox-dlq/events/" + id));
    }

    @Test
    @DisplayName("IT validation error response contains errors array with field and message")
    void validationError_containsFieldErrors() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(patch("/api/outbox-dlq/events/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": null}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").exists())
                .andExpect(jsonPath("$.errors[0].message").exists());
    }

    @Test
    @DisplayName("IT DELETE /batch with eventType only should return 200")
    void deleteBatch_eventTypeOnly_returns200() throws Exception {
        BatchDeleteRequest request = new BatchDeleteRequest(null, "ORDER_CREATED");
        when(manager.deleteBatch(any())).thenReturn(BatchModificationResponse.ofDelete(5));

        mockMvc.perform(delete("/api/outbox-dlq/events/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processedCount").value(5));
    }

    @Test
    @DisplayName("IT DELETE /batch when all events are IN_PROCESS should return 200 with zero deleted")
    void deleteBatch_allInProcess_returnsZeroDeleted() throws Exception {
        BatchDeleteRequest request = new BatchDeleteRequest(
                Set.of(UUID.randomUUID(), UUID.randomUUID()),
                null
        );
        when(manager.deleteBatch(any())).thenReturn(BatchModificationResponse.ofDelete(2, 0));

        mockMvc.perform(delete("/api/outbox-dlq/events/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestedCount").value(2))
                .andExpect(jsonPath("$.processedCount").value(0));
    }

    @Test
    @DisplayName("IT DELETE /batch with partial delete should return actual count")
    void deleteBatch_partialDelete_returnsActualCount() throws Exception {
        BatchDeleteRequest request = new BatchDeleteRequest(
                Set.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()),
                null
        );
        when(manager.deleteBatch(any())).thenReturn(BatchModificationResponse.ofDelete(3, 2));

        mockMvc.perform(delete("/api/outbox-dlq/events/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestedCount").value(3))
                .andExpect(jsonPath("$.processedCount").value(2));
    }


    @Test
    @DisplayName("IT DELETE /{id} when event is IN_PROCESS should return 409")
    void delete_whenEventInProcess_shouldReturn409() throws Exception {
        UUID id = UUID.randomUUID();
        when(manager.deleteById(id))
                .thenThrow(new OutboxDlqEventInProcessException(id));

        mockMvc.perform(delete("/api/outbox-dlq/events/{id}", id))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("https://oncebox.io/errors/dlq-event-in-process"))
                .andExpect(jsonPath("$.title").value("DLQ event is in process"))
                .andExpect(jsonPath("$.detail").value("Outbox DLQ event with id=%s is IN_PROCESS, interaction impossible".formatted(id)));
    }

    @Test
    @DisplayName("IT PATCH /batch when body is malformed JSON should return 400")
    void updateBatchStatus_whenBodyIsMalformedJson_shouldReturn400() throws Exception {
        // when / then
        mockMvc.perform(patch("/api/outbox-dlq/events/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{bad"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(manager);
    }

    @Test
    @DisplayName("IT PATCH /batch when body is missing should return 400")
    void updateBatchStatus_whenBodyIsMissing_shouldReturn400() throws Exception {
        // when / then
        mockMvc.perform(patch("/api/outbox-dlq/events/batch")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(not(containsString("OutboxDlqController"))));
    }

    @Test
    @DisplayName("IT PATCH /{id} when body status is not a DlqStatus constant should return 400")
    void updateStatus_whenBodyStatusIsUnknown_shouldReturn400() throws Exception {
        // when / then
        mockMvc.perform(patch("/api/outbox-dlq/events/{id}", EVENT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"FOO\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(manager);
    }

    @Test
    @DisplayName("IT DELETE /batch when ids hold a malformed UUID should return 400")
    void deleteBatch_whenIdIsMalformed_shouldReturn400() throws Exception {
        // when / then
        mockMvc.perform(delete("/api/outbox-dlq/events/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ids\": [\"x\"]}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(manager);
    }

    @Test
    @DisplayName("IT PATCH /batch when content type is not JSON should return 415")
    void updateBatchStatus_whenContentTypeIsNotJson_shouldReturn415() throws Exception {
        // when / then
        mockMvc.perform(patch("/api/outbox-dlq/events/batch")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("x"))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    @DisplayName("IT GET /{id} when unexpected error should answer a fixed detail without the exception message")
    void get_whenUnexpectedError_shouldNotExposeExceptionMessage() throws Exception {
        // given
        when(manager.findById(EVENT_ID)).thenThrow(new IllegalStateException("secret internal state"));

        // when / then
        mockMvc.perform(get("/api/outbox-dlq/events/{id}", EVENT_ID))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.detail").value("The operation could not be completed"));
    }

    @Test
    @DisplayName("IT GET /{id} when database error should answer like any unexpected error, without the cause")
    void get_whenDatabaseError_shouldNotExposeCause() throws Exception {
        // given
        when(manager.findById(EVENT_ID)).thenThrow(new DataAccessResourceFailureException(
                "select * from outbox_dlq_events", new RuntimeException("password authentication failed")
        ));

        // when / then
        mockMvc.perform(get("/api/outbox-dlq/events/{id}", EVENT_ID))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.type").value("https://oncebox.io/errors/internal-error"))
                .andExpect(jsonPath("$.detail").value("The operation could not be completed"));
    }

    @Test
    @DisplayName("IT PATCH /{id} when target status is IN_PROCESS should return 400 without reaching the service")
    void updateStatus_whenTargetStatusIsInProcess_shouldReturn400() throws Exception {
        // when / then
        mockMvc.perform(patch("/api/outbox-dlq/events/{id}", EVENT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new DlqStatusDto(DlqStatus.IN_PROCESS))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("status"));
        verifyNoInteractions(manager);
    }

    @Test
    @DisplayName("IT PATCH /batch when target status is IN_PROCESS should return 400 without reaching the service")
    void updateBatchStatus_whenTargetStatusIsInProcess_shouldReturn400() throws Exception {
        // given
        BatchUpdateRequest request = new BatchUpdateRequest(Set.of(EVENT_ID), null, DlqStatus.IN_PROCESS);

        // when / then
        mockMvc.perform(patch("/api/outbox-dlq/events/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("status"));
        verifyNoInteractions(manager);
    }

    @Test
    @DisplayName("IT PATCH /batch when both ids and eventType provided should report both JSON fields")
    void updateBatchStatus_whenBothIdsAndEventType_shouldReportBothFields() throws Exception {
        // given
        BatchUpdateRequest request = new BatchUpdateRequest(Set.of(EVENT_ID), "event-type", DlqStatus.RESOLVED);

        // when / then
        mockMvc.perform(patch("/api/outbox-dlq/events/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field", containsInAnyOrder("ids", "eventType")));
    }

    @Test
    @DisplayName("IT DELETE /batch when neither ids nor eventType provided should report both JSON fields")
    void deleteBatch_whenNeitherIdsNorEventType_shouldReportBothFields() throws Exception {
        // when / then
        mockMvc.perform(delete("/api/outbox-dlq/events/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field", containsInAnyOrder("ids", "eventType")));
    }

    @Test
    @DisplayName("IT GET /batch when status is unknown should name the field without exposing Java types")
    void getBatch_whenStatusIsUnknown_shouldNotExposeJavaTypes() throws Exception {
        // when / then
        mockMvc.perform(get("/api/outbox-dlq/events/batch")
                        .param("status", "FOO")
                        .param("batchNumber", "0")
                        .param("batchSize", "10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("status"))
                .andExpect(jsonPath("$.errors[0].message").value(not(containsString("io."))));
    }

    @Test
    @DisplayName("IT GET /batch when batch size is below the minimum should say so with the bound")
    void getBatch_whenBatchSizeBelowMinimum_shouldReturnMessageWithBound() throws Exception {
        // when / then
        mockMvc.perform(get("/api/outbox-dlq/events/batch")
                        .param("batchNumber", "0")
                        .param("batchSize", "5"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("batchSize"))
                .andExpect(jsonPath("$.errors[0].message").value("batchSize must be at least 10"));
    }

    @Test
    @DisplayName("IT GET /batch when batch number is not a number should name the field and its type")
    void getBatch_whenBatchNumberIsNotNumber_shouldNameFieldAndType() throws Exception {
        // when / then
        mockMvc.perform(get("/api/outbox-dlq/events/batch")
                        .param("batchNumber", "abc")
                        .param("batchSize", "10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("batchNumber"))
                .andExpect(jsonPath("$.errors[0].message").value("batchNumber must be a valid int"));
        verifyNoInteractions(manager);
    }

    private OutboxDlqEvent buildEvent(UUID id) {
        Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        return new OutboxDlqEvent(
                id,
                EventStatus.FAILED,
                "ORDER_CREATED",
                "io.example.OrderCreated",
                "{\"orderId\":\"123\"}",
                0,
                now.plusSeconds(60),
                now,
                now,
                DlqStatus.MOVED,
                now
        );
    }
}
