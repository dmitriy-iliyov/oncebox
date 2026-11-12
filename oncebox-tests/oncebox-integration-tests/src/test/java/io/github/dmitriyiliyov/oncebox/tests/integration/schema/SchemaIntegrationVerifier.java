package io.github.dmitriyiliyov.oncebox.tests.integration.schema;

import io.github.dmitriyiliyov.oncebox.core.inbox.InboxRepository;
import io.github.dmitriyiliyov.oncebox.tests.integration.DatabaseContainer;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

// Starts a real application, keeping its own schema the way ApplicationMigrations says, next to oncebox, as many
// times as a scenario needs, on the one database of the cell
public class SchemaIntegrationVerifier {

    private static final List<String> ONCEBOX_PROPERTIES = List.of(
            "oncebox.thread-pool-size=2",
            "oncebox.outbox.enabled=true",
            "oncebox.outbox.sender.type=kafka",
            "oncebox.outbox.events.schema-it-event.topic=schema.it",
            "oncebox.outbox.dlq.enabled=true",
            "oncebox.outbox.clean-up.enabled=true",
            "oncebox.inbox.enabled=true",
            "oncebox.inbox.source.type=kafka",
            "oncebox.inbox.mappings.schema-it-event=java.lang.String",
            "oncebox.inbox.cache.enabled=false",
            "oncebox.inbox.clean-up.enabled=true"
    );
    private static final List<String> ONCEBOX_DISABLED_PROPERTIES = List.of(
            "oncebox.outbox.enabled=false",
            "oncebox.inbox.enabled=false"
    );

    private final DatabaseContainer container;
    private final ApplicationMigrations migrations;
    private final SchemaDatabase database;

    public SchemaIntegrationVerifier(DatabaseContainer container, ApplicationMigrations migrations) {
        this.container = Objects.requireNonNull(container, "container cannot be null");
        this.migrations = Objects.requireNonNull(migrations, "migrations cannot be null");
        this.database = new SchemaDatabase(container);
    }

    public void cleanUp() {
        database.dropTestTables();
    }

    public void startup_whenDatabaseIsEmpty_shouldCreateApplicationAndOnceboxTables(OnceboxSchemaMode mode) {
        // when
        try (ConfigurableApplicationContext context = startWithOncebox(mode)) {

            // then
            assertApplicationSchema();
            assertOnceboxSchema(mode);
            assertThat(context.getBean(InboxRepository.class).saveIfAbsent(UUID.randomUUID()))
                    .as("oncebox writes to the table it has just created")
                    .isEqualTo(1);
        }
    }

    public void restart_shouldStartAgainAndKeepRowsAndHistory(OnceboxSchemaMode mode) {
        // given
        UUID id = UUID.randomUUID();
        Map<String, Integer> histories;
        try (ConfigurableApplicationContext context = startWithOncebox(mode)) {
            context.getBean(InboxRepository.class).saveIfAbsent(id);
            histories = database.histories();
        }

        // when
        try (ConfigurableApplicationContext context = startWithOncebox(mode)) {

            // then
            assertThat(database.histories())
                    .as("a second start applies nothing new")
                    .isEqualTo(histories);
            assertThat(context.getBean(InboxRepository.class).saveIfAbsent(id))
                    .as("the row written before the restart is still there")
                    .isZero();
        }
    }

    public void restart_whenModeChanges_shouldStartAndKeepRows(OnceboxSchemaMode from, OnceboxSchemaMode to) {
        // given
        UUID id = UUID.randomUUID();
        try (ConfigurableApplicationContext context = startWithOncebox(from)) {
            context.getBean(InboxRepository.class).saveIfAbsent(id);
        }

        // when
        try (ConfigurableApplicationContext context = startWithOncebox(to)) {

            // then
            assertApplicationSchema();
            assertOnceboxSchema(to);
            assertThat(context.getBean(InboxRepository.class).saveIfAbsent(id))
                    .as("the row written under %s is still there under %s", from, to)
                    .isZero();
        }
    }

    public void startup_whenApplicationAlreadyRanWithoutOncebox_shouldAddOnceboxTablesAndKeepApplicationHistory(
            OnceboxSchemaMode mode
    ) {
        // given
        try (ConfigurableApplicationContext ignored = start(ONCEBOX_DISABLED_PROPERTIES)) {
            assertThat(onceboxTablesPresent()).as("oncebox switched off creates nothing").isEmpty();
        }
        Map<String, Integer> applicationHistory = applicationHistory();

        // when
        try (ConfigurableApplicationContext ignored = startWithOncebox(mode)) {

            // then
            assertOnceboxSchema(mode);
            assertThat(applicationHistory())
                    .as("oncebox leaves the history of the application alone")
                    .isEqualTo(applicationHistory);
        }
    }

    public void startup_whenTablesComeFromAutoCreateOfRelease200_shouldMigrateAndKeepRows(OnceboxSchemaMode mode) {
        // given
        try (ConfigurableApplicationContext ignored = start(ONCEBOX_DISABLED_PROPERTIES)) {
            // the application had its own schema before oncebox 2.0.0 created its tables next to it
        }
        database.createRelease200Tables();
        database.insertInboxRecord(UUID.randomUUID());

        // when
        try (ConfigurableApplicationContext ignored = startWithOncebox(mode)) {

            // then
            assertApplicationSchema();
            assertOnceboxSchema(mode);
            assertThat(database.rowCount("inbox_events"))
                    .as("the row recorded under 2.0.0 is still there")
                    .isEqualTo(1);
        }
    }

    private void assertApplicationSchema() {
        assertThat(database.tableExists(SchemaDatabase.APP_TABLE))
                .as("the application table exists exactly when the application migrates")
                .isEqualTo(migrations.createsApplicationTable());
        if (migrations.createsApplicationTable()) {
            assertThat(database.rowCount(migrations.historyTable()))
                    .as("the application history holds its one migration")
                    .isEqualTo(1);
        }
    }

    private void assertOnceboxSchema(OnceboxSchemaMode mode) {
        assertThat(onceboxTablesPresent())
                .as("every oncebox table of an enabled feature exists")
                .containsExactlyInAnyOrderElementsOf(SchemaDatabase.ONCEBOX_TABLES);
        for (String feature : SchemaDatabase.FEATURES) {
            if (mode.keepsHistory()) {
                assertThat(database.tableExists(mode.historyTable(feature)))
                        .as("%s keeps the history of %s in its own table", mode, feature)
                        .isTrue();
                assertThat(database.rowCount(mode.historyTable(feature)))
                        .as("%s recorded what it applied to %s", mode, feature)
                        .isPositive();
            }
        }
    }

    private List<String> onceboxTablesPresent() {
        return SchemaDatabase.ONCEBOX_TABLES.stream().filter(database::tableExists).toList();
    }

    private Map<String, Integer> applicationHistory() {
        return database.histories().entrySet().stream()
                .filter(entry -> !entry.getKey().startsWith("oncebox_"))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private ConfigurableApplicationContext startWithOncebox(OnceboxSchemaMode mode) {
        List<String> properties = new ArrayList<>(ONCEBOX_PROPERTIES);
        properties.add("oncebox.schema.mode=" + mode.name());
        return start(properties);
    }

    private ConfigurableApplicationContext start(List<String> onceboxProperties) {
        List<String> properties = new ArrayList<>(List.of(
                "spring.datasource.url=" + container.getJdbcUrl(),
                "spring.datasource.username=" + container.getUsername(),
                "spring.datasource.password=" + container.getPassword(),
                "spring.datasource.driver-class-name=" + container.getDatabaseType().getDriverClassName(),
                "spring.jpa.hibernate.ddl-auto=none"
        ));
        properties.addAll(migrations.properties());
        properties.addAll(onceboxProperties);
        return new SpringApplicationBuilder(SchemaIntegrationTestApplication.class)
                .web(WebApplicationType.NONE)
                .properties(properties.toArray(String[]::new))
                .run();
    }
}
