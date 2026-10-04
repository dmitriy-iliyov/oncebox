package io.github.dmitriyiliyov.oncebox.core;

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
