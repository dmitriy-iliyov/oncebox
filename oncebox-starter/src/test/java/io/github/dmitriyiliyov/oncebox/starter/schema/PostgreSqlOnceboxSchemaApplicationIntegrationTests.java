package io.github.dmitriyiliyov.oncebox.starter.schema;

import io.github.dmitriyiliyov.oncebox.starter.schema.app.SchemaMigrationTestApplication;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// A real Spring Boot application running its own Flyway (baseline-on-migrate off) next to oncebox.schema.mode=FLYWAY:
// the application's Flyway refuses a non-empty schema without its history table, so it has to run before oncebox
class PostgreSqlOnceboxSchemaApplicationIntegrationTests {

    private static final String[] APPLICATION_PROPERTIES = {
            "spring.flyway.locations=classpath:schema/app-migrations",
            "spring.flyway.baseline-on-migrate=false",
            "spring.liquibase.enabled=false",
            "oncebox.schema.mode=FLYWAY",
            "oncebox.outbox.enabled=true",
            "oncebox.outbox.sender.type=kafka",
            "oncebox.outbox.events.schema-it-event.topic=schema.it"
    };

    private final SchemaDatabase database = new SchemaDatabase(SchemaDialect.POSTGRESQL);

    @BeforeEach
    void setUp() {
        database.dropTestTables();
    }

    @AfterEach
    void tearDown() {
        database.dropTestTables();
    }

    @Test
    @DisplayName("IT startup() when database is empty should run the app Flyway before oncebox creates its tables")
    void startup_whenDatabaseIsEmpty_shouldRunApplicationFlywayBeforeOnceboxCreatesItsTables() {
        // when
        try (ConfigurableApplicationContext ignored = start()) {

            // then
            assertThat(database.tableExists(SchemaDatabase.APP_TABLE)).isTrue();
            assertThat(database.jdbcTemplate().queryForObject(
                    "SELECT COUNT(*) FROM flyway_schema_history WHERE version = '1' AND success",
                    Integer.class
            )).isEqualTo(1);
            assertThat(database.tableExists("outbox_events")).isTrue();
            assertThat(database.tableExists("oncebox_outbox_schema_history")).isTrue();
        }
    }

    @Test
    @DisplayName("IT startup() should make oncebox migrate after the application Flyway and before onceboxJdbcTemplate")
    void startup_shouldOrderOnceboxMigrationAfterApplicationFlywayAndBeforeOnceboxJdbcTemplate() {
        // when
        try (ConfigurableApplicationContext context = start()) {

            // then
            // Boot sets these dependsOn only for an initializer whose detector is registered in spring.factories
            ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
            assertThat(beanFactory.getBeanDefinition("onceboxSchemaMigrator").getDependsOn())
                    .contains("flywayInitializer");
            assertThat(beanFactory.getBeanDefinition("onceboxJdbcTemplate").getDependsOn())
                    .contains("onceboxSchemaMigrator");
        }
    }

    @Test
    @DisplayName("IT startup() when oncebox tables precede the app Flyway should fail on the non-empty schema")
    void startup_whenOnceboxTablesExistBeforeApplicationFlyway_shouldFailOnNonEmptySchema() {
        // given
        database.runScript(SchemaDialect.RELEASE_2_0_0 + "psql/psql_outbox_table.sql");

        // when / then
        // a start that unexpectedly succeeds is closed at once, so its schedulers do not outlive the test's tables
        assertThatThrownBy(() -> {
            try (ConfigurableApplicationContext ignored = start()) {
                // nothing to do: reaching this point already fails the assertion
            }
        })
                .rootCause()
                .hasMessageContaining("non-empty schema");
    }

    private ConfigurableApplicationContext start() {
        return new SpringApplicationBuilder(SchemaMigrationTestApplication.class)
                .web(WebApplicationType.NONE)
                .properties(Stream.concat(
                        Stream.of(SchemaDialect.POSTGRESQL.datasourceProperties()),
                        Stream.of(APPLICATION_PROPERTIES)
                ).toArray(String[]::new))
                .run();
    }
}
