package io.github.dmitriyiliyov.oncebox.starter;

import io.github.dmitriyiliyov.oncebox.core.inbox.InboxRepository;
import io.github.dmitriyiliyov.oncebox.core.locks.DistributedLockRepository;
import io.github.dmitriyiliyov.oncebox.core.outbox.OutboxRepository;
import io.github.dmitriyiliyov.oncebox.core.outbox.dlq.OutboxDlqRepository;
import io.github.dmitriyiliyov.oncebox.core.utils.DefaultResultSetMapper;
import io.github.dmitriyiliyov.oncebox.postgresql.*;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Objects;


public class PostgreSqlOnceboxRepositoryFactory implements OnceboxRepositoryFactory {

    private final JdbcTemplate jdbcTemplate;

    public PostgreSqlOnceboxRepositoryFactory(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate cannot be null");
    }

    @Override
    public OutboxRepository createOutboxRepository() {
        return new PostgreSqlOutboxRepository(
                jdbcTemplate,
                new PostgreSqlUuidHelper(),
                new DefaultResultSetMapper()
        );
    }

    @Override
    public OutboxDlqRepository createOutboxDlqRepository() {
        return new PostgreSqlOutboxDlqRepository(
                jdbcTemplate,
                new PostgreSqlUuidHelper(),
                new DefaultResultSetMapper()
        );
    }

    @Override
    public DistributedLockRepository createDistributedLockRepository() {
        return new PostgreSqlDistributedLockRepository(jdbcTemplate, new PostgreSqlUuidHelper());
    }

    @Override
    public InboxRepository createInboxRepository() {
        return new PostgreSqlInboxRepository(jdbcTemplate);
    }
}
