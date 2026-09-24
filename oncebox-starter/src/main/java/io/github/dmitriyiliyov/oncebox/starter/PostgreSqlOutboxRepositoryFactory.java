package io.github.dmitriyiliyov.oncebox.starter;

import io.github.dmitriyiliyov.oncebox.core.consumer.ConsumedOutboxRepository;
import io.github.dmitriyiliyov.oncebox.core.locks.DistributedLockRepository;
import io.github.dmitriyiliyov.oncebox.core.publisher.OutboxRepository;
import io.github.dmitriyiliyov.oncebox.core.publisher.dlq.OutboxDlqRepository;
import io.github.dmitriyiliyov.oncebox.core.utils.DefaultResultSetMapper;
import io.github.dmitriyiliyov.oncebox.postgresql.*;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Clock;

public class PostgreSqlOutboxRepositoryFactory implements OutboxRepositoryFactory {

    private final JdbcTemplate jdbcTemplate;
    private final Clock clock;

    public PostgreSqlOutboxRepositoryFactory(JdbcTemplate jdbcTemplate, Clock clock) {
        this.jdbcTemplate = jdbcTemplate;
        this.clock = clock;
    }

    @Override
    public OutboxRepository createOutboxRepository() {
        return new PostgreSqlOutboxRepository(
                jdbcTemplate,
                clock,
                new PostgreSqlUuidHelper(),
                new DefaultResultSetMapper()
        );
    }

    @Override
    public OutboxDlqRepository createOutboxDlqRepository() {
        return new PostgreSqlOutboxDlqRepository(
                jdbcTemplate,
                new PostgreSqlUuidHelper(),
                new DefaultResultSetMapper(),
                clock
        );
    }

    @Override
    public DistributedLockRepository createDistributedLockRepository() {
        return new PostgreSqlDistributedLockRepository(jdbcTemplate, new PostgreSqlUuidHelper());
    }

    @Override
    public ConsumedOutboxRepository createConsumedOutboxRepository() {
        return new PostgreSqlConsumedOutboxRepository(jdbcTemplate, clock);
    }
}
