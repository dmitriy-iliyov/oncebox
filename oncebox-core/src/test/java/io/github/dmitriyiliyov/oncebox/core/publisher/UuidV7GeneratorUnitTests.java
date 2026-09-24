package io.github.dmitriyiliyov.oncebox.core.publisher;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UuidV7GeneratorUnitTests {

    private final UuidV7Generator tested = new UuidV7Generator();

    @Test
    @DisplayName("UT generate() should return an RFC 9562 version 7 UUID")
    void generate_shouldReturnVersion7Uuid() {
        // when
        UUID id = tested.generate();

        // then
        assertThat(id.version()).isEqualTo(7);
        assertThat(id.variant()).isEqualTo(2);
    }

    @Test
    @DisplayName("UT generate() when called in sequence should return ids in creation order")
    void generate_whenCalledInSequence_shouldReturnIdsInCreationOrder() {
        // given
        List<UUID> ids = new ArrayList<>();

        // when
        for (int i = 0; i < 1000; i++) {
            ids.add(tested.generate());
        }

        // then
        assertThat(ids).isSortedAccordingTo(UUID::compareTo).doesNotHaveDuplicates();
    }
}
