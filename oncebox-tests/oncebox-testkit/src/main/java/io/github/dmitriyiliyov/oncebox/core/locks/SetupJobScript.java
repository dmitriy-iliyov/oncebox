package io.github.dmitriyiliyov.oncebox.core.locks;

import org.springframework.jdbc.core.JdbcTemplate;

public interface SetupJobScript {
    void setup(JdbcTemplate jdbcTemplate, DistributedLockRepositoryVerifier.IdPreparer idPreparer, String jobName, Long lockAtLeastFor, Long lockAtMostFor);
}
