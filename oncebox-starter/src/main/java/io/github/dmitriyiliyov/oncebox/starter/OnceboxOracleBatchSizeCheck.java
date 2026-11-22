package io.github.dmitriyiliyov.oncebox.starter;

import io.github.dmitriyiliyov.oncebox.starter.outbox.OutboxProperties;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

public final class OnceboxOracleBatchSizeCheck {

    private static final int MAX_IN_LIST_SIZE = 1000;

    private OnceboxOracleBatchSizeCheck() { }

    public static void check(JdbcTemplate jdbcTemplate, OnceboxProperties properties) {
        if (hasNotLimit(jdbcTemplate)) {
            return;
        }

        String message = """
                '%s' must not be greater than 1000 on Oracle: a batch is passed to the database as a single IN list, \
                and Oracle rejects an IN list of more than 1000 expressions (ORA-01795)""";
        if (properties.getInbox().isEnabled()
                && properties.getInbox().getCleanUp().isEnabled()
                && properties.getInbox().getCleanUp().getBatchSize() > MAX_IN_LIST_SIZE) {
            throw new IllegalArgumentException(message.formatted("oncebox.inbox.clean-up.batch-size"));
        }

        if (properties.getOutbox().isEnabled()) {
            if (properties.getOutbox().getCleanUp().isEnabled()
                    && properties.getOutbox().getCleanUp().getBatchSize() > MAX_IN_LIST_SIZE) {
                throw new IllegalArgumentException(message.formatted("oncebox.outbox.clean-up.batch-size"));
            }
            if (properties.getOutbox().getDlq().isEnabled()) {
                String property = "oncebox.outbox.dlq";
                if (properties.getOutbox().getDlq().getTransferFrom().getBatchSize() > MAX_IN_LIST_SIZE) {
                    throw new IllegalArgumentException(message.formatted(property + ".transfer-from.batch-size"));
                }
                if (properties.getOutbox().getDlq().getTransferTo().getBatchSize() > MAX_IN_LIST_SIZE) {
                    throw new IllegalArgumentException(message.formatted(property + ".transfer-to.batch-size"));
                }
                if (properties.getOutbox().getDlq().getCleanUp().isEnabled()
                        && properties.getOutbox().getDlq().getCleanUp().getBatchSize() > MAX_IN_LIST_SIZE) {
                    throw new IllegalArgumentException(message.formatted(property + ".clean-up.batch-size"));
                }
            }

            Map<String, OutboxProperties.EventProperties> events = properties.getOutbox().getEvents();
            for (String eventName : events.keySet()) {
                if (events.get(eventName).getBatchSize() > MAX_IN_LIST_SIZE) {
                    throw new IllegalArgumentException(message.formatted("oncebox.outbox.events." + eventName + ".batch-size"));
                }
            }

            if (properties.getOutbox().getStuckRecovery().getBatchSize() > MAX_IN_LIST_SIZE) {
                throw new IllegalArgumentException(message.formatted("oncebox.outbox.stuck-recovery.batch-size"));
            }
        }
    }

    private static boolean hasNotLimit(JdbcTemplate jdbcTemplate) {
        String binds = String.join(",", Collections.nCopies(MAX_IN_LIST_SIZE + 1, "?"));
        Object[] args = new Object[MAX_IN_LIST_SIZE + 1];
        Arrays.fill(args, 1);
        try {
            jdbcTemplate.queryForList("SELECT 1 FROM dual WHERE 1 IN (" + binds + ")", args);
            return true;
        } catch (DataAccessException e) {
            if (e.getMostSpecificCause() instanceof SQLException sql && sql.getErrorCode() == 1795) {
                return false;
            }
            throw e;
        }
    }
}
