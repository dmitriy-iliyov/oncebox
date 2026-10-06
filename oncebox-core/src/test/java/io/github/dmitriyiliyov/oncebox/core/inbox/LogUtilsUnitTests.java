package io.github.dmitriyiliyov.oncebox.core.inbox;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class LogUtilsUnitTests {

    @Test
    @DisplayName("UT preview() when ids fit the limit should list all of them")
    void preview_whenIdsFitLimit_shouldListAll() {
        // given
        List<UUID> ids = ids(10);

        // when
        String preview = LogUtils.preview(ids);

        // then
        assertThat(preview).isEqualTo(ids.toString());
    }

    @Test
    @DisplayName("UT preview() when ids exceed the limit should list the first ten and count the rest")
    void preview_whenIdsExceedLimit_shouldListFirstTenAndCountRest() {
        // given
        List<UUID> ids = ids(13);

        // when
        String preview = LogUtils.preview(ids);

        // then
        assertThat(preview)
                .startsWith("[" + ids.get(0))
                .contains(ids.get(9).toString())
                .doesNotContain(ids.get(10).toString())
                .endsWith(", ... and 3 more]");
    }

    @Test
    @DisplayName("UT preview() when ids are empty should return empty brackets")
    void preview_whenIdsAreEmpty_shouldReturnEmptyBrackets() {
        assertThat(LogUtils.preview(List.of())).isEqualTo("[]");
    }

    private static List<UUID> ids(int count) {
        return IntStream.range(0, count)
                .mapToObj(i -> UUID.fromString("00000000-0000-0000-0000-%012d".formatted(i)))
                .toList();
    }
}
