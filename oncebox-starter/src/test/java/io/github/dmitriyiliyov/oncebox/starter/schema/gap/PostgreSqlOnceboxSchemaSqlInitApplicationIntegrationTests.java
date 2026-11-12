package io.github.dmitriyiliyov.oncebox.starter.schema.gap;

import io.github.dmitriyiliyov.oncebox.starter.schema.gap.app.GapSqlInitTestApplication;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

// A real Spring Boot application whose spring.sql.init data script writes into outbox_events: it can run only once
// oncebox has created the table, so the detector has to order oncebox before the script initializer
class PostgreSqlOnceboxSchemaSqlInitApplicationIntegrationTests {

    private static final UUID SCRIPT_EVENT_ID = UUID.fromString("dddddddd-0000-0000-0000-000000000001");
    private static final String[] APPLICATION_PROPERTIES = {
            "spring.flyway.enabled=false",
            "spring.liquibase.enabled=false",
            "spring.sql.init.mode=always",
            "spring.sql.init.data-locations=classpath:schema/gap/sql-init/data.sql",
            "oncebox.schema.mode=FLYWAY",
            "oncebox.outbox.enabled=true",
            "oncebox.outbox.sender.type=kafka",
            "oncebox.outbox.events.schema-it-event.topic=schema.it"
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
    @DisplayName("IT startup() when a spring.sql.init script writes to outbox_events should run it after oncebox")
    void startup_whenSqlInitScriptWritesToOutboxEvents_shouldRunItAfterOnceboxMigration() {
        // when
        try (ConfigurableApplicationContext ignored = start()) {

            // then
            assertThat(database.jdbcTemplate().queryForObject(
                    "SELECT COUNT(*) FROM outbox_events WHERE id = ?",
                    Integer.class,
                    SCRIPT_EVENT_ID
            )).isEqualTo(1);
            assertThat(database.tableExists("oncebox_outbox_schema_history")).isTrue();
        }
    }

    private ConfigurableApplicationContext start() {
        return new SpringApplicationBuilder(GapSqlInitTestApplication.class)
                .web(WebApplicationType.NONE)
                .properties(Stream.concat(
                        Stream.of(GapDialect.POSTGRESQL.datasourceProperties()),
                        Stream.of(APPLICATION_PROPERTIES)
                ).toArray(String[]::new))
                .run();
    }
}
