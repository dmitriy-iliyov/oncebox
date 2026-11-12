package io.github.dmitriyiliyov.oncebox.starter.schema.gap;

import io.github.dmitriyiliyov.oncebox.tests.fixtures.MySqlTestContainerSingleton;
import io.github.dmitriyiliyov.oncebox.tests.fixtures.OracleTestContainerSingleton;
import io.github.dmitriyiliyov.oncebox.tests.fixtures.PostgresTestContainerSingleton;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.JdbcDatabaseContainer;

import javax.sql.DataSource;
import java.util.List;

// One supported database for the gap tests; starter.schema.SchemaDialect is package-private, hence this copy
enum GapDialect {

    POSTGRESQL("postgresql", "org.postgresql.Driver", ";") {
        @Override
        JdbcDatabaseContainer<?> container() {
            return PostgresTestContainerSingleton.INSTANCE;
        }

        @Override
        List<String> repositoryTestScripts() {
            return List.of(
                    "psql/psql_outbox_table.sql",
                    "psql/psql_outbox_dlq_table.sql",
                    "psql/psql_inbox_table.sql",
                    "psql/psql_oncebox_jobs_table.sql"
            );
        }
    },
    MYSQL("mysql", "com.mysql.cj.jdbc.Driver", ";") {
        @Override
        JdbcDatabaseContainer<?> container() {
            return MySqlTestContainerSingleton.INSTANCE;
        }

        @Override
        List<String> repositoryTestScripts() {
            return List.of(
                    "mysql/mysql_outbox_table.sql",
                    "mysql/mysql_outbox_dlq_table.sql",
                    "mysql/mysql_inbox_table.sql",
                    "mysql/mysql_oncebox_jobs_table.sql"
            );
        }
    },
    ORACLE("oracle", "oracle.jdbc.OracleDriver", "/") {
        @Override
        JdbcDatabaseContainer<?> container() {
            return OracleTestContainerSingleton.INSTANCE;
        }

        @Override
        List<String> repositoryTestScripts() {
            return List.of(
                    "oracle/oracle_outbox_table.sql",
                    "oracle/oracle_outbox_dlq_table.sql",
                    "oracle/oracle_inbox_table.sql",
                    "oracle/oracle_oncebox_jobs_table.sql"
            );
        }
    };

    private final String vendor;
    private final String driver;
    private final String scriptSeparator;

    GapDialect(String vendor, String driver, String scriptSeparator) {
        this.vendor = vendor;
        this.driver = driver;
        this.scriptSeparator = scriptSeparator;
    }

    // the container is reached only here, so a run of one dialect starts only its own container
    abstract JdbcDatabaseContainer<?> container();

    // the scripts of oncebox-test-fixtures the testkit repository verifiers run on in the dialect modules
    abstract List<String> repositoryTestScripts();

    String vendor() {
        return vendor;
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
