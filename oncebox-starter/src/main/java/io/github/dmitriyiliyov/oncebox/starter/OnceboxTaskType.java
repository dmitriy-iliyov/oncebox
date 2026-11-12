package io.github.dmitriyiliyov.oncebox.starter;

/**
 * Background tasks of oncebox. The value names the task in its logs and tags the timing of its runs in the
 * metrics.
 */
public enum OnceboxTaskType {
    OUTBOX_PROCESSED_CLEANUP("outbox-processed-cleanup"),
    INBOX_CLEANUP("inbox-cleanup"),
    OUTBOX_STUCK_RECOVERY("outbox-stuck-recovery"),
    OUTBOX_DLQ_TRANSFER_TO("outbox-dlq-transfer-to"),
    OUTBOX_DLQ_TRANSFER_FROM("outbox-dlq-transfer-from"),
    OUTBOX_DLQ_CLEANUP("outbox-dlq-cleanup");

    private final String value;

    OnceboxTaskType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
