package io.github.dmitriyiliyov.oncebox.starter;

import io.github.dmitriyiliyov.oncebox.core.OnceboxSchemaMigrator;
import io.github.dmitriyiliyov.oncebox.flyway.FlywayOnceboxSchemaMigrator;

import javax.sql.DataSource;
import java.util.Objects;

public class FlywayOnceboxSchemaMigratorSupplier implements OnceboxSchemaMigratorSupplier {

    private final DataSource dataSource;

    public FlywayOnceboxSchemaMigratorSupplier(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource cannot be null");
    }

    @Override
    public OnceboxSchemaMigrator supply(DatabaseType databaseType) {
        return new FlywayOnceboxSchemaMigrator(dataSource, databaseType.getVendorName());
    }

    @Override
    public OnceboxProperties.SchemaProperties.Mode getMode() {
        return OnceboxProperties.SchemaProperties.Mode.FLYWAY;
    }
}
