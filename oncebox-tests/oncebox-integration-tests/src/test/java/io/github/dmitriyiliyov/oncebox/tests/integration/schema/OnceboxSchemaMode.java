package io.github.dmitriyiliyov.oncebox.tests.integration.schema;

// the values of 'oncebox.schema.mode' that create tables, with the history each one leaves per feature
public enum OnceboxSchemaMode {

    FLYWAY("_schema_history"),
    LIQUIBASE("_changelog"),
    DEV(null);

    private final String historySuffix;

    OnceboxSchemaMode(String historySuffix) {
        this.historySuffix = historySuffix;
    }

    public boolean keepsHistory() {
        return historySuffix != null;
    }

    public String historyTable(String feature) {
        return "oncebox_" + feature + historySuffix;
    }
}
