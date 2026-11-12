package io.github.dmitriyiliyov.oncebox.starter.schema.testkit;

import io.github.dmitriyiliyov.oncebox.core.MySqlDatabaseClock;
import io.github.dmitriyiliyov.oncebox.core.OracleDatabaseClock;
import io.github.dmitriyiliyov.oncebox.core.PostgreSqlDatabaseClock;
import io.github.dmitriyiliyov.oncebox.core.TestDatabaseClock;
import io.github.dmitriyiliyov.oncebox.core.locks.DistributedLockRepositoryVerifier.IdExtractor;
import io.github.dmitriyiliyov.oncebox.core.locks.DistributedLockRepositoryVerifier.IdPreparer;
import io.github.dmitriyiliyov.oncebox.core.locks.SetupJobScript;
import io.github.dmitriyiliyov.oncebox.core.utils.DefaultBytesResultSetMapper;
import io.github.dmitriyiliyov.oncebox.core.utils.DefaultResultSetMapper;
import io.github.dmitriyiliyov.oncebox.core.utils.ResultSetMapper;
import io.github.dmitriyiliyov.oncebox.core.utils.SqlUuidHelper;
import io.github.dmitriyiliyov.oncebox.mysql.MySqlResultSetMapper;
import io.github.dmitriyiliyov.oncebox.mysql.MySqlUuidHelper;
import io.github.dmitriyiliyov.oncebox.oracle.OracleSqlUuidHelper;
import io.github.dmitriyiliyov.oncebox.postgresql.PostgreSqlUuidHelper;
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
import java.nio.ByteBuffer;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

// One supported database as its dialect module's repository ITs wire the testkit verifiers: the same uuid helper,
// result set mapper, clock, id conversions and oncebox_jobs row; the repositories come from the starter's factory
enum TestkitDialect {

    POSTGRESQL("postgresql", "org.postgresql.Driver") {
        @Override
        JdbcDatabaseContainer<?> container() {
            return PostgresTestContainerSingleton.INSTANCE;
        }

        @Override
        OnceboxRepositoryFactory repositoryFactory(JdbcTemplate jdbcTemplate) {
            return new PostgreSqlOnceboxRepositoryFactory(jdbcTemplate);
        }

        @Override
        TestDatabaseClock clock(JdbcTemplate jdbcTemplate) {
            return new PostgreSqlDatabaseClock(jdbcTemplate);
        }

        @Override
        SqlUuidHelper uuidHelper() {
            return new PostgreSqlUuidHelper();
        }

        @Override
        ResultSetMapper resultSetMapper() {
            return new DefaultResultSetMapper();
        }

        @Override
        IdExtractor idExtractor() {
            return raw -> (UUID) raw;
        }

        @Override
        IdPreparer idPreparer() {
            return id -> id;
        }

        @Override
        SetupJobScript setupJobScript() {
            return (jdbcTemplate, idPreparer, jobName, lockAtLeastFor, lockAtMostFor) -> {
                Timestamp longAgo = Timestamp.from(Instant.now().minus(1, ChronoUnit.HOURS));
                jdbcTemplate.update(
                        INSERT_JOB.formatted("?", "?"),
                        jobName,
                        longAgo,
                        idPreparer.prepare(UUID.randomUUID()),
                        longAgo,
                        lockAtLeastFor,
                        lockAtMostFor
                );
            };
        }
    },
    MYSQL("mysql", "com.mysql.cj.jdbc.Driver") {
        @Override
        JdbcDatabaseContainer<?> container() {
            return MySqlTestContainerSingleton.INSTANCE;
        }

        @Override
        OnceboxRepositoryFactory repositoryFactory(JdbcTemplate jdbcTemplate) {
            return new MySqlOnceboxRepositoryFactory(jdbcTemplate);
        }

        @Override
        TestDatabaseClock clock(JdbcTemplate jdbcTemplate) {
            return new MySqlDatabaseClock(jdbcTemplate);
        }

        @Override
        SqlUuidHelper uuidHelper() {
            return new MySqlUuidHelper();
        }

        @Override
        ResultSetMapper resultSetMapper() {
            return new MySqlResultSetMapper();
        }

        @Override
        SetupJobScript setupJobScript() {
            return (jdbcTemplate, idPreparer, jobName, lockAtLeastFor, lockAtMostFor) -> {
                String moment = "TIMESTAMPADD(MICROSECOND, lock_at_most_for * 1000, UTC_TIMESTAMP(3))";
                jdbcTemplate.update(
                        INSERT_JOB.formatted(moment, moment),
                        jobName,
                        idPreparer.prepare(UUID.randomUUID()),
                        lockAtLeastFor,
                        lockAtMostFor
                );
            };
        }
    },
    ORACLE("oracle", "oracle.jdbc.OracleDriver") {
        @Override
        JdbcDatabaseContainer<?> container() {
            return OracleTestContainerSingleton.INSTANCE;
        }

        @Override
        OnceboxRepositoryFactory repositoryFactory(JdbcTemplate jdbcTemplate) {
            return new OracleOnceboxRepositoryFactory(jdbcTemplate);
        }

        @Override
        TestDatabaseClock clock(JdbcTemplate jdbcTemplate) {
            return new OracleDatabaseClock(jdbcTemplate);
        }

        @Override
        SqlUuidHelper uuidHelper() {
            return new OracleSqlUuidHelper();
        }

        @Override
        ResultSetMapper resultSetMapper() {
            return new DefaultBytesResultSetMapper();
        }

        @Override
        SetupJobScript setupJobScript() {
            return (jdbcTemplate, idPreparer, jobName, lockAtLeastFor, lockAtMostFor) -> {
                String now = "SYS_EXTRACT_UTC(SYSTIMESTAMP)";
                jdbcTemplate.update(
                        INSERT_JOB.formatted(now, now),
                        jobName,
                        idPreparer.prepare(UUID.randomUUID()),
                        lockAtLeastFor,
                        lockAtMostFor
                );
            };
        }
    };

    // lock_until and locked_at are the two placeholders: a bound parameter or an expression of the dialect
    private static final String INSERT_JOB = """
            INSERT INTO oncebox_jobs (job_name, lock_until, locked_by, locked_at, lock_at_least_for, lock_at_most_for)
            VALUES (?, %s, ?, %s, ?, ?)
            """;

    private final String vendor;
    private final String driver;

    TestkitDialect(String vendor, String driver) {
        this.vendor = vendor;
        this.driver = driver;
    }

    // the container is reached only here, so a run of one dialect starts only its own container
    abstract JdbcDatabaseContainer<?> container();

    abstract OnceboxRepositoryFactory repositoryFactory(JdbcTemplate jdbcTemplate);

    abstract TestDatabaseClock clock(JdbcTemplate jdbcTemplate);

    abstract SqlUuidHelper uuidHelper();

    abstract ResultSetMapper resultSetMapper();

    abstract SetupJobScript setupJobScript();

    // MySQL and Oracle keep an id as sixteen bytes, most significant half first
    IdExtractor idExtractor() {
        return raw -> {
            ByteBuffer bytes = ByteBuffer.wrap((byte []) raw);
            return new UUID(bytes.getLong(), bytes.getLong());
        };
    }

    IdPreparer idPreparer() {
        return id -> ByteBuffer.allocate(16)
                .putLong(id.getMostSignificantBits())
                .putLong(id.getLeastSignificantBits())
                .array();
    }

    String vendor() {
        return vendor;
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
}
