package io.github.dmitriyiliyov.oncebox.core.utils;

import java.util.HashSet;
import java.util.Set;

public final class SetUtils {

    private SetUtils() {}

    public static <T> boolean isEmpty(Set<T> s) {
        return s == null || s.isEmpty();
    }

    public static <T> int size(Set<T> s) {
        return s == null ? 0 : s.size();
    }

    public static <T> Set<T> mutableCopy(Set<T> s) {
        return s == null ? null : new HashSet<>(s);
    }
}
