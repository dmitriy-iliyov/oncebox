package io.github.dmitriyiliyov.oncebox.tests.integration.consume.shared;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * How many times a business operation ran for each event ID - counted by the operation itself, so a run is seen
 * even when the operation fails and its rows say nothing.
 */
public class OperationRuns {

    private final Map<UUID, Integer> runs = new ConcurrentHashMap<>();

    public void record(Collection<UUID> eventIds) {
        eventIds.forEach(id -> runs.merge(id, 1, Integer::sum));
    }

    public Map<UUID, Integer> of(Collection<UUID> eventIds) {
        return eventIds.stream()
                .filter(runs::containsKey)
                .collect(Collectors.toMap(id -> id, runs::get));
    }

    public void clear() {
        runs.clear();
    }
}
