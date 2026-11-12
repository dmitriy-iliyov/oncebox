package io.github.dmitriyiliyov.oncebox.starter;

import io.github.dmitriyiliyov.oncebox.core.inbox.InboxRepository;
import io.github.dmitriyiliyov.oncebox.core.locks.DistributedLockRepository;
import io.github.dmitriyiliyov.oncebox.core.outbox.OutboxRepository;
import io.github.dmitriyiliyov.oncebox.core.outbox.dlq.OutboxDlqRepository;
import io.github.dmitriyiliyov.oncebox.mysql.*;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Objects;


public class MySqlOnceboxRepositoryFactory implements OnceboxRepositoryFactory {

    private final JdbcTemplate jdbcTemplate;

    public MySqlOnceboxRepositoryFactory(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate cannot be null");
    }

    @Override
    public OutboxRepository createOutboxRepository() {
        return new MySqlOutboxRepository(
                jdbcTemplate,
                new MySqlUuidHelper(),
                new MySqlResultSetMapper()
        );
    }

    @Override
    public OutboxDlqRepository createOutboxDlqRepository() {
        return new MySqlOutboxDlqRepository(
                jdbcTemplate,
                new MySqlUuidHelper(),
                new MySqlResultSetMapper()
        );
    }

    @Override
    public DistributedLockRepository createDistributedLockRepository() {
        return new MySqlDistributedLockRepository(jdbcTemplate, new MySqlUuidHelper());
    }

    @Override
    public InboxRepository createInboxRepository() {
        return new MySqlInboxRepository(
                jdbcTemplate,
                new MySqlUuidHelper(),
                new MySqlResultSetMapper()
        );
    }
}
