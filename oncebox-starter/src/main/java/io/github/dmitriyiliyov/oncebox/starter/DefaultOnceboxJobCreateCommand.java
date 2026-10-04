package io.github.dmitriyiliyov.oncebox.starter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.temporal.ChronoUnit;

public class DefaultOnceboxJobCreateCommand implements OnceboxJobCreateCommand {

    private static final Logger log = LoggerFactory.getLogger(DefaultOnceboxJobCreateCommand.class);

    private final JdbcTemplate jdbcTemplate;
    private final Clock clock;
    private final String jobName;
    private final Long lockAtLeastFor;
    private final Long lockAtMostFor;

    public DefaultOnceboxJobCreateCommand(JdbcTemplate jdbcTemplate, Clock clock, String jobName, Long lockAtLeastFor, Long lockAtMostFor) {
        this.jdbcTemplate = jdbcTemplate;
        this.clock = clock;
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
            Timestamp pastTmstmpStub = Timestamp.from(clock.instant().minus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MILLIS));
            jdbcTemplate.update(
                    sql,
                    ps -> {
                        ps.setString(1, jobName);
                        ps.setTimestamp(2, pastTmstmpStub);
                        ps.setTimestamp(3, pastTmstmpStub);
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
