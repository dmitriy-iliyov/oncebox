package io.github.dmitriyiliyov.oncebox.starter;

import io.github.dmitriyiliyov.oncebox.core.OnceboxSchemaMigrator;
import io.github.dmitriyiliyov.oncebox.starter.outbox.OutboxProperties;

import java.util.Objects;

public final class OnceboxDatabaseMigrationOrchestrator {

    private OnceboxDatabaseMigrationOrchestrator() {}

    public static void migrate(OnceboxSchemaMigrator migrator, OnceboxProperties properties) {
        Objects.requireNonNull(migrator, "migrator cannot be null");
        Objects.requireNonNull(properties, "properties cannot be null");

        if (isOutboxEnabled(properties)) {
            migrator.migrate(OnceboxFeature.OUTBOX.getValue());
            if (isDlqEnabled(properties.getOutbox())) {
                migrator.migrate(OnceboxFeature.DLQ.getValue());
            }
        }

        if (isInboxEnabled(properties)) {
            migrator.migrate(OnceboxFeature.INBOX.getValue());
        }

        if (isAnyCleanUpEnabled(properties)) {
            migrator.migrate(OnceboxFeature.JOBS.getValue());
        }
    }

    private static boolean isOutboxEnabled(OnceboxProperties properties) {
        return properties.getOutbox() != null && Boolean.TRUE.equals(properties.getOutbox().isEnabled());
    }

    private static boolean isDlqEnabled(OutboxProperties outbox) {
        return outbox.getDlq() != null && Boolean.TRUE.equals(outbox.getDlq().isEnabled());
    }

    private static boolean isInboxEnabled(OnceboxProperties properties) {
        return properties.getInbox() != null && Boolean.TRUE.equals(properties.getInbox().isEnabled());
    }

    private static boolean isAnyCleanUpEnabled(OnceboxProperties properties) {
        if (isOutboxEnabled(properties) && properties.getOutbox().getCleanUp() != null
                && Boolean.TRUE.equals(properties.getOutbox().getCleanUp().isEnabled())) {
            return true;
        }
        return isInboxEnabled(properties) && properties.getInbox().getCleanUp() != null
                && Boolean.TRUE.equals(properties.getInbox().getCleanUp().isEnabled());
    }
}
