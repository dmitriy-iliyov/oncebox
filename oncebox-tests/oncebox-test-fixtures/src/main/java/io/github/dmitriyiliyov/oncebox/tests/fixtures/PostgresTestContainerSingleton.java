package io.github.dmitriyiliyov.oncebox.tests.fixtures;

import org.testcontainers.containers.PostgreSQLContainer;

public class PostgresTestContainerSingleton {

    public static final PostgreSQLContainer<?> INSTANCE;

    static {
        INSTANCE = new PostgreSQLContainer<>("postgres:18")
                .withDatabaseName("testdb")
                .withUsername("test")
                .withPassword("test");
        INSTANCE.start();
    }

    private PostgresTestContainerSingleton() {}
}