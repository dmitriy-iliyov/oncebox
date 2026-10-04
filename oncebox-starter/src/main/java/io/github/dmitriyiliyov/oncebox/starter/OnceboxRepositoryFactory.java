package io.github.dmitriyiliyov.oncebox.starter;

import io.github.dmitriyiliyov.oncebox.core.inbox.InboxRepository;
import io.github.dmitriyiliyov.oncebox.core.locks.DistributedLockRepository;
import io.github.dmitriyiliyov.oncebox.core.outbox.OutboxRepository;
import io.github.dmitriyiliyov.oncebox.core.outbox.dlq.OutboxDlqRepository;

/**
 * A factory for creating repositories required by the outbox and the inbox.
 * <p>
 * Implementations provide specific instances of repositories tailored to the database type.
 */
public interface OnceboxRepositoryFactory {
    
    /**
     * Creates an instance of {@link OutboxRepository} for managing outbox events.
     *
     * @return the created {@link OutboxRepository}.
     */
    OutboxRepository createOutboxRepository();

    /**
     * Creates an instance of {@link OutboxDlqRepository} for managing dead-letter queue outbox events.
     *
     * @return the created {@link OutboxDlqRepository}.
     */
    OutboxDlqRepository createOutboxDlqRepository();

    /**
     * Creates an instance of {@link DistributedLockRepository} for managing distributed locks.
     *
     * @return the created {@link DistributedLockRepository}.
     */
    DistributedLockRepository createDistributedLockRepository();

    /**
     * Creates an instance of {@link InboxRepository} for managing the IDs of consumed events.
     *
     * @return the created {@link InboxRepository}.
     */
    InboxRepository createInboxRepository();
}
