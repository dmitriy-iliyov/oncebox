package io.github.dmitriyiliyov.oncebox.core;

/**
 * Creates or upgrades the tables of one oncebox feature in the application's database.
 */
public interface OnceboxSchemaMigrator {

    /**
     * Brings the tables of the feature up to date. Running it again on a database that is already up to date
     * changes nothing.
     *
     * @param feature the feature whose tables are migrated: {@code outbox}, {@code dlq}, {@code inbox} or
     *                {@code jobs}
     * @throws OnceboxMigrationException if the migration fails; the tool's own exception is its cause
     */
    void migrate(String feature);

    /**
     * A migrator that leaves the schema alone, for applications that create the tables themselves.
     */
    OnceboxSchemaMigrator NOOP = (feature) -> {};
}
