package io.github.dmitriyiliyov.oncebox.mysql;

import io.github.dmitriyiliyov.oncebox.core.locks.DistributedLockRepository;
import io.github.dmitriyiliyov.oncebox.core.utils.BytesSqlUuidHelper;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Objects;
import java.util.UUID;

public class MySqlDistributedLockRepository implements DistributedLockRepository {

    private final JdbcTemplate jdbcTemplate;
    private final BytesSqlUuidHelper uuidHelper;

    public MySqlDistributedLockRepository(JdbcTemplate jdbcTemplate, BytesSqlUuidHelper uuidHelper) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate cannot be null");
        this.uuidHelper = Objects.requireNonNull(uuidHelper, "uuidHelper cannot be null");
    }

    @Override
    public boolean tryLock(String jobName, UUID workerId) {
        String sql = """
            UPDATE outbox_jobs
            SET lock_until = TIMESTAMPADD(MICROSECOND, lock_at_most_for * 1000, UTC_TIMESTAMP(3)), 
                locked_by = ?,
                locked_at = UTC_TIMESTAMP(3)
            WHERE job_name = ? AND lock_until <= UTC_TIMESTAMP(3)
        """;
        return jdbcTemplate.update(
                sql,
                ps -> {
                    uuidHelper.setToPs(ps, 1, workerId);
                    ps.setString(2, jobName);
                }
        ) == 1;
    }

    @Override
    public void unlock(String jobName, UUID workerId) {
        String sql = """
            UPDATE outbox_jobs
            SET lock_until = GREATEST(
                UTC_TIMESTAMP(3), TIMESTAMPADD(MICROSECOND, lock_at_least_for * 1000, locked_at)
            )
            WHERE job_name = ? AND locked_by = ?
        """;
        jdbcTemplate.update(
                sql,
                ps -> {
                    ps.setString(1, jobName);
                    uuidHelper.setToPs(ps, 2, workerId);
                }
        );
    }
}