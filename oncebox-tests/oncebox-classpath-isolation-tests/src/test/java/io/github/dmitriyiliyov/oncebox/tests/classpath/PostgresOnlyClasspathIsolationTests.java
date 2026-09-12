package io.github.dmitriyiliyov.oncebox.tests.classpath;

import io.github.dmitriyiliyov.oncebox.dlq.api.OutboxDlqApiRepository;
import io.github.dmitriyiliyov.oncebox.postgresql.PostgreSqlOutboxDlqApiRepository;
import io.github.dmitriyiliyov.oncebox.starter.OutboxAutoConfiguration;
import io.github.dmitriyiliyov.oncebox.starter.OutboxRepositoryFactory;
import io.github.dmitriyiliyov.oncebox.starter.PostgreSqlOutboxRepositoryFactory;
import io.github.dmitriyiliyov.oncebox.starter.publisher.dlq.OutboxDlqApiAutoConfiguration;
import io.github.dmitriyiliyov.oncebox.tests.fixtures.PostgresTestContainerSingleton;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.time.Clock;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Characterises what happens when a consumer puts ONLY the PostgreSQL dialect module on its
 * classpath (mysql / oracle modules absent).
 * <p>
 * The core outbox auto-configuration boots fine, because it delegates dialect construction to
 * starter-local factory classes ({@code PostgreSqlOutboxRepositoryFactory} etc.) — introspecting
 * {@code OutboxAutoConfiguration} never resolves the dialect module classes themselves.
 * <p>
 * {@code OutboxDlqApiAutoConfiguration}, on the other hand, references the dialect id-helper /
 * repository classes directly in its bean-method bodies, so Spring's configuration-class
 * introspection ({@code Class.getDeclaredMethods}) forces those classes to be resolved and the
 * context fails to start when the corresponding dialect module is missing.
 */
class PostgresOnlyClasspathIsolationTests {

    private static final String POSTGRES_OUTBOX_REPO = "io.github.dmitriyiliyov.oncebox.postgresql.PostgreSqlOutboxRepository";

    private static final List<String> ABSENT_DIALECT_CLASSES = List.of(
            "io.github.dmitriyiliyov.oncebox.mysql.MySqlOutboxRepository",
            "io.github.dmitriyiliyov.oncebox.mysql.MySqlOutboxDlqApiRepository",
            "io.github.dmitriyiliyov.oncebox.oracle.OracleOutboxRepository",
            "io.github.dmitriyiliyov.oncebox.oracle.OracleOutboxDlqApiRepository"
    );

    private ApplicationContextRunner baseRunner() {
        return new ApplicationContextRunner()
                .withBean(Clock.class, Clock::systemDefaultZone)
                .withPropertyValues(
                        "spring.datasource.url=" + PostgresTestContainerSingleton.INSTANCE.getJdbcUrl(),
                        "spring.datasource.driver-class-name=org.postgresql.Driver",
                        "spring.datasource.username=" + PostgresTestContainerSingleton.INSTANCE.getUsername(),
                        "spring.datasource.password=" + PostgresTestContainerSingleton.INSTANCE.getPassword(),
                        "oncebox.tables.auto-create=false"
                );
    }

    @Test
    @DisplayName("only the postgresql dialect module is on the classpath; mysql and oracle modules are absent")
    void onlyPostgresDialectModuleIsOnClasspath() {
        ClassLoader classLoader = getClass().getClassLoader();

        assertThatCode(() -> Class.forName(POSTGRES_OUTBOX_REPO, false, classLoader))
                .as("postgresql dialect module must be present")
                .doesNotThrowAnyException();

        for (String absent : ABSENT_DIALECT_CLASSES) {
            assertThatThrownBy(() -> Class.forName(absent, false, classLoader))
                    .as("class %s must NOT be on the classpath", absent)
                    .isInstanceOf(ClassNotFoundException.class);
        }
    }

    @Test
    @DisplayName("core outbox context boots against Postgres even though mysql/oracle dialect modules are absent")
    void coreOutboxContextBootsWithPostgresOnly() {
        baseRunner()
                .withConfiguration(AutoConfigurations.of(
                        DataSourceAutoConfiguration.class,
                        DataSourceTransactionManagerAutoConfiguration.class,
                        TransactionAutoConfiguration.class,
                        OutboxAutoConfiguration.class
                ))
                .run(ctx -> {
                    assertThat(ctx).hasNotFailed();
                    assertThat(ctx).hasSingleBean(OutboxRepositoryFactory.class);
                    assertThat(ctx.getBean(OutboxRepositoryFactory.class))
                            .isInstanceOf(PostgreSqlOutboxRepositoryFactory.class);
                });
    }

    @Test
    @DisplayName("DLQ API context boots against Postgres even though mysql/oracle dialect modules are absent")
    void dlqApiContextBootsWithPostgresOnly() {
        baseRunner()
                .withConfiguration(AutoConfigurations.of(
                        DataSourceAutoConfiguration.class,
                        DataSourceTransactionManagerAutoConfiguration.class,
                        TransactionAutoConfiguration.class,
                        OutboxAutoConfiguration.class,
                        OutboxDlqApiAutoConfiguration.class
                ))
                .withPropertyValues(
                        "oncebox.publisher.dlq.enabled=true",
                        "oncebox.publisher.sender.type=kafka",
                        "oncebox.publisher.events.my-event.topic=my.topic"
                )
                .run(ctx -> {
                    assertThat(ctx).hasNotFailed();
                    assertThat(ctx).hasSingleBean(OutboxDlqApiRepository.class);
                    assertThat(ctx.getBean(OutboxDlqApiRepository.class))
                            .isInstanceOf(PostgreSqlOutboxDlqApiRepository.class);
                });
    }
}
