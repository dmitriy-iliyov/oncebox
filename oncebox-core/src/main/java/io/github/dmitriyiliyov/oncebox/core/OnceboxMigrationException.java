package io.github.dmitriyiliyov.oncebox.core;

import java.util.Objects;

/**
 * The tables of an oncebox feature could not be migrated. Whatever the migration tool threw is the cause, so a caller
 * handles one type whichever tool the schema mode selects.
 *
 * @see OnceboxSchemaMigrator#migrate(String)
 */
public class OnceboxMigrationException extends RuntimeException {

    private final String feature;
    private final String vendor;

    public OnceboxMigrationException(String feature, String vendor, Throwable cause) {
        super(
                "Migration of oncebox feature '%s' failed on database '%s'".formatted(feature, vendor),
                Objects.requireNonNull(cause, "cause cannot be null")
        );
        this.feature = feature;
        this.vendor = vendor;
    }

    public String getFeature() {
        return feature;
    }

    public String getVendor() {
        return vendor;
    }
}
