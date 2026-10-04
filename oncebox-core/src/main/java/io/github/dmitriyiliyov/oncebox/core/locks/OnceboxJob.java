package io.github.dmitriyiliyov.oncebox.core.locks;

public enum OnceboxJob {
    OUTBOX_PROCESSED_CLEANUP("outbox-processed-cleanup"),
    OUTBOX_DLQ_CLEANUP("outbox-dlq-cleanup"),
    INBOX_CLEANUP("inbox-cleanup");

    private final String jobName;

    OnceboxJob(String jobName) {
        this.jobName = jobName;
    }

    public String getJobName() {
        return jobName;
    }
}
