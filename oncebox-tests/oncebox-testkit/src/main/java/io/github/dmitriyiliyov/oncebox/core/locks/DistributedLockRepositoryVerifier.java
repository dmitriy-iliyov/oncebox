package io.github.dmitriyiliyov.oncebox.core.locks;

import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Objects;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class DistributedLockRepositoryVerifier {

    private final JdbcTemplate jdbcTemplate;
    private final DistributedLockRepository repository;
    private final IdExtractor idExtractor;
    private final IdPreparer idPreparer;
    private final SetupJobScript setupJobScript;

    /**
     * Reads a {@link UUID} back from the value the dialect's JDBC driver returns for an id column.
     */
    @FunctionalInterface
    public interface IdExtractor {
        UUID extract(Object raw);
    }

    /**
     * Converts a {@link UUID} into the value the dialect's JDBC driver binds to an id column.
     */
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
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate cannot be null");
        this.repository = Objects.requireNonNull(repository, "repository cannot be null");
        this.idExtractor = Objects.requireNonNull(idExtractor, "idExtractor cannot be null");
        this.idPreparer = Objects.requireNonNull(idPreparer, "idPreparer cannot be null");
        this.setupJobScript = Objects.requireNonNull(setupJobScript, "setupJobScript cannot be null");
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
                "SELECT locked_by FROM oncebox_jobs WHERE job_name = ?",
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