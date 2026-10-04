package io.github.dmitriyiliyov.oncebox.dlq.api;

import io.github.dmitriyiliyov.oncebox.core.outbox.dlq.DlqStatus;

import java.util.Set;
import java.util.UUID;

public class DlqFilter {

    private final DlqStatus status;
    private final String eventType;
    private final Set<UUID> ids;

    private DlqFilter(DlqStatus status, String eventType, Set<UUID> ids) {
        this.status = status;
        this.eventType = eventType;
        this.ids = ids;
    }

    public static Builder builder() {
        return new Builder();
    }

    public DlqStatus getStatus() {
        return status;
    }

    public boolean hasStatus() {
        return status != null;
    }

    public String getEventType() {
        return eventType;
    }

    public boolean hasEventType() {
        return eventType != null && !eventType.isBlank();
    }

    public Set<UUID> getIds() {
        return ids;
    }

    public boolean hasIds() {
        return ids != null && !ids.isEmpty();
    }

    public static class Builder {

        private DlqStatus status;
        private String eventType;
        private Set<UUID> ids;

        public Builder status(DlqStatus status) {
            this.status = status;
            return this;
        }

        public Builder eventType(String eventType) {
            this.eventType = eventType;
            return this;
        }

        public Builder ids(Set<UUID> ids) {
            this.ids = ids;
            return this;
        }

        public DlqFilter build() {
            return new DlqFilter(status, eventType, ids);
        }
    }
}
