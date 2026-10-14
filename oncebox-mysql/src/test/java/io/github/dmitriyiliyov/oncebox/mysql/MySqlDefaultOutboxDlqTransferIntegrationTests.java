package io.github.dmitriyiliyov.oncebox.mysql;

import io.github.dmitriyiliyov.oncebox.core.outbox.dlq.DefaultOutboxDlqTransfer;
import io.github.dmitriyiliyov.oncebox.core.outbox.dlq.DefaultOutboxDlqTransferVerifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.nio.ByteBuffer;
import java.util.UUID;

@Transactional
class MySqlDefaultOutboxDlqTransferIntegrationTests extends BaseMySqlIntegrationTests {

    private final DefaultOutboxDlqTransferVerifier verifier;

    @Autowired
    MySqlDefaultOutboxDlqTransferIntegrationTests(MySqlOutboxRepository outboxRepository,
                                                  MySqlOutboxDlqRepository dlqRepository,
                                                  @Qualifier("mysqlOutboxDlqTransfer") DefaultOutboxDlqTransfer transfer,
                                                  JdbcTemplate jdbcTemplate) {
        this.verifier = new DefaultOutboxDlqTransferVerifier(
                jdbcTemplate,
                outboxRepository,
                dlqRepository,
                transfer,
                raw -> {
                    byte[] bytes = (byte[]) raw;
                    ByteBuffer bb = ByteBuffer.wrap(bytes);
                    return new UUID(bb.getLong(), bb.getLong());
                }
        );
    }

    @Test @DisplayName("IT transferToDlq() failed events should be moved to DLQ and deleted from outbox")
    void transferToDlq_failedEvents_movedToDlqAndDeletedFromOutbox() { verifier.transferToDlq_failedEvents_movedToDlqAndDeletedFromOutbox(); }

    @Test @DisplayName("IT transferToDlq() no failed events should do nothing")
    void transferToDlq_noFailedEvents_doesNothing() { verifier.transferToDlq_noFailedEvents_doesNothing(); }

    @Test @DisplayName("IT transferToDlq() should respect batch size")
    void transferToDlq_respectsBatchSize() { verifier.transferToDlq_respectsBatchSize(); }

    @Test @DisplayName("IT transferToDlq() should preserve event data")
    void transferToDlq_preservesEventData() { verifier.transferToDlq_preservesEventData(); }

    @Test @DisplayName("IT transferFromDlq() TO_RETRY events should be moved back to outbox")
    void transferFromDlq_toRetryEvents_movedBackToOutbox() { verifier.transferFromDlq_toRetryEvents_movedBackToOutbox(); }

    @Test @DisplayName("IT transferFromDlq() no TO_RETRY events should do nothing")
    void transferFromDlq_noToRetryEvents_doesNothing() { verifier.transferFromDlq_noToRetryEvents_doesNothing(); }

    @Test @DisplayName("IT transferFromDlq() should respect batch size")
    void transferFromDlq_respectsBatchSize() { verifier.transferFromDlq_respectsBatchSize(); }

    @Test @DisplayName("IT transferFromDlq() should reset retry count to zero")
    void transferFromDlq_resetsRetryCountToZero() { verifier.transferFromDlq_resetsRetryCountToZero(); }

    @Test @DisplayName("IT transferFromDlq() should preserve event data")
    void transferFromDlq_preservesEventData() { verifier.transferFromDlq_preservesEventData(); }

    @Test @DisplayName("IT transferFromDlq() when an event is brought back should keep its creation time")
    void transferFromDlq_whenEventIsBroughtBack_shouldKeepItsCreationTime() { verifier.transferFromDlq_whenEventIsBroughtBack_shouldKeepItsCreationTime(); }
}
