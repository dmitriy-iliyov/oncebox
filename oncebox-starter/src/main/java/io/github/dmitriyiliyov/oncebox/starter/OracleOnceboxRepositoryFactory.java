package io.github.dmitriyiliyov.oncebox.starter;

import io.github.dmitriyiliyov.oncebox.core.inbox.InboxRepository;
import io.github.dmitriyiliyov.oncebox.core.locks.DistributedLockRepository;
import io.github.dmitriyiliyov.oncebox.core.outbox.OutboxRepository;
import io.github.dmitriyiliyov.oncebox.core.outbox.dlq.OutboxDlqRepository;
import io.github.dmitriyiliyov.oncebox.core.utils.DefaultBytesResultSetMapper;
import io.github.dmitriyiliyov.oncebox.oracle.*;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Clock;

public class OracleOnceboxRepositoryFactory implements OnceboxRepositoryFactory {

    private final JdbcTemplate jdbcTemplate;
    private final Clock clock;

    public OracleOnceboxRepositoryFactory(JdbcTemplate jdbcTemplate, Clock clock) {
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
    public InboxRepository createInboxRepository() {
        return new OracleInboxRepository(
                jdbcTemplate,
                clock,
                new OracleSqlUuidHelper(),
                new DefaultBytesResultSetMapper()
        );
    }
}
