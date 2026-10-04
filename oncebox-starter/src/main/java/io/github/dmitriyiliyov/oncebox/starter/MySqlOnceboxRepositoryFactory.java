package io.github.dmitriyiliyov.oncebox.starter;

import io.github.dmitriyiliyov.oncebox.core.inbox.InboxRepository;
import io.github.dmitriyiliyov.oncebox.core.locks.DistributedLockRepository;
import io.github.dmitriyiliyov.oncebox.core.outbox.OutboxRepository;
import io.github.dmitriyiliyov.oncebox.core.outbox.dlq.OutboxDlqRepository;
import io.github.dmitriyiliyov.oncebox.core.utils.DefaultBytesResultSetMapper;
import io.github.dmitriyiliyov.oncebox.mysql.*;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Clock;

public class MySqlOnceboxRepositoryFactory implements OnceboxRepositoryFactory {

    private final JdbcTemplate jdbcTemplate;
    private final Clock clock;

    public MySqlOnceboxRepositoryFactory(JdbcTemplate jdbcTemplate, Clock clock) {
        this.jdbcTemplate = jdbcTemplate;
        this.clock = clock;
    }

    @Override
    public OutboxRepository createOutboxRepository() {
        return new MySqlOutboxRepository(
                jdbcTemplate,
                clock,
                new MySqlUuidHelper(),
                new DefaultBytesResultSetMapper()
        );
    }

    @Override
    public OutboxDlqRepository createOutboxDlqRepository() {
        return new MySqlOutboxDlqRepository(
                jdbcTemplate,
                new MySqlUuidHelper(),
                new DefaultBytesResultSetMapper(),
                clock
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
                clock,
                new MySqlUuidHelper(),
                new DefaultBytesResultSetMapper()
        );
    }
}
