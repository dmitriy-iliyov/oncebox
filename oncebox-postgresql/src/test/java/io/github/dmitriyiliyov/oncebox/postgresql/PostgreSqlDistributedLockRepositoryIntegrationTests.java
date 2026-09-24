package io.github.dmitriyiliyov.oncebox.postgresql;

import io.github.dmitriyiliyov.oncebox.core.locks.DistributedLockRepositoryVerifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class PostgreSqlDistributedLockRepositoryIntegrationTests extends BasePostgresSqlIntegrationTests {

    private DistributedLockRepositoryVerifier verifier;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private PostgreSqlDistributedLockRepository repository;
    private PostgreSqlUuidHelper postgreSqlUuidHelper;

    @BeforeEach
    void setUp() {
        postgreSqlUuidHelper = new PostgreSqlUuidHelper();
        repository = new PostgreSqlDistributedLockRepository(jdbcTemplate, postgreSqlUuidHelper);
        this.verifier = new DistributedLockRepositoryVerifier(
                jdbcTemplate,
                repository,
                raw -> (UUID) raw,
                id -> id,
                (jdbcTemplate, idPreparer, jobName, lockAtLeastFor, lockAtMostFor) -> {
                    Timestamp stub = Timestamp.from(Instant.now().minus(1, ChronoUnit.HOURS));
                    String sql = """
                        INSERT INTO outbox_jobs (job_name, lock_until, locked_by, locked_at, lock_at_least_for, lock_at_most_for) 
                        VALUES (?, ?, ?, ?, ?, ?)
                    """;
                    jdbcTemplate.update(
                            sql,
                            jobName,
                            stub,
                            idPreparer.prepare(UUID.randomUUID()),
                            stub,
                            lockAtLeastFor,
                            lockAtMostFor
                    );
                }
        );
    }

    @Test
    @DisplayName("UT constructor when jdbcTemplate is null should throw NullPointerException")
    void constructor_whenJdbcTemplateIsNull_shouldThrowNullPointerException() {
        assertThatThrownBy(() -> new PostgreSqlDistributedLockRepository(null, postgreSqlUuidHelper))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("jdbcTemplate cannot be null");
    }

    @Test
    @DisplayName("UT constructor when uuidHelper is null should throw NullPointerException")
    void constructor_whenUuidHelperIsNull_shouldThrowNullPointerException() {
        assertThatThrownBy(() -> new PostgreSqlDistributedLockRepository(jdbcTemplate, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("uuidHelper cannot be null");
    }

    @Test
    @DisplayName("IT tryLock() should lock job when available")
    void tryLock_jobAvailable_locksSuccessfully() {
        verifier.tryLock_jobAvailable_locksSuccessfully();
    }

    @Test
    @DisplayName("IT tryLock() should return false when job already locked")
    void tryLock_jobAlreadyLocked_returnsFalse() {
        verifier.tryLock_jobAlreadyLocked_returnsFalse();
    }

    @Test
    @DisplayName("IT tryLock() should lock job when lock expired")
    void tryLock_lockExpired_locksSuccessfully() {
        verifier.tryLock_lockExpired_locksSuccessfully();
    }

    @Test
    @DisplayName("IT unlock() should unlock job for valid worker")
    void unlock_validWorker_unlocksJob() {
        verifier.unlock_validWorker_unlocksJob();
    }

    @Test
    @DisplayName("IT unlock() should not unlock for different worker")
    void unlock_differentWorker_doesNotUnlock() {
        verifier.unlock_differentWorker_doesNotUnlock();
    }

    @Test
    @DisplayName("IT tryLock() should return false for non-existent job")
    void tryLock_nonExistentJob_returnsFalse() {
        verifier.tryLock_nonExistentJob_returnsFalse();
    }
}