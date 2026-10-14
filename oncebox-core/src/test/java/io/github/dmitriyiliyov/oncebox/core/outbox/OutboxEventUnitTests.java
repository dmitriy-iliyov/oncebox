package io.github.dmitriyiliyov.oncebox.core.outbox;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OutboxEventUnitTests {

    private static final UUID ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1");
    private static final UUID OTHER_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2");
    private static final Instant NEXT_RETRY_AT = Instant.parse("2026-01-01T10:05:00Z");
    private static final Instant CREATED_AT = Instant.parse("2026-01-01T10:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-01-01T10:01:00Z");

    @Test
    @DisplayName("UT constructor when id is null should throw NullPointerException")
    void constructor_whenIdIsNull_shouldThrowNullPointerException() {
        assertThatThrownBy(() -> new OutboxEvent(null, EventStatus.PENDING, "TYPE", "JSON", "{}", 0,
                NEXT_RETRY_AT, CREATED_AT, UPDATED_AT))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("id cannot be null");
    }

    @Test
    @DisplayName("UT constructor of a new event should make it pending with no times and no retry delay")
    void constructor_whenEventIsNew_shouldBePendingWithoutTimesAndRetryDelay() {
        // when
        OutboxEvent tested = new OutboxEvent(ID, "TYPE", "JSON", "{}");

        // then
        assertThat(tested.getStatus()).isEqualTo(EventStatus.PENDING);
        assertThat(tested.getNextRetryAt()).isNull();
        assertThat(tested.getCreatedAt()).isNull();
        assertThat(tested.getUpdatedAt()).isNull();
        assertThat(tested.getRetryDelay()).isNull();
    }

    @Test
    @DisplayName("UT copy() should carry every field, the retry delay included")
    void copy_whenEventIsRead_shouldCarryEveryField() {
        // given
        OutboxEvent tested = storedEvent();
        tested.setRetryDelay(Duration.ofSeconds(30));

        // when
        OutboxEvent copy = tested.copy();

        // then
        assertThat(copy).usingRecursiveComparison().isEqualTo(tested);
        assertThat(copy.getRetryDelay()).isEqualTo(Duration.ofSeconds(30));
    }

    @Test
    @DisplayName("UT copy() when the copy is changed should leave the original as it was")
    void copy_whenCopyIsChanged_shouldLeaveOriginalUnchanged() {
        // given
        OutboxEvent tested = storedEvent();

        // when
        OutboxEvent copy = tested.copy();
        copy.setStatus(EventStatus.FAILED);
        copy.setRetryCount(3);
        copy.setRetryDelay(Duration.ofSeconds(30));

        // then
        assertThat(copy).isNotSameAs(tested);
        assertThat(tested.getStatus()).isEqualTo(EventStatus.IN_PROCESS);
        assertThat(tested.getRetryCount()).isEqualTo(1);
        assertThat(tested.getRetryDelay()).isNull();
    }

    @Test
    @DisplayName("UT equals() when ids are equal should be true whatever the other fields")
    void equals_whenIdsAreEqual_shouldBeTrueWhateverOtherFields() {
        // given
        OutboxEvent tested = new OutboxEvent(ID, "TYPE1", "JSON", "{}");
        OutboxEvent other = new OutboxEvent(ID, EventStatus.FAILED, "TYPE2", "XML", "<xml/>", 2,
                NEXT_RETRY_AT, CREATED_AT, UPDATED_AT);

        // when / then
        assertThat(tested).isEqualTo(other);
        assertThat(tested.hashCode()).isEqualTo(other.hashCode());
    }

    @Test
    @DisplayName("UT equals() when ids differ should be false")
    void equals_whenIdsDiffer_shouldBeFalse() {
        // given
        OutboxEvent tested = new OutboxEvent(ID, "TYPE", "JSON", "{}");
        OutboxEvent other = new OutboxEvent(OTHER_ID, "TYPE", "JSON", "{}");

        // when / then
        assertThat(tested).isNotEqualTo(other);
    }

    @Test
    @DisplayName("UT equals() when compared with null or another type should be false")
    void equals_whenNullOrAnotherType_shouldBeFalse() {
        // given
        OutboxEvent tested = new OutboxEvent(ID, "TYPE", "JSON", "{}");

        // when / then
        assertThat(tested).isNotEqualTo(null);
        assertThat(tested).isNotEqualTo("TYPE");
    }

    private OutboxEvent storedEvent() {
        return new OutboxEvent(ID, EventStatus.IN_PROCESS, "TYPE", "JSON", "{}", 1,
                NEXT_RETRY_AT, CREATED_AT, UPDATED_AT);
    }
}
