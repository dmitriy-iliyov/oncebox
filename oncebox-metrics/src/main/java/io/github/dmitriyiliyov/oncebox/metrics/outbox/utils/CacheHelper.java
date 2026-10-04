package io.github.dmitriyiliyov.oncebox.metrics.outbox.utils;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public final class CacheHelper {

    public static <S extends Enum<S>> long count(OutboxCache<S> cache, Supplier<Long> countSupplier) {
        Long count = cache.getCount();
        if (count != null) {
            return count;
        }
        return cache.putCount(countSupplier.get());
    }

    public static <S extends Enum<S>> long countByStatus(OutboxCache<S> cache, S status, Function<S, Long> countSupplier) {
        Long count = cache.getCountByStatus(status);
        if (count != null) {
            return count;
        }
        return cache.putCountByStatus(status, countSupplier.apply(status));
    }

    public static <S extends Enum<S>> long countByEventTypeAndStatus(OutboxCache<S> cache, String eventType, S status,
                                                              BiFunction<String,S, Long> countSupplier) {
        Long count = cache.getCountByEventTypeAndStatus(eventType, status);
        if (count != null) {
            return count;
        }
        return cache.putCountByEventTypeAndStatus(
                eventType, status,
                countSupplier.apply(eventType, status)
        );
    }
}
