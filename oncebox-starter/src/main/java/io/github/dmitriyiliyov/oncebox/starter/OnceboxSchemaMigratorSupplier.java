package io.github.dmitriyiliyov.oncebox.starter;

import io.github.dmitriyiliyov.oncebox.core.OnceboxSchemaMigrator;

/**
 * Creates the {@link OnceboxSchemaMigrator} of one schema mode. Oncebox picks the supplier whose mode matches
 * {@code oncebox.schema.mode} and fails at startup if there is none, so a mode whose migration tool is missing
 * from the classpath is reported rather than skipped.
 * <p>
 * A built-in supplier gives way to an application bean of the same name - {@code devOnceboxSchemaMigratorSupplier},
 * {@code flywayOnceboxSchemaMigratorSupplier} or {@code liquibaseOnceboxSchemaMigratorSupplier}. To replace the migration altogether, declare a bean named
 * {@code onceboxSchemaMigrator} instead.
 */
public interface OnceboxSchemaMigratorSupplier {
    OnceboxSchemaMigrator supply(DatabaseType databaseType);
    OnceboxProperties.SchemaProperties.Mode getMode();
}
