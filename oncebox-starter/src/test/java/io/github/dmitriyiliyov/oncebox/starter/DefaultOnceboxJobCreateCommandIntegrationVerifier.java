package io.github.dmitriyiliyov.oncebox.starter;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

// Built per test instance, so the pool it opens is closed by the test class after every test
public class DefaultOnceboxJobCreateCommandIntegrationVerifier implements AutoCloseable {

    private final HikariDataSource dataSource;
    private final JdbcTemplate jdbcTemplate;
    private final DefaultOnceboxJobCreateCommand command;

    public DefaultOnceboxJobCreateCommandIntegrationVerifier(String dbUrl, String dbDriver, String dbUsername, String dbPassword) {
        HikariConfig config = new HikariConfig();
        config.setDriverClassName(dbDriver);
        config.setJdbcUrl(dbUrl);
        config.setUsername(dbUsername);
        config.setPassword(dbPassword);
        config.setMinimumIdle(1);
        config.setMaximumPoolSize(2);
        this.dataSource = new HikariDataSource(config);
        this.jdbcTemplate = new JdbcTemplate(dataSource);

        this.command = new DefaultOnceboxJobCreateCommand(this.jdbcTemplate, "integration_job", 1000L, 5000L);
    }

    public void setUpSchema() {
        try {
            jdbcTemplate.execute("DROP TABLE oncebox_jobs");
        } catch (Exception ignored) {}
        jdbcTemplate.execute("""
            CREATE TABLE oncebox_jobs (
                job_name VARCHAR(255) PRIMARY KEY,
                lock_until TIMESTAMP,
                locked_by VARCHAR(255),
                locked_at TIMESTAMP NOT NULL,
                lock_at_least_for NUMERIC(19, 0),
                lock_at_most_for NUMERIC(19, 0)
            )
        """);
    }

    public void shouldInsertNewJob() {
        command.create();

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM oncebox_jobs WHERE job_name = 'integration_job'",
                Integer.class
        );

        assertThat(count).isEqualTo(1);
    }

    public void shouldNotThrowExceptionWhenJobAlreadyExists() {
        command.create();
        command.create();

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM oncebox_jobs WHERE job_name = 'integration_job'",
                Integer.class
        );

        assertThat(count).isEqualTo(1);
    }

    @Override
    public void close() {
        dataSource.close();
    }
}
