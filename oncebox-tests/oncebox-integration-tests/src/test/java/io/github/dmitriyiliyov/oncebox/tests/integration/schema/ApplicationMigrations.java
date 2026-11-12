package io.github.dmitriyiliyov.oncebox.tests.integration.schema;

import java.util.List;

// how the user application keeps its own schema next to oncebox
public enum ApplicationMigrations {

    FLYWAY(
            "flyway_schema_history",
            List.of(
                    "spring.flyway.locations=classpath:schema/app/flyway",
                    "spring.flyway.baseline-on-migrate=false",
                    "spring.liquibase.enabled=false"
            )
    ),
    LIQUIBASE(
            "databasechangelog",
            List.of(
                    "spring.flyway.enabled=false",
                    "spring.liquibase.change-log=classpath:schema/app/liquibase/changelog.yaml"
            )
    ),
    NONE(
            null,
            List.of(
                    "spring.flyway.enabled=false",
                    "spring.liquibase.enabled=false"
            )
    );

    private final String historyTable;
    private final List<String> properties;

    ApplicationMigrations(String historyTable, List<String> properties) {
        this.historyTable = historyTable;
        this.properties = properties;
    }

    public boolean createsApplicationTable() {
        return historyTable != null;
    }

    public String historyTable() {
        return historyTable;
    }

    public List<String> properties() {
        return properties;
    }
}
