package io.github.dmitriyiliyov.oncebox.core.utils;

import java.util.List;

public final class ListUtils {

    private ListUtils() { }

    public static boolean isEmpty(List<?> l) {
        return l == null || l.isEmpty();
    }
}
