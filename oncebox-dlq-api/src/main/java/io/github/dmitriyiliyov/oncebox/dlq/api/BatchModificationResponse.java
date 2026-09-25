package io.github.dmitriyiliyov.oncebox.dlq.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Result of a batch operation over DLQ events")
public record BatchModificationResponse(

        @Schema(
                description = "Number of ids in the request, 0 when events were selected by event type",
                example = "100",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        int requestedCount,

        @Schema(
                description = "Number of events successfully processed",
                example = "95",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        int processedCount,

        @Schema(
                description = "Overall status of the batch operation",
                example = "PARTIAL_SUCCESS",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        OperationStatus status,

        @Schema(
                description = "Human-readable explanation of the operation result",
                example = "Some events were not updated because they were in IN_PROCESS status.",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String message

) {

    public static BatchModificationResponse ofUpdate(int requestedCount, int actualUpdatedCount) {
        OperationStatus status;
        String message;
        if (requestedCount == actualUpdatedCount) {
            status = OperationStatus.SUCCESS;
            message = "All events were successfully updated.";
        } else {
            status = OperationStatus.PARTIAL_SUCCESS;
            message = "Some events were not updated because they were in IN_PROCESS status.";
        }
        return new BatchModificationResponse(requestedCount, actualUpdatedCount, status, message);
    }

    public static BatchModificationResponse ofUpdate(int actualUpdatedCount) {
        return new BatchModificationResponse(
                0,
                actualUpdatedCount,
                OperationStatus.POSSIBLE_PARTIAL_SUCCESS,
                "Some events might not have been updated because they were in IN_PROCESS status."
        );
    }

    public static BatchModificationResponse ofDelete(int requestedCount, int actualDeletedCount) {
        OperationStatus status;
        String message;
        if (requestedCount == actualDeletedCount) {
            status = OperationStatus.SUCCESS;
            message = "All events were successfully deleted.";
        } else {
            status = OperationStatus.PARTIAL_SUCCESS;
            message = "Some events were not deleted because they were in IN_PROCESS status.";
        }
        return new BatchModificationResponse(requestedCount, actualDeletedCount, status, message);
    }


    public static BatchModificationResponse ofDelete(int actualDeletedCount) {
        return new BatchModificationResponse(
                0,
                actualDeletedCount,
                OperationStatus.POSSIBLE_PARTIAL_SUCCESS,
                "Some events might not have been deleted because they were in IN_PROCESS status."
        );
    }
}
