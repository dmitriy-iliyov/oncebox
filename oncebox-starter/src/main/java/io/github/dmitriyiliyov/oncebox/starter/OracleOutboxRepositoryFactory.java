package io.github.dmitriyiliyov.oncebox.starter;

import io.github.dmitriyiliyov.oncebox.core.consumer.ConsumedOutboxRepository;
import io.github.dmitriyiliyov.oncebox.core.locks.DistributedLockRepository;
import io.github.dmitriyiliyov.oncebox.core.publisher.OutboxRepository;
import io.github.dmitriyiliyov.oncebox.core.publisher.dlq.OutboxDlqRepository;
import io.github.dmitriyiliyov.oncebox.core.utils.DefaultBytesResultSetMapper;
import io.github.dmitriyiliyov.oncebox.oracle.*;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Clock;

public class OracleOutboxRepositoryFactory implements OutboxRepositoryFactory {

    private final JdbcTemplate jdbcTemplate;
    private final Clock clock;

    public OracleOutboxRepositoryFactory(JdbcTemplate jdbcTemplate, Clock clock) {
        this.jdbcTemplate = jdbcTemplate;
        this.clock = clock;
    }

    @Override
    public OutboxRepository createOutboxRepository() {
        return new OracleOutboxRepository(
                jdbcTemplate,
                clock,
                new OracleSqlUuidHelper(),
                new DefaultBytesResultSetMapper()
        );
    }

    @Override
    public OutboxDlqRepository createOutboxDlqRepository() {
        return new OracleOutboxDlqRepository(
                jdbcTemplate,
                new OracleSqlUuidHelper(),
                new DefaultBytesResultSetMapper(),
                clock
        );
    }

    @Override
    public DistributedLockRepository createDistributedLockRepository() {
        return new OracleDistributedLockRepository(jdbcTemplate, new OracleSqlUuidHelper());
    }

    @Override
    public ConsumedOutboxRepository createConsumedOutboxRepository() {
        return new OracleConsumedOutboxRepository(
                jdbcTemplate,
                clock,
                new OracleSqlUuidHelper(),
                new DefaultBytesResultSetMapper()
        );
    }
}
