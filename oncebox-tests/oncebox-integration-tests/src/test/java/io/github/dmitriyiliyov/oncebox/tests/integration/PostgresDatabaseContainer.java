package io.github.dmitriyiliyov.oncebox.tests.integration;

import io.github.dmitriyiliyov.oncebox.tests.fixtures.PostgresTestContainerSingleton;
import org.testcontainers.containers.PostgreSQLContainer;

public class PostgresDatabaseContainer implements DatabaseContainer {

    private final PostgreSQLContainer<?> container = PostgresTestContainerSingleton.INSTANCE;

    @Override
    public DatabaseType getDatabaseType() {
        return DatabaseType.POSTGRES_SQL;
    }

    @Override
    public void start() {
        container.start();
    }

    @Override
    public void stop() {
        container.stop();
    }

    @Override
    public String getJdbcUrl() {
        return container.getJdbcUrl();
    }

    @Override
    public String getUsername() {
        return container.getUsername();
    }

    @Override
    public String getPassword() {
        return container.getPassword();
    }
}
