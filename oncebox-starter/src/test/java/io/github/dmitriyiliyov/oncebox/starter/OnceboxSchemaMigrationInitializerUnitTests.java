package io.github.dmitriyiliyov.oncebox.starter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OnceboxSchemaMigrationInitializerUnitTests {

    @Test
    @DisplayName("UT constructor when migration is null should throw NullPointerException")
    void constructor_whenMigrationIsNull_shouldThrowNullPointerException() {
        // when / then
        assertThatThrownBy(() -> new OnceboxSchemaMigrationInitializer(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("migration cannot be null");
    }

    @Test
    @DisplayName("UT constructor when migration is given should not run it")
    void constructor_whenMigrationIsGiven_shouldNotRunIt() {
        // given
        AtomicInteger runs = new AtomicInteger();

        // when
        new OnceboxSchemaMigrationInitializer(runs::incrementAndGet);

        // then
        assertThat(runs).hasValue(0);
    }

    @Test
    @DisplayName("UT afterPropertiesSet() when migration succeeds should run it once")
    void afterPropertiesSet_whenMigrationSucceeds_shouldRunItOnce() throws Exception {
        // given
        AtomicInteger runs = new AtomicInteger();
        OnceboxSchemaMigrationInitializer tested = new OnceboxSchemaMigrationInitializer(runs::incrementAndGet);

        // when
        tested.afterPropertiesSet();

        // then
        assertThat(runs).hasValue(1);
    }

    @Test
    @DisplayName("UT afterPropertiesSet() when migration fails should propagate its exception")
    void afterPropertiesSet_whenMigrationFails_shouldPropagateItsException() {
        // given
        IllegalStateException failure = new IllegalStateException("migration failed");
        OnceboxSchemaMigrationInitializer tested = new OnceboxSchemaMigrationInitializer(() -> {
            throw failure;
        });

        // when / then
        assertThatThrownBy(tested::afterPropertiesSet).isSameAs(failure);
    }
}
