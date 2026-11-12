package io.github.dmitriyiliyov.oncebox.core.locks;

/**
 * Clean-up jobs that run on one instance at a time. The job name is the key of the job's row in
 * {@code oncebox_jobs}, which holds the distributed lock.
 */
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
