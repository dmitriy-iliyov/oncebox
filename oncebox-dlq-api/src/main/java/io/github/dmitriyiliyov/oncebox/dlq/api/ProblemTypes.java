package io.github.dmitriyiliyov.oncebox.dlq.api;

import java.net.URI;

/**
 * The {@code type} URIs the DLQ API puts into its problem responses.
 *
 * <p>A type is the only field of the response a client may branch on, and one type always comes with the same
 * status and title. They are names, not addresses - nothing is fetched from them - and they are public contract:
 * renaming one breaks every client that branches on it. A standard Spring MVC failure keeps {@code about:blank}.
 */
public final class ProblemTypes {

    private static final String BASE = "https://oncebox.io/errors/";

    public static final URI DLQ_EVENT_NOT_FOUND = URI.create(BASE + "dlq-event-not-found");

    public static final URI DLQ_EVENT_IN_PROCESS = URI.create(BASE + "dlq-event-in-process");

    public static final URI INVALID_DLQ_FILTER = URI.create(BASE + "invalid-dlq-filter");

    public static final URI VALIDATION_FAILED = URI.create(BASE + "validation-failed");

    public static final URI INTERNAL_ERROR = URI.create(BASE + "internal-error");

    private ProblemTypes() {}
}
