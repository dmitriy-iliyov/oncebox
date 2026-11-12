package io.github.dmitriyiliyov.oncebox.starter;

/**
 * How often a background task polls the database: at a fixed delay or at one that grows while there is nothing
 * to do.
 */
public enum PollingType {
    FIXED, ADAPTIVE;

    /**
     * Resolves a polling type by its name, ignoring case.
     *
     * @throws IllegalArgumentException if the value is {@code null}, blank or not a polling type
     */
    public static PollingType from(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("PoolingType is null, empty or blank");
        }
        try {
            return valueOf(value.toUpperCase());
        } catch (Exception e) {
            throw new IllegalArgumentException("Unsupported pooling type '%s'".formatted(value));
        }
    }
}
