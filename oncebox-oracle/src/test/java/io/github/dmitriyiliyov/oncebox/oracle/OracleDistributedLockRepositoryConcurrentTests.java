package io.github.dmitriyiliyov.oncebox.oracle;

import io.github.dmitriyiliyov.oncebox.core.locks.DistributedLockRepositoryConcurrentVerifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.nio.ByteBuffer;
import java.util.UUID;

class OracleDistributedLockRepositoryConcurrentTests extends BaseOracleIntegrationTests {

    private DistributedLockRepositoryConcurrentVerifier verifier;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private OracleDistributedLockRepository repository;

    @BeforeEach
    void setUp() {
        repository = new OracleDistributedLockRepository(jdbcTemplate, new OracleSqlUuidHelper());
        this.verifier = new DistributedLockRepositoryConcurrentVerifier(
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
                        VALUES (?, SYS_EXTRACT_UTC(SYSTIMESTAMP) - INTERVAL '1' HOUR, NULL, SYS_EXTRACT_UTC(SYSTIMESTAMP) - INTERVAL '1' HOUR, ?, ?)
                    """;
                    jdbcTemplate.update(sql, jobName, lockAtLeastFor, lockAtMostFor);
                }
        );
        this.verifier.cleanUp();
    }

    @Test
    @DisplayName("CT tryLock() should allow only one thread to acquire lock")
    void concurrent_multipleTryLock_onlyOneSucceeds() throws Exception {
        verifier.concurrent_multipleTryLock_onlyOneSucceeds();
    }

    @Test
    @DisplayName("CT lock and unlock should have no race condition")
    void concurrent_lockAndUnlock_noRaceCondition() throws Exception {
        verifier.concurrent_lockAndUnlock_noRaceCondition();
    }

    @Test
    @DisplayName("CT tryLock() after lock expired should allow only one thread")
    void concurrent_lockExpiredAndRetry_onlyOneGetsLock() throws Exception {
        verifier.concurrent_lockExpiredAndRetry_onlyOneGetsLock();
    }

    @Test
    @DisplayName("CT high contention should behave correctly")
    void concurrent_highContention_correctBehavior() throws Exception {
        verifier.concurrent_highContention_correctBehavior();
    }

    @Test
    @DisplayName("CT unlock by different workers should only allow owner to unlock")
    void concurrent_unlockByDifferentWorkers_onlyOwnerUnlocks() throws Exception {
        verifier.concurrent_unlockByDifferentWorkers_onlyOwnerUnlocks();
    }

    @Test
    @DisplayName("CT locking multiple jobs should maintain independent locks")
    void concurrent_multipleJobsLocking_independentLocks() throws Exception {
        verifier.concurrent_multipleJobsLocking_independentLocks();
    }
}