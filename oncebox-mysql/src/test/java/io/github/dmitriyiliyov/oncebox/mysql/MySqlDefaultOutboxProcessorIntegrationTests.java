package io.github.dmitriyiliyov.oncebox.mysql;

import io.github.dmitriyiliyov.oncebox.core.outbox.DefaultOutboxProcessor;
import io.github.dmitriyiliyov.oncebox.core.outbox.DefaultOutboxProcessorVerifier;
import io.github.dmitriyiliyov.oncebox.core.outbox.OutboxManager;
import io.github.dmitriyiliyov.oncebox.core.outbox.OutboxSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.nio.ByteBuffer;
import java.util.UUID;

@Transactional
public class MySqlDefaultOutboxProcessorIntegrationTests extends BaseMySqlIntegrationTests {

    private DefaultOutboxProcessorVerifier verifier;

    @MockBean
    private OutboxSender outboxSender;

    @Autowired
    private OutboxManager outboxManager;

    @Autowired
    private MySqlOutboxRepository outboxRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        Mockito.reset(outboxSender);
        DefaultOutboxProcessor processor = new DefaultOutboxProcessor(outboxManager, outboxSender);
        this.verifier = new DefaultOutboxProcessorVerifier(
                jdbcTemplate,
                outboxRepository,
                processor,
                outboxSender,
                raw -> {
                    byte[] bytes = (byte[]) raw;
                    ByteBuffer bb = ByteBuffer.wrap(bytes);
                    return new UUID(bb.getLong(), bb.getLong());
                },
                id -> {
                    ByteBuffer bb = ByteBuffer.allocate(16);
                    bb.putLong(id.getMostSignificantBits());
                    bb.putLong(id.getLeastSignificantBits());
                    return bb.array();
                },
                new MySqlResultSetMapper()
        );
    }

    @Test
    @DisplayName("IT process() should throw NullPointerException when properties is null")
    void process_nullProperties_throwsNullPointerException() {
        verifier.process_nullProperties_throwsNullPointerException();
    }

    @Test
    @DisplayName("IT process() should do nothing when outbox is empty")
    void process_emptyOutbox_doesNothing() {
        verifier.process_emptyOutbox_doesNothing();
    }

    @Test
    @DisplayName("IT process() should send events and change status to PROCESSED")
    void process_allEventsSentSuccessfully_statusChangedToProcessed() {
        verifier.process_allEventsSentSuccessfully_statusChangedToProcessed();
    }

    @Test
    @DisplayName("IT process() when some events fail should increment retry count and update next retry time")
    void process_someEventsFailed_retryCountIncrementedAndNextRetryUpdated() {
        verifier.process_someEventsFailed_retryCountIncrementedAndNextRetryUpdated();
    }

    @Test
    @DisplayName("IT process() when sender throws exception should treat all batch as failed")
    void process_senderThrowsException_allEventsTreatedAsFailed() {
        verifier.process_senderThrowsException_allEventsTreatedAsFailed();
    }

    @Test
    @DisplayName("IT process() when max retries exceeded should mark event as FAILED")
    void process_maxRetriesExceeded_statusChangedToFailed() {
        verifier.process_maxRetriesExceeded_statusChangedToFailed();
    }
}
