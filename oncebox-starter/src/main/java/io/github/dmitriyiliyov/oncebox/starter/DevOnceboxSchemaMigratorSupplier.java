package io.github.dmitriyiliyov.oncebox.starter;

import io.github.dmitriyiliyov.oncebox.core.OnceboxSchemaMigrator;

import javax.sql.DataSource;
import java.util.Objects;

public class DevOnceboxSchemaMigratorSupplier implements OnceboxSchemaMigratorSupplier {

    private final DataSource dataSource;

    public DevOnceboxSchemaMigratorSupplier(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource cannot be null");
    }

    @Override
    public OnceboxSchemaMigrator supply(DatabaseType databaseType) {
        return new DevOnceboxSchemaMigrator(dataSource, databaseType.getVendorName());
    }

    @Override
    public OnceboxProperties.SchemaProperties.Mode getMode() {
        return OnceboxProperties.SchemaProperties.Mode.DEV;
    }
}
