package io.github.dmitriyiliyov.oncebox.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OnceboxMigrationExceptionUnitTests {

    @Test
    @DisplayName("UT constructor should name the feature and the database and keep the cause")
    void constructor_whenCreated_shouldNameFeatureAndDatabaseAndKeepCause() {
        // given
        IllegalStateException cause = new IllegalStateException("syntax error at or near \"TABLE\"");

        // when
        OnceboxMigrationException tested = new OnceboxMigrationException("outbox", "postgresql", cause);

        // then
        assertThat(tested.getFeature()).isEqualTo("outbox");
        assertThat(tested.getVendor()).isEqualTo("postgresql");
        assertThat(tested.getMessage()).isEqualTo("Migration of oncebox feature 'outbox' failed on database 'postgresql'");
        assertThat(tested.getCause()).isSameAs(cause);
    }

    @Test
    @DisplayName("UT constructor when the cause is null should throw NullPointerException")
    void constructor_whenCauseIsNull_shouldThrowNullPointerException() {
        // when / then
        assertThatThrownBy(() -> new OnceboxMigrationException("outbox", "postgresql", null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("cause cannot be null");
    }
}
