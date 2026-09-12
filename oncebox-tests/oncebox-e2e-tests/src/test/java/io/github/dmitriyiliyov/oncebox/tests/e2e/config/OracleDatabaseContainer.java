package io.github.dmitriyiliyov.oncebox.tests.e2e.config;

import io.github.dmitriyiliyov.oncebox.tests.fixtures.OracleTestContainerSingleton;
import org.testcontainers.oracle.OracleContainer;

public class OracleDatabaseContainer implements DatabaseContainer {

    private final OracleContainer container = OracleTestContainerSingleton.INSTANCE;

    @Override
    public DatabaseType getDatabaseType() {
        return DatabaseType.ORACLE;
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
