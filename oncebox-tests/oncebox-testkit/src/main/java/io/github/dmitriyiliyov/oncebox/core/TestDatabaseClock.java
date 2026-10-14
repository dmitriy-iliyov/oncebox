package io.github.dmitriyiliyov.oncebox.core;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * The database clock as the dialect tests see it: the times of a row are set by the database, so a test ages a row
 * by moving its column and reads a time back as its distance from the database time, never from the JVM clock.
 */
public interface TestDatabaseClock {

    /**
     * Moves a time column of one row by {@code delta}: a negative one into the past, a positive one into the future.
     */
    void shift(String table, String column, UUID id, Duration delta);

    /**
     * How far the time column of one row is from the database time, negative when it lies in the past.
     */
    Duration distanceFromNow(String table, String column, UUID id);

    /**
     * The value a test binds to write {@code moment} into a time column as the same moment, whatever the time zone
     * of the JVM. The default suits a column that keeps its time zone; a dialect whose column keeps none overrides it.
     */
    default Object toColumnValue(Instant moment) {
        return OffsetDateTime.ofInstant(moment, ZoneOffset.UTC);
    }
}
