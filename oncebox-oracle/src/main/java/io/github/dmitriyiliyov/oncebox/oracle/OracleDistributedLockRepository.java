package io.github.dmitriyiliyov.oncebox.oracle;

import io.github.dmitriyiliyov.oncebox.core.locks.DistributedLockRepository;
import io.github.dmitriyiliyov.oncebox.core.utils.BytesSqlUuidHelper;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Objects;
import java.util.UUID;

public class OracleDistributedLockRepository implements DistributedLockRepository {

    private final JdbcTemplate jdbcTemplate;
    private final BytesSqlUuidHelper uuidHelper;

    public OracleDistributedLockRepository(JdbcTemplate jdbcTemplate, BytesSqlUuidHelper uuidHelper) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate cannot be null");
        this.uuidHelper = Objects.requireNonNull(uuidHelper, "uuidHelper cannot be null");
    }

    @Override
    public boolean tryLock(String jobName, UUID workerId) {
        String sql = """
            UPDATE outbox_jobs
            SET lock_until = SYS_EXTRACT_UTC(SYSTIMESTAMP) + NUMTODSINTERVAL(lock_at_most_for / 1000, 'SECOND'), 
                locked_by = ?,
                locked_at = SYS_EXTRACT_UTC(SYSTIMESTAMP)
            WHERE job_name = ? AND lock_until <= SYS_EXTRACT_UTC(SYSTIMESTAMP)
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
                SYS_EXTRACT_UTC(SYSTIMESTAMP),
                locked_at + NUMTODSINTERVAL(lock_at_least_for / 1000, 'SECOND')
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
