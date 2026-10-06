package io.github.dmitriyiliyov.oncebox.core.inbox;

import java.util.Collection;
import java.util.UUID;
import java.util.stream.Collectors;

final class LogUtils {

    private static final int LOGGED_IDS_LIMIT = 10;

    private LogUtils() {}

    static String preview(Collection<UUID> ids) {
        if (ids.size() <= LOGGED_IDS_LIMIT) {
            return ids.toString();
        }
        return ids.stream()
                .limit(LOGGED_IDS_LIMIT)
                .map(UUID::toString)
                .collect(Collectors.joining(", ", "[", ", ... and %d more]".formatted(ids.size() - LOGGED_IDS_LIMIT)));
    }
}
