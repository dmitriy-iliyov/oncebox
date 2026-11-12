package io.github.dmitriyiliyov.oncebox.starter;

/**
 * Message brokers oncebox sends events through.
 */
public enum TransportType {
    KAFKA, RABBIT;

    /**
     * Resolves a transport type by its name, ignoring case.
     *
     * @throws IllegalArgumentException if the value is {@code null}, blank or not a transport type
     */
    public static TransportType fromString(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("TransportType is null or blank");
        }
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown TransportType: " + value);
        }
    }
}
