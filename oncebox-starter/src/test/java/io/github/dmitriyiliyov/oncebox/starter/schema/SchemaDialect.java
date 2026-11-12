package io.github.dmitriyiliyov.oncebox.starter.schema;

import io.github.dmitriyiliyov.oncebox.starter.MySqlOnceboxRepositoryFactory;
import io.github.dmitriyiliyov.oncebox.starter.OnceboxRepositoryFactory;
import io.github.dmitriyiliyov.oncebox.starter.OracleOnceboxRepositoryFactory;
import io.github.dmitriyiliyov.oncebox.starter.PostgreSqlOnceboxRepositoryFactory;
import io.github.dmitriyiliyov.oncebox.tests.fixtures.MySqlTestContainerSingleton;
import io.github.dmitriyiliyov.oncebox.tests.fixtures.OracleTestContainerSingleton;
import io.github.dmitriyiliyov.oncebox.tests.fixtures.PostgresTestContainerSingleton;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.JdbcDatabaseContainer;

import javax.sql.DataSource;
import java.util.List;

// One supported database: its container, its oncebox repositories and the table scripts of the previous release
enum SchemaDialect {

    POSTGRESQL("postgresql", "org.postgresql.Driver", ";") {
        @Override
        JdbcDatabaseContainer<?> container() {
            return PostgresTestContainerSingleton.INSTANCE;
        }

        @Override
        OnceboxRepositoryFactory repositoryFactory(JdbcTemplate jdbcTemplate) {
            return new PostgreSqlOnceboxRepositoryFactory(jdbcTemplate);
        }

        @Override
        List<String> previousReleaseScripts() {
            return List.of(
                    RELEASE_2_0_0 + "psql/psql_outbox_table.sql",
                    RELEASE_2_0_0 + "psql/psql_outbox_dlq_table.sql",
                    RELEASE_2_0_0 + "psql/psql_inbox_table.sql",
                    RELEASE_2_0_0 + "psql/psql_oncebox_jobs_table.sql"
            );
        }
    },
    MYSQL("mysql", "com.mysql.cj.jdbc.Driver", ";") {
        @Override
        JdbcDatabaseContainer<?> container() {
            return MySqlTestContainerSingleton.INSTANCE;
        }

        @Override
        OnceboxRepositoryFactory repositoryFactory(JdbcTemplate jdbcTemplate) {
            return new MySqlOnceboxRepositoryFactory(jdbcTemplate);
        }

        @Override
        List<String> previousReleaseScripts() {
            return List.of(
                    RELEASE_2_0_0 + "mysql/mysql_outbox_table.sql",
                    RELEASE_2_0_0 + "mysql/mysql_outbox_dlq_table.sql",
                    RELEASE_2_0_0 + "mysql/mysql_inbox_table.sql",
                    RELEASE_2_0_0 + "mysql/mysql_oncebox_jobs_table.sql"
            );
        }
    },
    ORACLE("oracle", "oracle.jdbc.OracleDriver", "/") {
        @Override
        JdbcDatabaseContainer<?> container() {
            return OracleTestContainerSingleton.INSTANCE;
        }

        @Override
        OnceboxRepositoryFactory repositoryFactory(JdbcTemplate jdbcTemplate) {
            return new OracleOnceboxRepositoryFactory(jdbcTemplate);
        }

        @Override
        List<String> previousReleaseScripts() {
            return List.of(
                    RELEASE_2_0_0 + "oracle/oracle_outbox_table.sql",
                    RELEASE_2_0_0 + "oracle/oracle_outbox_dlq_table.sql",
                    RELEASE_2_0_0 + "oracle/oracle_inbox_table.sql",
                    RELEASE_2_0_0 + "oracle/oracle_oncebox_jobs_table.sql"
            );
        }
    };

    // the table scripts as released in v2.0.0, copied so that a change to the shared fixtures does not move them
    static final String RELEASE_2_0_0 = "schema/release-2.0.0/";

    private final String vendor;
    private final String driver;
    private final String scriptSeparator;

    SchemaDialect(String vendor, String driver, String scriptSeparator) {
        this.vendor = vendor;
        this.driver = driver;
        this.scriptSeparator = scriptSeparator;
    }

    // the container is reached only here, so a run of one dialect starts only its own container
    abstract JdbcDatabaseContainer<?> container();

    abstract OnceboxRepositoryFactory repositoryFactory(JdbcTemplate jdbcTemplate);

    abstract List<String> previousReleaseScripts();

    String vendor() {
        return vendor;
    }

    String driver() {
        return driver;
    }

    String scriptSeparator() {
        return scriptSeparator;
    }

    DataSource dataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                container().getJdbcUrl(),
                container().getUsername(),
                container().getPassword()
        );
        dataSource.setDriverClassName(driver);
        return dataSource;
    }

    String[] datasourceProperties() {
        return new String[] {
                "spring.datasource.url=" + container().getJdbcUrl(),
                "spring.datasource.driver-class-name=" + driver,
                "spring.datasource.username=" + container().getUsername(),
                "spring.datasource.password=" + container().getPassword()
        };
    }
}
