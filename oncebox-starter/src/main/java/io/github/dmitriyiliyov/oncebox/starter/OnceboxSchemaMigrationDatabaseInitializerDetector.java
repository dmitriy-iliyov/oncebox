package io.github.dmitriyiliyov.oncebox.starter;

import org.springframework.boot.sql.init.dependency.AbstractBeansOfTypeDatabaseInitializerDetector;

import java.util.Set;

public class OnceboxSchemaMigrationDatabaseInitializerDetector extends AbstractBeansOfTypeDatabaseInitializerDetector {

    @Override
    protected Set<Class<?>> getDatabaseInitializerBeanTypes() {
        return Set.of(OnceboxSchemaMigrationInitializer.class);
    }

    /**
     * After the application's own Flyway and Liquibase (orders 0 and 1), so that their check for a non-empty
     * schema without a history table does not trip over the oncebox tables, and before the
     * {@code spring.sql.init} scripts, which may refer to them.
     */
    @Override
    public int getOrder() {
        return 2;
    }
}
