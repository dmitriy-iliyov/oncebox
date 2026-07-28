package io.github.dmitriyiliyov.oncebox.core.locks;

import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class DistributedLockRepositoryVerifier {

    private final JdbcTemplate jdbcTemplate;
    private final DistributedLockRepository repository;
    private final IdExtractor idExtractor;
    private final IdPreparer idPreparer;
    private final SetupJobScript setupJobScript;

    @FunctionalInterface
    public interface IdExtractor {
        UUID extract(Object raw);
    }

    @FunctionalInterface
    public interface IdPreparer {
        Object prepare(UUID id);
    }

    public DistributedLockRepositoryVerifier(
            JdbcTemplate jdbcTemplate,
            DistributedLockRepository repository,
            IdExtractor idExtractor,
            IdPreparer idPreparer, SetupJobScript setupJobScript
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.repository = repository;
        this.idExtractor = idExtractor;
        this.idPreparer = idPreparer;
        this.setupJobScript = setupJobScript;
    }

    public void tryLock_jobAvailable_locksSuccessfully() {
        UUID workerId = UUID.randomUUID();
        String jobName = "test-job-" + UUID.randomUUID();
        setupJobScript.setup(jdbcTemplate, idPreparer, jobName, 100L, 500L);

        boolean locked = repository.tryLock(jobName, workerId);

        assertThat(locked).isTrue();
        assertThat(getLockedBy(jobName)).isEqualTo(workerId);
    }

    public void tryLock_jobAlreadyLocked_returnsFalse() {
        UUID worker1 = UUID.randomUUID();
        UUID worker2 = UUID.randomUUID();
        String jobName = "test-job-" + UUID.randomUUID();
        setupJobScript.setup(jdbcTemplate, idPreparer, jobName, 100L, 500L);

        repository.tryLock(jobName, worker1);
        boolean locked = repository.tryLock(jobName, worker2);

        assertThat(locked).isFalse();
        assertThat(getLockedBy(jobName)).isEqualTo(worker1);
    }

    public void tryLock_lockExpired_locksSuccessfully() {
        UUID worker1 = UUID.randomUUID();
        UUID worker2 = UUID.randomUUID();
        String jobName = "test-job-" + UUID.randomUUID();
        setupJobScript.setup(jdbcTemplate, idPreparer, jobName, 50L, 100L);

        repository.tryLock(jobName, worker1);
        waitForLockExpiration();
        boolean locked = repository.tryLock(jobName, worker2);

        assertThat(locked).isTrue();
        assertThat(getLockedBy(jobName)).isEqualTo(worker2);
    }

    public void unlock_validWorker_unlocksJob() {
        UUID workerId = UUID.randomUUID();
        String jobName = "test-job-" + UUID.randomUUID();
        setupJobScript.setup(jdbcTemplate, idPreparer, jobName, 100L, 500L);

        repository.tryLock(jobName, workerId);
        repository.unlock(jobName, workerId);

        waitForLockExpiration();
        boolean canLock = repository.tryLock(jobName, UUID.randomUUID());
        assertThat(canLock).isTrue();
    }

    public void unlock_differentWorker_doesNotUnlock() {
        UUID worker1 = UUID.randomUUID();
        UUID worker2 = UUID.randomUUID();
        String jobName = "test-job-" + UUID.randomUUID();
        setupJobScript.setup(jdbcTemplate, idPreparer, jobName, 100L, 500L);

        repository.tryLock(jobName, worker1);
        repository.unlock(jobName, worker2);

        boolean locked = repository.tryLock(jobName, worker2);
        assertThat(locked).isFalse();
        assertThat(getLockedBy(jobName)).isEqualTo(worker1);
    }

    public void tryLock_nonExistentJob_returnsFalse() {
        UUID workerId = UUID.randomUUID();
        String jobName = "non-existent-job-" + UUID.randomUUID();

        boolean locked = repository.tryLock(jobName, workerId);

        assertThat(locked).isFalse();
    }

    private UUID getLockedBy(String jobName) {
        return jdbcTemplate.query(
                "SELECT locked_by FROM outbox_jobs WHERE job_name = ?",
                ps -> ps.setString(1, jobName),
                (rs, n) -> idExtractor.extract(rs.getObject("locked_by"))
        ).stream().findFirst().orElse(null);
    }

    private void waitForLockExpiration() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}