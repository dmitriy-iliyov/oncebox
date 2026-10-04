package io.github.dmitriyiliyov.oncebox.starter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.dmitriyiliyov.oncebox.core.locks.OnceboxJob;
import io.github.dmitriyiliyov.oncebox.starter.inbox.InboxAutoConfiguration;
import io.github.dmitriyiliyov.oncebox.starter.outbox.OutboxAutoConfiguration;
import io.github.dmitriyiliyov.oncebox.starter.outbox.dlq.OutboxDlqAutoConfiguration;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class OnceboxJobsInitializerIntegrationVerifier {

    private final String dbUrl;
    private final String dbDriver;
    private final String dbUsername;
    private final String dbPassword;

    public OnceboxJobsInitializerIntegrationVerifier(String dbUrl, String dbDriver, String dbUsername, String dbPassword) {
        this.dbUrl = dbUrl;
        this.dbDriver = dbDriver;
        this.dbUsername = dbUsername;
        this.dbPassword = dbPassword;
    }

    private ApplicationContextRunner getBaseContextRunner() {
        return new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        DataSourceAutoConfiguration.class,
                        DataSourceTransactionManagerAutoConfiguration.class,
                        TransactionAutoConfiguration.class,
                        KafkaAutoConfiguration.class,
                        OnceboxAutoConfiguration.class,
                        OutboxAutoConfiguration.class,
                        OutboxDlqAutoConfiguration.class,
                        InboxAutoConfiguration.class
                ))
                .withBean(ObjectMapper.class, ObjectMapper::new)
                .withBean(Clock.class, Clock::systemDefaultZone)
                .withPropertyValues(
                        "spring.datasource.url=" + dbUrl,
                        "spring.datasource.driver-class-name=" + dbDriver,
                        "spring.datasource.username=" + dbUsername,
                        "spring.datasource.password=" + dbPassword,
                        "oncebox.tables.auto-create=true",
                        "oncebox.outbox.sender.type=kafka",
                        "oncebox.inbox.mappings.test-event=io.github.dmitriyiliyov.oncebox.starter.inbox.TestEvent",
                        "oncebox.outbox.events.my-event.topic=my.topic"
                );
    }

    public void shouldExecuteCommandsAndSaveToDatabase() {
        getBaseContextRunner()
                .run(context -> {
                    assertThat(context).hasSingleBean(ApplicationRunner.class);

                    ApplicationRunner runner = context.getBean("onceboxJobsInitializer", ApplicationRunner.class);

                    runner.run(mock(ApplicationArguments.class));

                    JdbcTemplate jdbcTemplate = context.getBean("onceboxJdbcTemplate", JdbcTemplate.class);
                    Integer count = jdbcTemplate.queryForObject(
                            "SELECT COUNT(*) FROM oncebox_jobs WHERE job_name = '%s'".formatted(OnceboxJob.OUTBOX_PROCESSED_CLEANUP.getJobName()),
                            Integer.class
                    );
                    jdbcTemplate.execute("DROP TABLE oncebox_jobs");
                });
    }

    public void shouldExecuteCommandsAndSaveToDatabase_whenDlqEnabled() {
        getBaseContextRunner()
                .withPropertyValues("oncebox.outbox.dlq.enabled=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(ApplicationRunner.class);

                    ApplicationRunner runner = context.getBean("onceboxJobsInitializer", ApplicationRunner.class);

                    runner.run(mock(ApplicationArguments.class));

                    JdbcTemplate jdbcTemplate = context.getBean("onceboxJdbcTemplate", JdbcTemplate.class);
                    Integer count = jdbcTemplate.queryForObject(
                            "SELECT COUNT(*) FROM oncebox_jobs WHERE job_name = '%s'".formatted(OnceboxJob.OUTBOX_DLQ_CLEANUP.getJobName()),
                            Integer.class
                    );
                    assertThat(count).isEqualTo(1);
                });
    }

    public void shouldExecuteCommandsAndSaveToDatabase_whenInboxEnabled() {
        getBaseContextRunner()
                .withPropertyValues("oncebox.inbox.enabled=true", "oncebox.inbox.source.type=kafka", "oncebox.inbox.clean-up.enabled=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(ApplicationRunner.class);

                    ApplicationRunner runner = context.getBean("onceboxJobsInitializer", ApplicationRunner.class);

                    runner.run(mock(ApplicationArguments.class));

                    JdbcTemplate jdbcTemplate = context.getBean("onceboxJdbcTemplate", JdbcTemplate.class);
                    Integer count = jdbcTemplate.queryForObject(
                            "SELECT COUNT(*) FROM oncebox_jobs WHERE job_name = '%s'".formatted(OnceboxJob.INBOX_CLEANUP.getJobName()),
                            Integer.class
                    );
                    assertThat(count).isEqualTo(1);
                });
    }
}