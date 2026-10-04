package io.github.dmitriyiliyov.oncebox.core.inbox;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;

public class ConcurrentInsertException extends RuntimeException {

    private final int expectedInserts;
    private final int actualInserts;
    private final Set<UUID> ids;

    public ConcurrentInsertException(int expectedInserts, int actualInserts, Set<UUID> ids) {
        super(message(expectedInserts, actualInserts, ids));
        this.expectedInserts = expectedInserts;
        this.actualInserts = actualInserts;
        this.ids = Collections.unmodifiableSet(ids);
    }

    public ConcurrentInsertException(int expectedInserts, int actualInserts,
                                     Set<UUID> ids, Throwable cause) {
        super(message(expectedInserts, actualInserts, ids), cause);
        this.expectedInserts = expectedInserts;
        this.actualInserts = actualInserts;
        this.ids = Collections.unmodifiableSet(ids);
    }

    private static String message(int expectedInserts, int actualInserts, Set<UUID> ids) {
        return String.format(
                "Concurrent insert detected: expected %d inserts but only %d succeeded. " +
                        "Some ids from %s were already inserted by another transaction.",
                expectedInserts, actualInserts, ids
        );
    }

    public int getExpectedInserts() {
        return expectedInserts;
    }

    public int getActualInserts() {
        return actualInserts;
    }

    public Set<UUID> getIds() {
        return ids;
    }
}
