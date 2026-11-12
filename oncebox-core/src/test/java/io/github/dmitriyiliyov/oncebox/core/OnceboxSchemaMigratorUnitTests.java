package io.github.dmitriyiliyov.oncebox.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThatCode;

class OnceboxSchemaMigratorUnitTests {

    @ParameterizedTest(name = "feature = ''{0}''")
    @NullAndEmptySource
    @ValueSource(strings = {"outbox", "dlq", "inbox", "jobs", "unknown"})
    @DisplayName("UT NOOP migrate() when any feature is given should leave the schema alone")
    void noopMigrate_whenAnyFeatureIsGiven_shouldDoNothing(String feature) {
        // when / then
        assertThatCode(() -> OnceboxSchemaMigrator.NOOP.migrate(feature)).doesNotThrowAnyException();
    }
}
