package io.github.dmitriyiliyov.oncebox.starter.schema.gap;

import io.github.dmitriyiliyov.oncebox.core.OnceboxSchemaMigrator;
import io.github.dmitriyiliyov.oncebox.flyway.FlywayOnceboxSchemaMigrator;
import io.github.dmitriyiliyov.oncebox.liquibase.LiquibaseOnceboxSchemaMigrator;
import io.github.dmitriyiliyov.oncebox.starter.*;
import io.github.dmitriyiliyov.oncebox.starter.OnceboxProperties.SchemaProperties.Mode;
import liquibase.integration.spring.SpringLiquibase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.context.properties.bind.BindException;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.assertj.core.api.Assertions.assertThat;

// oncebox.schema.mode binding and the replaceable migrator suppliers; none of it depends on the database, so it runs
// on PostgreSQL only
class PostgreSqlOnceboxSchemaModeAutoConfigurationIntegrationTests {

    // outbox with DLQ and clean-up, and inbox: every feature migrates
    private static final String[] ALL_FEATURES = {
            "oncebox.outbox.enabled=true",
            "oncebox.outbox.sender.type=kafka",
            "oncebox.outbox.events.schema-it-event.topic=schema.it",
            "oncebox.outbox.dlq.enabled=true",
            "oncebox.outbox.clean-up.enabled=true",
            "oncebox.inbox.enabled=true",
            "oncebox.inbox.source.type=kafka"
    };

    private final GapDatabase database = new GapDatabase(GapDialect.POSTGRESQL);

    @BeforeEach
    void setUp() {
        database.dropTestTables();
    }

    @AfterEach
    void tearDown() {
        database.dropTestTables();
    }

    @Test
    @DisplayName("IT context() when mode is not a known value should fail naming 'oncebox.schema.mode'")
    void context_whenModeIsUnknown_shouldFailNamingTheProperty() {
        contextRunner()
                .withPropertyValues(ALL_FEATURES)
                .withPropertyValues("oncebox.schema.mode=foo")
                .run(context -> {
                    // then
                    assertThat(context).hasFailed();
                    BindException failure = bindException(context.getStartupFailure());
                    assertThat(failure.getName()).hasToString("oncebox.schema.mode");
                    assertThat(failure).hasMessageContaining("'oncebox.schema.mode'");
                    assertNoOnceboxTableExists();
                });
    }

    @Test
    @DisplayName("IT context() when mode is spelled in lower case should bind it and migrate through Flyway")
    void context_whenModeIsLowerCase_shouldBindItAndMigrate() {
        contextRunner()
                .withPropertyValues(ALL_FEATURES)
                .withPropertyValues("oncebox.schema.mode=flyway")
                .run(context -> {
                    // then
                    assertThat(context).hasNotFailed();
                    assertThat(GapDatabase.ONCEBOX_TABLES).allSatisfy(table ->
                            assertThat(database.tableExists(table)).as(table).isTrue());
                    assertThat(database.tableExists("oncebox_outbox_schema_history")).isTrue();
                });
    }

    @Test
    @DisplayName("IT context() when mode is LIQUIBASE without Liquibase on the classpath should fail naming the mode")
    void context_whenModeIsLiquibaseAndLiquibaseIsNotOnClasspath_shouldFailNamingTheMode() {
        contextRunner()
                .withClassLoader(new FilteredClassLoader(SpringLiquibase.class))
                .withPropertyValues(ALL_FEATURES)
                .withPropertyValues("oncebox.schema.mode=LIQUIBASE")
                .run(context -> {
                    // then
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .rootCause()
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessageContaining("LIQUIBASE");
                    assertNoOnceboxTableExists();
                });
    }

    @Test
    @DisplayName("IT context() when mode is FLYWAY with Flyway but without oncebox-flyway should fail naming the mode")
    void context_whenModeIsFlywayAndOnceboxFlywayIsNotOnClasspath_shouldFailNamingTheMode() {
        contextRunner()
                .withClassLoader(new FilteredClassLoader(FlywayOnceboxSchemaMigrator.class))
                .withPropertyValues(ALL_FEATURES)
                .withPropertyValues("oncebox.schema.mode=FLYWAY")
                .run(context -> {
                    // then
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .rootCause()
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessageContaining("FLYWAY");
                    assertNoOnceboxTableExists();
                });
    }

    @Test
    @DisplayName("IT context() when mode is LIQUIBASE with Liquibase but without oncebox-liquibase should fail naming"
            + " the mode")
    void context_whenModeIsLiquibaseAndOnceboxLiquibaseIsNotOnClasspath_shouldFailNamingTheMode() {
        contextRunner()
                .withClassLoader(new FilteredClassLoader(LiquibaseOnceboxSchemaMigrator.class))
                .withPropertyValues(ALL_FEATURES)
                .withPropertyValues("oncebox.schema.mode=LIQUIBASE")
                .run(context -> {
                    // then
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .rootCause()
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessageContaining("LIQUIBASE");
                    assertNoOnceboxTableExists();
                });
    }

    @Test
    @DisplayName("IT context() when mode is FLYWAY with Liquibase but without oncebox-liquibase should migrate through"
            + " Flyway")
    void context_whenModeIsFlywayAndOnceboxLiquibaseIsNotOnClasspath_shouldMigrateThroughFlyway() {
        contextRunner()
                .withClassLoader(new FilteredClassLoader(LiquibaseOnceboxSchemaMigrator.class))
                .withPropertyValues(ALL_FEATURES)
                .withPropertyValues("oncebox.schema.mode=FLYWAY")
                .run(context -> {
                    // then
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean("liquibaseOnceboxSchemaMigratorSupplier");
                    assertThat(GapDatabase.ONCEBOX_TABLES).allSatisfy(table ->
                            assertThat(database.tableExists(table)).as(table).isTrue());
                    assertThat(database.tableExists("oncebox_outbox_schema_history")).isTrue();
                });
    }

    @Test
    @DisplayName("IT context() when mode is LIQUIBASE with Flyway but without oncebox-flyway should migrate through"
            + " Liquibase")
    void context_whenModeIsLiquibaseAndOnceboxFlywayIsNotOnClasspath_shouldMigrateThroughLiquibase() {
        contextRunner()
                .withClassLoader(new FilteredClassLoader(FlywayOnceboxSchemaMigrator.class))
                .withPropertyValues(ALL_FEATURES)
                .withPropertyValues("oncebox.schema.mode=LIQUIBASE")
                .run(context -> {
                    // then
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean("flywayOnceboxSchemaMigratorSupplier");
                    assertThat(GapDatabase.ONCEBOX_TABLES).allSatisfy(table ->
                            assertThat(database.tableExists(table)).as(table).isTrue());
                    assertThat(database.tableExists("oncebox_outbox_changelog")).isTrue();
                });
    }

    @Test
    @DisplayName("IT context() when application declares flywayOnceboxSchemaMigratorSupplier should use it instead")
    void context_whenApplicationDeclaresFlywaySupplier_shouldUseItInsteadOfBuiltIn() {
        assertApplicationSupplierDisplacesBuiltIn(
                "flywayOnceboxSchemaMigratorSupplier",
                Mode.FLYWAY,
                FlywayOnceboxSchemaMigratorSupplier.class
        );
    }

    @Test
    @DisplayName("IT context() when application declares liquibaseOnceboxSchemaMigratorSupplier should use it instead")
    void context_whenApplicationDeclaresLiquibaseSupplier_shouldUseItInsteadOfBuiltIn() {
        assertApplicationSupplierDisplacesBuiltIn(
                "liquibaseOnceboxSchemaMigratorSupplier",
                Mode.LIQUIBASE,
                LiquibaseOnceboxSchemaMigratorSupplier.class
        );
    }

    @Test
    @DisplayName("IT context() when application declares devOnceboxSchemaMigratorSupplier should use it instead")
    void context_whenApplicationDeclaresDevSupplier_shouldUseItInsteadOfBuiltIn() {
        assertApplicationSupplierDisplacesBuiltIn(
                "devOnceboxSchemaMigratorSupplier",
                Mode.DEV,
                DevOnceboxSchemaMigratorSupplier.class
        );
    }

    private void assertApplicationSupplierDisplacesBuiltIn(String beanName,
                                                           Mode mode,
                                                           Class<? extends OnceboxSchemaMigratorSupplier> builtIn) {
        // given
        RecordingOnceboxSchemaMigratorSupplier applicationSupplier = new RecordingOnceboxSchemaMigratorSupplier(mode);

        contextRunner()
                .withPropertyValues(ALL_FEATURES)
                .withPropertyValues("oncebox.schema.mode=" + mode.name())
                .withBean(beanName, OnceboxSchemaMigratorSupplier.class, () -> applicationSupplier)
                .run(context -> {
                    // then
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(beanName)).isSameAs(applicationSupplier);
                    assertThat(context).doesNotHaveBean(builtIn);
                    assertThat(applicationSupplier.migrated())
                            .containsExactlyInAnyOrder("outbox", "dlq", "inbox", "jobs");
                    assertNoOnceboxTableExists();
                });
    }

    private ApplicationContextRunner contextRunner() {
        return new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        DataSourceAutoConfiguration.class,
                        JdbcTemplateAutoConfiguration.class,
                        DataSourceTransactionManagerAutoConfiguration.class,
                        TransactionAutoConfiguration.class,
                        OnceboxAutoConfiguration.class
                ))
                .withPropertyValues(GapDialect.POSTGRESQL.datasourceProperties());
    }

    private void assertNoOnceboxTableExists() {
        assertThat(GapDatabase.ONCEBOX_TABLES).allSatisfy(table ->
                assertThat(database.tableExists(table)).as(table).isFalse());
    }

    private static BindException bindException(Throwable failure) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause instanceof BindException bindException) {
                return bindException;
            }
        }
        throw new AssertionError("no BindException in the startup failure", failure);
    }

    // an application's own supplier of one mode: its migrator only records the features it is asked to migrate
    static final class RecordingOnceboxSchemaMigratorSupplier implements OnceboxSchemaMigratorSupplier {

        private final Mode mode;
        private final Queue<String> migrated = new ConcurrentLinkedQueue<>();

        RecordingOnceboxSchemaMigratorSupplier(Mode mode) {
            this.mode = mode;
        }

        @Override
        public OnceboxSchemaMigrator supply(DatabaseType databaseType) {
            return migrated::add;
        }

        @Override
        public Mode getMode() {
            return mode;
        }

        Queue<String> migrated() {
            return migrated;
        }
    }
}
