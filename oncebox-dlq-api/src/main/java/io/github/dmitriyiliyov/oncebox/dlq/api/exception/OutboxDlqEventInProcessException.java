package io.github.dmitriyiliyov.oncebox.dlq.api.exception;

import java.util.UUID;

public class OutboxDlqEventInProcessException extends OutboxDlqException {

    private final UUID id;

    public OutboxDlqEventInProcessException(UUID id) {
        this.id = id;
    }

    @Override
    public String getDetail() {
        return "Outbox DLQ event with id=%s is IN_PROCESS, interaction impossible".formatted(id);
    }

    public UUID getId() {
        return id;
    }
}
