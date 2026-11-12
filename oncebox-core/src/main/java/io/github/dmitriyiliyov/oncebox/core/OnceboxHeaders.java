package io.github.dmitriyiliyov.oncebox.core;

/**
 * Message headers oncebox puts on every event it sends. The receiving side reads them back to deduplicate the
 * event by its id and to choose the class its payload is read into.
 */
public enum OnceboxHeaders {
    EVENT_TYPE("oncebox_event_type"),
    EVENT_ID("oncebox_event_id"),
    EVENT_PAYLOAD_TYPE("oncebox_event_payload_type");

    private final String value;

    OnceboxHeaders(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
