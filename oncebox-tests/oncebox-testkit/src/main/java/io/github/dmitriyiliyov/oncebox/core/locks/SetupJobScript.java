package io.github.dmitriyiliyov.oncebox.core.locks;

import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Inserts the row of a job into {@code oncebox_jobs} in the dialect under test, before a lock is taken on it.
 */
public interface SetupJobScript {
    void setup(JdbcTemplate jdbcTemplate, DistributedLockRepositoryVerifier.IdPreparer idPreparer, String jobName, Long lockAtLeastFor, Long lockAtMostFor);
}
