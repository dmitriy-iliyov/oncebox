package io.github.dmitriyiliyov.oncebox.tests.fixtures;

import org.testcontainers.containers.MySQLContainer;

public class MySqlTestContainerSingleton {

    public static final MySQLContainer<?> INSTANCE;

    static {
        INSTANCE = new MySQLContainer<>("mysql:8")
                .withDatabaseName("testdb")
                .withUsername("test")
                .withPassword("test");
        INSTANCE.start();
    }

    private MySqlTestContainerSingleton() {}
}
