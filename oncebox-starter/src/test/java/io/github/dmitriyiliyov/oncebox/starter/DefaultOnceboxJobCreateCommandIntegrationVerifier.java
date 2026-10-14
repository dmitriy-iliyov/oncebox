package io.github.dmitriyiliyov.oncebox.starter;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import static org.assertj.core.api.Assertions.assertThat;

public class DefaultOnceboxJobCreateCommandIntegrationVerifier {

    private final JdbcTemplate jdbcTemplate;
    private final DefaultOnceboxJobCreateCommand command;

    public DefaultOnceboxJobCreateCommandIntegrationVerifier(String dbUrl, String dbDriver, String dbUsername, String dbPassword) {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName(dbDriver);
        dataSource.setUrl(dbUrl);
        dataSource.setUsername(dbUsername);
        dataSource.setPassword(dbPassword);
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
}
