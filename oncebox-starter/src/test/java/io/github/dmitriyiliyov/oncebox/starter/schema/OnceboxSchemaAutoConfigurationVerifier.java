package io.github.dmitriyiliyov.oncebox.starter.schema;

import io.github.dmitriyiliyov.oncebox.core.outbox.EventStatus;
import io.github.dmitriyiliyov.oncebox.core.outbox.OutboxEvent;
import io.github.dmitriyiliyov.oncebox.core.outbox.OutboxRepository;
import io.github.dmitriyiliyov.oncebox.starter.*;
import liquibase.integration.spring.SpringLiquibase;
import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

// oncebox.schema.mode wired by OnceboxAutoConfiguration over a real database, where the migration actually runs
public class OnceboxSchemaAutoConfigurationVerifier {

    private static final UUID PROBE_EVENT_ID = UUID.fromString("cccccccc-0000-0000-0000-000000000001");

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

    private final SchemaDialect dialect;
    private final SchemaDatabase database;

    public OnceboxSchemaAutoConfigurationVerifier(SchemaDialect dialect) {
        this.dialect = Objects.requireNonNull(dialect, "dialect cannot be null");
        this.database = new SchemaDatabase(dialect);
    }

    public void cleanUp() {
        database.dropTestTables();
    }

    public void context_whenModeIsFlyway_shouldCreateTablesThroughFlyway() {
        // when / then
        contextRunner()
                .withPropertyValues(ALL_FEATURES)
                .withPropertyValues("oncebox.schema.mode=FLYWAY")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertEveryOnceboxTableExists();
                    assertThat(database.tableExists("oncebox_outbox_schema_history")).isTrue();
                    assertThat(database.tableExists("oncebox_outbox_changelog")).isFalse();
                });
    }

    public void context_whenModeIsLiquibase_shouldCreateTablesThroughLiquibase() {
        // when / then
        contextRunner()
                .withPropertyValues(ALL_FEATURES)
                .withPropertyValues("oncebox.schema.mode=LIQUIBASE")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertEveryOnceboxTableExists();
                    assertThat(database.tableExists("oncebox_outbox_changelog")).isTrue();
                    assertThat(database.tableExists("oncebox_outbox_schema_history")).isFalse();
                });
    }

    public void context_whenModeIsDev_shouldCreateTablesWithoutHistory() {
        // when / then
        contextRunner()
                .withPropertyValues(ALL_FEATURES)
                .withPropertyValues("oncebox.schema.mode=DEV")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertEveryOnceboxTableExists();
                    assertThat(database.tableExists("oncebox_outbox_schema_history")).isFalse();
                    assertThat(database.tableExists("oncebox_outbox_changelog")).isFalse();
                });
    }

    public void context_whenModeIsNone_shouldCreateNoTables() {
        // when / then
        contextRunner()
                .withPropertyValues(ALL_FEATURES)
                .withPropertyValues("oncebox.schema.mode=NONE")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertNoOnceboxTableExists();
                });
    }

    public void context_whenModeIsAbsent_shouldCreateNoTables() {
        // when / then
        contextRunner()
                .withPropertyValues(ALL_FEATURES)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertNoOnceboxTableExists();
                });
    }

    public void context_whenOutboxIsDisabled_shouldCreateNoOutboxTables() {
        // when / then
        contextRunner()
                .withPropertyValues(
                        "oncebox.schema.mode=FLYWAY",
                        "oncebox.outbox.enabled=false",
                        "oncebox.inbox.enabled=true",
                        "oncebox.inbox.source.type=kafka"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(database.tableExists("inbox_events")).isTrue();
                    assertThat(database.tableExists("outbox_events")).isFalse();
                    assertThat(database.tableExists("outbox_dlq_events")).isFalse();
                });
    }

    public void context_whenFlywayAndLiquibaseAreOnClasspath_shouldRegisterTheirSuppliers() {
        // when / then
        contextRunner()
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(supplierTypes(context.getBeansOfType(OnceboxSchemaMigratorSupplier.class).values()))
                            .containsExactlyInAnyOrder(
                                    DevOnceboxSchemaMigratorSupplier.class,
                                    FlywayOnceboxSchemaMigratorSupplier.class,
                                    LiquibaseOnceboxSchemaMigratorSupplier.class
                            );
                });
    }

    public void context_whenFlywayIsNotOnClasspath_shouldNotRegisterFlywaySupplier() {
        // when / then
        contextRunner()
                .withClassLoader(new FilteredClassLoader(Flyway.class))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(supplierTypes(context.getBeansOfType(OnceboxSchemaMigratorSupplier.class).values()))
                            .containsExactlyInAnyOrder(
                                    DevOnceboxSchemaMigratorSupplier.class,
                                    LiquibaseOnceboxSchemaMigratorSupplier.class
                            );
                });
    }

    public void context_whenLiquibaseIsNotOnClasspath_shouldNotRegisterLiquibaseSupplier() {
        // when / then
        contextRunner()
                .withClassLoader(new FilteredClassLoader(SpringLiquibase.class))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(supplierTypes(context.getBeansOfType(OnceboxSchemaMigratorSupplier.class).values()))
                            .containsExactlyInAnyOrder(
                                    DevOnceboxSchemaMigratorSupplier.class,
                                    FlywayOnceboxSchemaMigratorSupplier.class
                            );
                });
    }

    public void context_whenModeIsFlywayAndFlywayIsNotOnClasspath_shouldFailNamingTheMode() {
        // when / then
        contextRunner()
                .withClassLoader(new FilteredClassLoader(Flyway.class))
                .withPropertyValues(ALL_FEATURES)
                .withPropertyValues("oncebox.schema.mode=FLYWAY")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).rootCause().hasMessageContaining("FLYWAY");
                    assertNoOnceboxTableExists();
                });
    }

    public void context_whenUserDefinesOnceboxSchemaMigrator_shouldUseItInsteadOfDefault() {
        // given
        AtomicBoolean userMigrationRan = new AtomicBoolean();
        OnceboxSchemaMigrationInitializer userMigrator = new OnceboxSchemaMigrationInitializer(
                () -> userMigrationRan.set(true)
        );

        // when / then
        contextRunner()
                .withPropertyValues(ALL_FEATURES)
                .withPropertyValues("oncebox.schema.mode=FLYWAY")
                .withBean("onceboxSchemaMigrator", OnceboxSchemaMigrationInitializer.class, () -> userMigrator)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).getBean(OnceboxSchemaMigrationInitializer.class).isSameAs(userMigrator);
                    assertThat(userMigrationRan).isTrue();
                    assertNoOnceboxTableExists();
                });
    }

    public void context_whenBeanUsesOnceboxRepositoryWhileInitialising_shouldFindTablesAlreadyMigrated() {
        // when / then
        contextRunner()
                .withPropertyValues(ALL_FEATURES)
                .withPropertyValues("oncebox.schema.mode=FLYWAY")
                // registered ahead of the auto-configuration: only the declared dependency puts the migration first
                .withBean(OutboxRepositoryProbe.class)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(database.rowCount("outbox_events")).isEqualTo(1);
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
                .withPropertyValues(dialect.datasourceProperties());
    }

    private void assertEveryOnceboxTableExists() {
        assertThat(SchemaDatabase.ONCEBOX_TABLES).allSatisfy(table ->
                assertThat(database.tableExists(table)).as(table).isTrue());
    }

    private void assertNoOnceboxTableExists() {
        assertThat(SchemaDatabase.ONCEBOX_TABLES).allSatisfy(table ->
                assertThat(database.tableExists(table)).as(table).isFalse());
    }

    private static List<Class<?>> supplierTypes(Collection<OnceboxSchemaMigratorSupplier> suppliers) {
        return suppliers.stream().<Class<?>>map(Object::getClass).toList();
    }

    // writes to the outbox table the moment it is created, as any oncebox bean using its repository would
    static final class OutboxRepositoryProbe {

        OutboxRepositoryProbe(OnceboxRepositoryFactory repositoryFactory) {
            OutboxRepository outbox = repositoryFactory.createOutboxRepository();
            outbox.save(new OutboxEvent(PROBE_EVENT_ID, "schema-it", "io.example.OrderCreated", "{}"));
            outbox.findAndLockBatchByStatus(EventStatus.PENDING, 1, EventStatus.PENDING);
        }
    }
}
