package io.github.dmitriyiliyov.oncebox.starter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Instant;

public class DefaultOnceboxJobCreateCommand implements OnceboxJobCreateCommand {

    private static final Logger log = LoggerFactory.getLogger(DefaultOnceboxJobCreateCommand.class);

    // any moment in the past leaves a new job unlocked; a fixed one depends on no clock and no time zone
    static final Timestamp EXPIRED_LOCK = Timestamp.from(Instant.parse("2000-01-01T00:00:00Z"));

    private final JdbcTemplate jdbcTemplate;
    private final String jobName;
    private final Long lockAtLeastFor;
    private final Long lockAtMostFor;

    public DefaultOnceboxJobCreateCommand(JdbcTemplate jdbcTemplate, String jobName, Long lockAtLeastFor, Long lockAtMostFor) {
        this.jdbcTemplate = jdbcTemplate;
        this.jobName = jobName;
        this.lockAtLeastFor = lockAtLeastFor;
        this.lockAtMostFor = lockAtMostFor;
    }

    @Override
    public void create() {
        String sql = """
                INSERT INTO oncebox_jobs (job_name, lock_until, locked_by, locked_at, lock_at_least_for, lock_at_most_for)
                VALUES (?, ?, NULL, ?, ?, ?)
        """;
        try {
            jdbcTemplate.update(
                    sql,
                    ps -> {
                        ps.setString(1, jobName);
                        ps.setTimestamp(2, EXPIRED_LOCK);
                        ps.setTimestamp(3, EXPIRED_LOCK);
                        ps.setLong(4, lockAtLeastFor);
                        ps.setLong(5, lockAtMostFor);
                    }
            );
            log.info("Initialized job with name '{}'", jobName);
        } catch (DuplicateKeyException dke) {
            log.info("Job with name '{}' already exists", jobName);
        }
    }
}
