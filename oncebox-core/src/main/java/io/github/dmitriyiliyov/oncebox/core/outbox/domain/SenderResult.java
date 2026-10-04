package io.github.dmitriyiliyov.oncebox.core.outbox.domain;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;

public record SenderResult(
        Set<UUID> processedIds,
        Set<UUID> failedIds
) {
    public static SenderResult empty() {
        return new SenderResult(Collections.emptySet(), Collections.emptySet());
    }
}
