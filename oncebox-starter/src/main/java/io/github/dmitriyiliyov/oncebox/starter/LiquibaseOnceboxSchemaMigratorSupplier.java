package io.github.dmitriyiliyov.oncebox.starter;

import io.github.dmitriyiliyov.oncebox.core.OnceboxSchemaMigrator;
import io.github.dmitriyiliyov.oncebox.liquibase.LiquibaseOnceboxSchemaMigrator;
import org.springframework.core.io.ResourceLoader;

import javax.sql.DataSource;
import java.util.Objects;

public class LiquibaseOnceboxSchemaMigratorSupplier implements OnceboxSchemaMigratorSupplier {

    private final DataSource dataSource;
    private final ResourceLoader resourceLoader;

    public LiquibaseOnceboxSchemaMigratorSupplier(DataSource dataSource, ResourceLoader resourceLoader) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource cannot be null");
        this.resourceLoader = Objects.requireNonNull(resourceLoader, "resourceLoader cannot be null");
    }

    @Override
    public OnceboxSchemaMigrator supply(DatabaseType databaseType) {
        return new LiquibaseOnceboxSchemaMigrator(dataSource, resourceLoader, databaseType.getVendorName());
    }

    @Override
    public OnceboxProperties.SchemaProperties.Mode getMode() {
        return OnceboxProperties.SchemaProperties.Mode.LIQUIBASE;
    }
}
