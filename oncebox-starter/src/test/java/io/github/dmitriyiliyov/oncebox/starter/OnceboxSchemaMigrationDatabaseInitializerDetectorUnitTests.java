package io.github.dmitriyiliyov.oncebox.starter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.sql.init.dependency.DatabaseInitializerDetector;
import org.springframework.core.io.support.SpringFactoriesLoader;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class OnceboxSchemaMigrationDatabaseInitializerDetectorUnitTests {

    private final OnceboxSchemaMigrationDatabaseInitializerDetector tested =
            new OnceboxSchemaMigrationDatabaseInitializerDetector();

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {
            "org.springframework.boot.flyway.FlywayDatabaseInitializerDetector",
            "org.springframework.boot.liquibase.LiquibaseDatabaseInitializerDetector",
            "org.springframework.boot.autoconfigure.flyway.FlywayMigrationInitializerDatabaseInitializerDetector"
    })
    @DisplayName("UT getOrder() when compared with the app's Flyway and Liquibase detectors should come after")
    void getOrder_whenComparedWithApplicationMigrationDetectors_shouldComeAfter(String bootDetector) {
        // given
        int bootOrder = bootDetector(bootDetector).getOrder();

        // when
        int order = tested.getOrder();

        // then
        assertThat(order).isGreaterThan(bootOrder);
    }

    @Test
    @DisplayName("UT getOrder() when compared with the spring.sql.init detector should come before")
    void getOrder_whenComparedWithSqlInitDetector_shouldComeBefore() {
        // given
        String sqlInitDetector = "org.springframework.boot.jdbc.init.DataSourceScriptDatabaseInitializerDetector";
        int sqlInitOrder = bootDetector(sqlInitDetector).getOrder();

        // when
        int order = tested.getOrder();

        // then
        assertThat(order).isLessThan(sqlInitOrder);
    }

    @Test
    @DisplayName("UT getDatabaseInitializerBeanTypes() should detect only the schema migration initializer")
    void getDatabaseInitializerBeanTypes_shouldDetectOnlySchemaMigrationInitializer() {
        // when
        Set<Class<?>> types = tested.getDatabaseInitializerBeanTypes();

        // then
        assertThat(types).containsExactly(OnceboxSchemaMigrationInitializer.class);
    }

    @Test
    @DisplayName("UT detector should be registered where Spring Boot loads DatabaseInitializerDetectors from")
    void registration_shouldBeLoadedBySpringFactoriesLoader() {
        // given
        // Boot's DatabaseInitializationDependencyConfigurer loads detectors this way, from META-INF/spring.factories
        SpringFactoriesLoader loader = SpringFactoriesLoader.forDefaultResourceLocation(getClass().getClassLoader());

        // when
        List<DatabaseInitializerDetector> detectors = loader.load(DatabaseInitializerDetector.class);

        // then
        assertThat(detectors).hasAtLeastOneElementOfType(OnceboxSchemaMigrationDatabaseInitializerDetector.class);
    }

    // Boot's detectors are package-private, so they are taken from where Boot itself loads them
    private static DatabaseInitializerDetector bootDetector(String className) {
        return SpringFactoriesLoader.forDefaultResourceLocation(
                        OnceboxSchemaMigrationDatabaseInitializerDetectorUnitTests.class.getClassLoader()
                )
                .load(DatabaseInitializerDetector.class)
                .stream()
                .filter(detector -> detector.getClass().getName().equals(className))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Detector %s is not registered".formatted(className)));
    }
}
