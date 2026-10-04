package io.github.dmitriyiliyov.oncebox.tests.e2e.repository;

import io.github.dmitriyiliyov.oncebox.tests.e2e.config.DatabaseType;
import org.springframework.jdbc.core.JdbcTemplate;

public final class TestOnceboxRepositoryFactory {

    private TestOnceboxRepositoryFactory() {}

    public static TestOutboxRepository generate(DatabaseType databaseType, JdbcTemplate jdbcTemplate) {
        return switch (databaseType) {
            case POSTGRES_SQL -> new PostgresTestOutboxRepository(jdbcTemplate);
            case MY_SQL -> new MySqlTestOutboxRepository(jdbcTemplate);
            case ORACLE -> new OracleTestOutboxRepository(jdbcTemplate);
        };
    }
}
