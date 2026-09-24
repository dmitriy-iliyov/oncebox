package io.github.dmitriyiliyov.oncebox.mysql;

import io.github.dmitriyiliyov.oncebox.core.locks.DistributedLockRepositoryVerifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.nio.ByteBuffer;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class MySqlDistributedLockRepositoryIntegrationTests extends BaseMySqlIntegrationTests {

    private DistributedLockRepositoryVerifier verifier;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private MySqlDistributedLockRepository repository;
    private MySqlUuidHelper mySqlUuidHelper;

    @BeforeEach
    void setUp() {
        mySqlUuidHelper = new MySqlUuidHelper();
        repository = new MySqlDistributedLockRepository(jdbcTemplate, mySqlUuidHelper);
        this.verifier = new DistributedLockRepositoryVerifier(
                jdbcTemplate,
                repository,
                raw -> {
                    byte[] bytes = (byte[]) raw;
                    ByteBuffer bb = ByteBuffer.wrap(bytes);
                    return new UUID(bb.getLong(), bb.getLong());
                },
                id -> {
                    ByteBuffer bb = ByteBuffer.allocate(16);
                    bb.putLong(id.getMostSignificantBits());
                    bb.putLong(id.getLeastSignificantBits());
                    return bb.array();
                },
                (jdbcTemplate, idPreparer, jobName, lockAtLeastFor, lockAtMostFor) -> {
                    String sql = """
                        INSERT INTO outbox_jobs (job_name, lock_until, locked_by, locked_at, lock_at_least_for, lock_at_most_for) 
                        VALUES (?, TIMESTAMPADD(MICROSECOND, lock_at_most_for * 1000, UTC_TIMESTAMP(3)), ?, TIMESTAMPADD(MICROSECOND, lock_at_most_for * 1000, UTC_TIMESTAMP(3)), ?, ?)
                    """;
                    jdbcTemplate.update(sql, jobName, idPreparer.prepare(UUID.randomUUID()), lockAtLeastFor, lockAtMostFor);
                }
        );
    }

    @Test
    @DisplayName("UT constructor when jdbcTemplate is null should throw NullPointerException")
    void constructor_whenJdbcTemplateIsNull_shouldThrowNullPointerException() {
        assertThatThrownBy(() -> new MySqlDistributedLockRepository(null, mySqlUuidHelper))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("jdbcTemplate cannot be null");
    }

    @Test
    @DisplayName("UT constructor when uuidHelper is null should throw NullPointerException")
    void constructor_whenUuidHelperIsNull_shouldThrowNullPointerException() {
        assertThatThrownBy(() -> new MySqlDistributedLockRepository(jdbcTemplate, null))
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
