package io.github.dmitriyiliyov.oncebox.dlq.api;

import io.github.dmitriyiliyov.oncebox.core.publisher.dlq.DlqStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DlqFilterUnitTests {

    private static final UUID EVENT_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    @Test
    @DisplayName("UT build() when every criterion is given should report each of them")
    void build_whenEveryCriterionGiven_shouldReportEach() {
        // when
        DlqFilter tested = DlqFilter.builder()
                .status(DlqStatus.MOVED)
                .eventType("order-created")
                .ids(Set.of(EVENT_ID))
                .build();

        // then
        assertThat(tested.hasStatus()).isTrue();
        assertThat(tested.hasEventType()).isTrue();
        assertThat(tested.hasIds()).isTrue();
        assertThat(tested.getStatus()).isEqualTo(DlqStatus.MOVED);
        assertThat(tested.getEventType()).isEqualTo("order-created");
        assertThat(tested.getIds()).containsExactly(EVENT_ID);
    }

    @Test
    @DisplayName("UT build() when nothing is given should report no criterion")
    void build_whenNothingGiven_shouldReportNoCriterion() {
        // when
        DlqFilter tested = DlqFilter.builder().build();

        // then
        assertThat(tested.hasStatus()).isFalse();
        assertThat(tested.hasEventType()).isFalse();
        assertThat(tested.hasIds()).isFalse();
    }

    @Test
    @DisplayName("UT hasEventType() and hasIds() when blank event type and empty ids should treat both as absent")
    void hasCriteria_whenBlankEventTypeAndEmptyIds_shouldTreatBothAsAbsent() {
        // when
        DlqFilter tested = DlqFilter.builder().eventType("  ").ids(Set.of()).build();

        // then
        assertThat(tested.hasEventType()).isFalse();
        assertThat(tested.hasIds()).isFalse();
    }
}
