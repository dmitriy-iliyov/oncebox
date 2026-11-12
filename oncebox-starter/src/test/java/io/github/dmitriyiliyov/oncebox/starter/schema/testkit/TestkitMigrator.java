package io.github.dmitriyiliyov.oncebox.starter.schema.testkit;

import io.github.dmitriyiliyov.oncebox.core.OnceboxSchemaMigrator;
import io.github.dmitriyiliyov.oncebox.flyway.FlywayOnceboxSchemaMigrator;
import io.github.dmitriyiliyov.oncebox.liquibase.LiquibaseOnceboxSchemaMigrator;
import io.github.dmitriyiliyov.oncebox.starter.DevOnceboxSchemaMigrator;
import org.springframework.core.io.DefaultResourceLoader;

import javax.sql.DataSource;

// the three migrators a schema mode selects, built the way their suppliers build them
enum TestkitMigrator {

    FLYWAY {
        @Override
        OnceboxSchemaMigrator create(DataSource dataSource, String vendor) {
            return new FlywayOnceboxSchemaMigrator(dataSource, vendor);
        }
    },
    LIQUIBASE {
        @Override
        OnceboxSchemaMigrator create(DataSource dataSource, String vendor) {
            return new LiquibaseOnceboxSchemaMigrator(dataSource, new DefaultResourceLoader(), vendor);
        }
    },
    DEV {
        @Override
        OnceboxSchemaMigrator create(DataSource dataSource, String vendor) {
            return new DevOnceboxSchemaMigrator(dataSource, vendor);
        }
    };

    abstract OnceboxSchemaMigrator create(DataSource dataSource, String vendor);
}
