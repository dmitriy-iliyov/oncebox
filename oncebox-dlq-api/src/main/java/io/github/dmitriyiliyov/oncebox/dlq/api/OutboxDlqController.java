
package io.github.dmitriyiliyov.oncebox.dlq.api;

import io.github.dmitriyiliyov.oncebox.core.publisher.dlq.DlqStatus;
import io.github.dmitriyiliyov.oncebox.core.publisher.dlq.OutboxDlqEvent;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api/outbox-dlq/events")
public class OutboxDlqController {

    private final OutboxDlqApiService service;

    public OutboxDlqController(OutboxDlqApiService service) {
        this.service = Objects.requireNonNull(service, "service cannot be null");
    }

    @Operation(summary = "Get event by id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Event successfully retrieved",
                    content = @Content(schema = @Schema(implementation = OutboxDlqEvent.class))),
            @ApiResponse(responseCode = "404", description = "Event not found", content = @Content)
    })
    @GetMapping("/{id}")
    public OutboxDlqEvent get(@Parameter(description = "Id of the DLQ event", required = true)
                              @PathVariable("id") UUID id) {
        return service.findById(id);
    }

    @Operation(summary = "Get a batch of DLQ events by status and/or event type (pages through all events if no filter is provided)")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200", description = "Event batch successfully retrieved",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = OutboxDlqEvent.class)))
            ),
            @ApiResponse(responseCode = "400", description = "Request validation failed", content = @Content)
    })
    @GetMapping("/batch")
    public List<OutboxDlqEvent> getBatch(@ParameterObject @ModelAttribute @Valid BatchRequest request) {
        return service.findBatch(request);
    }

    @Operation(summary = "Count DLQ events by status and/or event type (counts all events if no parameters are provided)")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200", description = "Event count successfully retrieved",
                    content = @Content(schema = @Schema(implementation = Long.class))
            ),
            @ApiResponse(responseCode = "400", description = "Request validation failed", content = @Content)
    })
    @GetMapping("/count")
    public Long getCount(@Parameter(description = "Filter by DLQ status")
                         @RequestParam(value = "status", required = false) DlqStatus status,
                         @Parameter(description = "Filter by event type")
                         @RequestParam(value = "eventType", required = false) String eventType) {
        return service.count(status, eventType);
    }

    @Operation(summary = "Update DLQ status of event by id")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Event successfully updated"),
            @ApiResponse(responseCode = "400", description = "Request validation failed", content = @Content),
            @ApiResponse(responseCode = "404", description = "Event not found", content = @Content),
            @ApiResponse(responseCode = "409", description = "Event is IN_PROCESS", content = @Content)
    })
    @PatchMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateStatus(@Parameter(description = "Id of the DLQ event to update", required = true)
                             @PathVariable("id") UUID id,
                             @RequestBody @Valid DlqStatusDto dto) {
        service.updateStatus(id, dto.status());
    }

    @Operation(summary = "Update DLQ status for multiple events by ids or event type")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200", description = "Event batch successfully updated",
                    content = @Content(schema = @Schema(implementation = BatchModificationResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Request validation failed", content = @Content)
    })
    @PatchMapping("/batch")
    public BatchModificationResponse updateBatchStatus(@RequestBody @Valid BatchUpdateRequest request) {
        return service.updateBatchStatus(request);
    }

    @Operation(summary = "Delete event by id")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Event successfully deleted"),
            @ApiResponse(responseCode = "404", description = "Event not found", content = @Content),
            @ApiResponse(responseCode = "409", description = "Event is IN_PROCESS", content = @Content)
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@Parameter(description = "Id of the DLQ event to delete", required = true)
                       @PathVariable("id") UUID id) {
        service.deleteById(id);
    }

    @Operation(summary = "Delete multiple events by ids or event type")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200", description = "Event batch successfully deleted",
                    content = @Content(schema = @Schema(implementation = BatchModificationResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Request validation failed", content = @Content)
    })
    @DeleteMapping("/batch")
    public BatchModificationResponse deleteBatch(@RequestBody @Valid BatchDeleteRequest request) {
        return service.deleteBatch(request);
    }
}