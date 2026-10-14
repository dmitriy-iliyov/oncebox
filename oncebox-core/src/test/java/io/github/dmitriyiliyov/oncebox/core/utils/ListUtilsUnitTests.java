package io.github.dmitriyiliyov.oncebox.core.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ListUtilsUnitTests {

    @Test
    @DisplayName("UT isEmpty() when list is null should return true")
    void isEmpty_whenListIsNull_shouldReturnTrue() {
        // given
        List<String> list = null;

        // when
        boolean result = ListUtils.isEmpty(list);

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("UT isEmpty() when list is empty should return true")
    void isEmpty_whenListIsEmpty_shouldReturnTrue() {
        // given
        List<String> list = List.of();

        // when
        boolean result = ListUtils.isEmpty(list);

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("UT isEmpty() when list has elements should return false")
    void isEmpty_whenListHasElements_shouldReturnFalse() {
        // given
        List<String> list = List.of("element");

        // when
        boolean result = ListUtils.isEmpty(list);

        // then
        assertThat(result).isFalse();
    }
}
