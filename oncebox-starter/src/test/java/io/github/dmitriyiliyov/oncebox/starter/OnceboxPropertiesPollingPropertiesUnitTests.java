package io.github.dmitriyiliyov.oncebox.starter;

import io.github.dmitriyiliyov.oncebox.starter.OnceboxProperties.PollingProperties;
import io.github.dmitriyiliyov.oncebox.starter.OnceboxProperties.PollingProperties.Defaults;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

class OnceboxPropertiesPollingPropertiesUnitTests {

    @Test
    @DisplayName("UT PollingProperties.Defaults should throw NullPointerException when type is null")
    void defaultsConstructor_withNullType_throwsIllegalStateException() {
        PollingProperties properties = new PollingProperties();
        properties.setType(null);
        assertThrows(NullPointerException.class, () -> new Defaults(null, Duration.ofSeconds(1), Duration.ofSeconds(2), Duration.ZERO, Duration.ZERO, Double.NaN));
    }

    @Test
    @DisplayName("UT Defaults constructor should throw when any required parameter is null")
    void defaults_constructor_throwsIfAnyParamIsNull() {
        Duration min = Duration.ofSeconds(1);
        Duration max = Duration.ofSeconds(2);
        assertThrows(NullPointerException.class, () -> new Defaults(PollingType.ADAPTIVE, null, Duration.ZERO, min, max, 2.0));
        assertThrows(NullPointerException.class, () -> new Defaults(PollingType.ADAPTIVE, Duration.ZERO, Duration.ZERO, null, max, 2.0));
        assertThrows(NullPointerException.class, () -> new Defaults(PollingType.ADAPTIVE, Duration.ZERO, Duration.ZERO, min, null, 2.0));
        assertThrows(NullPointerException.class, () -> new Defaults(PollingType.ADAPTIVE, Duration.ZERO, Duration.ZERO, min, max, null));
    }

    @Test
    @DisplayName("UT Defaults constructor should throw when minFixedDelay is greater than maxFixedDelay")
    void defaults_constructor_throwsIfMinGreaterThanMax() {
        assertThrows(IllegalArgumentException.class, () ->
                new Defaults(PollingType.ADAPTIVE, Duration.ZERO, Duration.ZERO, Duration.ofSeconds(5), Duration.ofSeconds(2), 1.0)
        );
    }

    @Test
    @DisplayName("UT Defaults constructor should throw when multiplier is zero or negative")
    void defaults_constructor_throwsIfMultiplierZeroOrNegative() {
        assertThrows(IllegalArgumentException.class, () ->
                new Defaults(PollingType.ADAPTIVE, Duration.ZERO, Duration.ZERO, Duration.ofSeconds(1), Duration.ofSeconds(2), 0.0)
        );
        assertThrows(IllegalArgumentException.class, () ->
                new Defaults(PollingType.ADAPTIVE, Duration.ZERO, Duration.ZERO, Duration.ofSeconds(1), Duration.ofSeconds(2), -1.0)
        );
    }

    @Test
    @DisplayName("UT PollingProperties.applyDefaults() should take all FIXED values from defaults when fields are null")
    void applyDefaults_whenFixedTypeAndNullFields_thenTakesFromDefaults() {
        PollingProperties properties = new PollingProperties();

        Defaults defaults = new Defaults(
                PollingType.FIXED,
                Duration.ofSeconds(10),
                Duration.ofSeconds(20),
                Duration.ZERO,
                Duration.ZERO,
                1.0
        );

        properties.applyDefaults(defaults);

        assertEquals(PollingType.FIXED, properties.getType());
        assertEquals(Duration.ofSeconds(10), properties.getInitialDelay());
        assertEquals(Duration.ofSeconds(20), properties.getFixedDelay());
        assertEquals(Duration.ZERO, properties.getMinFixedDelay());
        assertEquals(Duration.ZERO, properties.getMaxFixedDelay());
        assertTrue(Double.isNaN(properties.getMultiplier()));
    }

    @Test
    @DisplayName("UT PollingProperties.applyDefaults() should keep existing FIXED values and reset ADAPTIVE fields")
    void applyDefaults_whenFixedTypeAndExistingFields_thenKeepsValuesAndResetsOthers() {
        PollingProperties properties = new PollingProperties();
        properties.setType(PollingType.FIXED);
        properties.setInitialDelay(Duration.ofSeconds(5));
        properties.setFixedDelay(Duration.ofSeconds(15));

        properties.setMinFixedDelay(Duration.ofSeconds(100));
        properties.setMaxFixedDelay(Duration.ofSeconds(200));
        properties.setMultiplier(5.0);

        Defaults defaults = new Defaults(
                PollingType.FIXED,
                Duration.ofSeconds(10),
                Duration.ofSeconds(20),
                Duration.ZERO,
                Duration.ZERO,
                1.0
        );

        properties.applyDefaults(defaults);

        assertEquals(PollingType.FIXED, properties.getType());
        assertEquals(Duration.ofSeconds(5), properties.getInitialDelay());
        assertEquals(Duration.ofSeconds(15), properties.getFixedDelay());

        assertEquals(Duration.ZERO, properties.getMinFixedDelay());
        assertEquals(Duration.ZERO, properties.getMaxFixedDelay());
        assertTrue(Double.isNaN(properties.getMultiplier()));
    }

    @Test
    @DisplayName("UT PollingProperties.applyDefaults() should take all ADAPTIVE values from defaults when fields are null")
    void applyDefaults_whenAdaptiveTypeAndNullFields_thenTakesFromDefaults() {
        PollingProperties properties = new PollingProperties();

        Defaults defaults = new Defaults(
                PollingType.ADAPTIVE,
                Duration.ofSeconds(10),
                Duration.ZERO,
                Duration.ofSeconds(1),
                Duration.ofSeconds(5),
                2.0
        );

        properties.applyDefaults(defaults);

        assertEquals(PollingType.ADAPTIVE, properties.getType());
        assertEquals(Duration.ofSeconds(10), properties.getInitialDelay());
        assertEquals(Duration.ZERO, properties.getFixedDelay());
        assertEquals(Duration.ofSeconds(1), properties.getMinFixedDelay());
        assertEquals(Duration.ofSeconds(5), properties.getMaxFixedDelay());
        assertEquals(2.0, properties.getMultiplier());
    }

    @Test
    @DisplayName("UT PollingProperties.applyDefaults() should keep existing ADAPTIVE values and reset FIXED fields")
    void applyDefaults_whenAdaptiveTypeAndExistingFields_thenKeepsValuesAndResetsOthers() {
        PollingProperties properties = new PollingProperties();
        properties.setType(PollingType.ADAPTIVE);
        properties.setInitialDelay(Duration.ofSeconds(5));
        properties.setMinFixedDelay(Duration.ofSeconds(2));
        properties.setMaxFixedDelay(Duration.ofSeconds(8));
        properties.setMultiplier(3.0);

        properties.setFixedDelay(Duration.ofSeconds(100));

        Defaults defaults = new Defaults(
                PollingType.ADAPTIVE,
                Duration.ofSeconds(10),
                Duration.ZERO,
                Duration.ofSeconds(1),
                Duration.ofSeconds(5),
                2.0
        );

        properties.applyDefaults(defaults);

        assertEquals(PollingType.ADAPTIVE, properties.getType());
        assertEquals(Duration.ofSeconds(5), properties.getInitialDelay());

        assertEquals(Duration.ZERO, properties.getFixedDelay());

        assertEquals(Duration.ofSeconds(2), properties.getMinFixedDelay());
        assertEquals(Duration.ofSeconds(8), properties.getMaxFixedDelay());
        assertEquals(3.0, properties.getMultiplier());
    }

    @Test
    @DisplayName("UT PollingProperties.validate() should throw when type is FIXED and initialDelay is null")
    void validate_fixedType_throwsIfInitialDelayNull() {
        PollingProperties properties = new PollingProperties();
        properties.setType(PollingType.FIXED);
        properties.setFixedDelay(Duration.ofSeconds(1));

        assertThrows(NullPointerException.class, properties::validate);
    }

    @Test
    @DisplayName("UT PollingProperties.validate() should throw when type is FIXED and fixedDelay is null")
    void validate_fixedType_throwsIfFixedDelayNull() {
        PollingProperties properties = new PollingProperties();
        properties.setType(PollingType.FIXED);
        properties.setInitialDelay(Duration.ofSeconds(1));

        assertThrows(NullPointerException.class, properties::validate);
    }

    @Test
    @DisplayName("UT PollingProperties.validate() should succeed when type is FIXED and properties are valid")
    void validate_fixedType_success() {
        PollingProperties properties = new PollingProperties();
        properties.setType(PollingType.FIXED);
        properties.setInitialDelay(Duration.ofSeconds(1));
        properties.setFixedDelay(Duration.ofSeconds(1));

        assertDoesNotThrow(properties::validate);
    }

    @Test
    @DisplayName("UT PollingProperties.validate() should throw when type is ADAPTIVE and initialDelay is null")
    void validate_adaptiveType_throwsIfInitialDelayNull() {
        PollingProperties properties = new PollingProperties();
        properties.setType(PollingType.ADAPTIVE);
        properties.setMinFixedDelay(Duration.ofSeconds(1));
        properties.setMaxFixedDelay(Duration.ofSeconds(2));
        properties.setMultiplier(1.5);

        assertThrows(NullPointerException.class, properties::validate);
    }

    @Test
    @DisplayName("UT PollingProperties.validate() should throw when type is ADAPTIVE and minFixedDelay is null")
    void validate_adaptiveType_throwsIfMinFixedDelayNull() {
        PollingProperties properties = new PollingProperties();
        properties.setType(PollingType.ADAPTIVE);
        properties.setInitialDelay(Duration.ofSeconds(1));
        properties.setMaxFixedDelay(Duration.ofSeconds(2));
        properties.setMultiplier(1.5);

        assertThrows(NullPointerException.class, properties::validate);
    }

    @Test
    @DisplayName("UT PollingProperties.validate() should throw when type is ADAPTIVE and maxFixedDelay is null")
    void validate_adaptiveType_throwsIfMaxFixedDelayNull() {
        PollingProperties properties = new PollingProperties();
        properties.setType(PollingType.ADAPTIVE);
        properties.setInitialDelay(Duration.ofSeconds(1));
        properties.setMinFixedDelay(Duration.ofSeconds(1));
        properties.setMultiplier(1.5);

        assertThrows(NullPointerException.class, properties::validate);
    }

    @Test
    @DisplayName("UT PollingProperties.validate() should throw when type is ADAPTIVE and minFixedDelay is greater than maxFixedDelay")
    void validate_adaptiveType_throwsIfMinGreaterThanMax() {
        PollingProperties properties = new PollingProperties();
        properties.setType(PollingType.ADAPTIVE);
        properties.setInitialDelay(Duration.ofSeconds(1));
        properties.setMinFixedDelay(Duration.ofSeconds(5));
        properties.setMaxFixedDelay(Duration.ofSeconds(2));
        properties.setMultiplier(1.5);

        assertThrows(IllegalArgumentException.class, properties::validate);
    }

    @Test
    @DisplayName("UT PollingProperties.validate() should throw when type is ADAPTIVE and multiplier is null")
    void validate_adaptiveType_throwsIfMultiplierNull() {
        PollingProperties properties = new PollingProperties();
        properties.setType(PollingType.ADAPTIVE);
        properties.setInitialDelay(Duration.ofSeconds(1));
        properties.setMinFixedDelay(Duration.ofSeconds(1));
        properties.setMaxFixedDelay(Duration.ofSeconds(2));

        assertThrows(NullPointerException.class, properties::validate);
    }

    @Test
    @DisplayName("UT PollingProperties.validate() should throw when type is ADAPTIVE and multiplier is zero or negative")
    void validate_adaptiveType_throwsIfMultiplierZeroOrNegative() {
        PollingProperties properties = new PollingProperties();
        properties.setType(PollingType.ADAPTIVE);
        properties.setInitialDelay(Duration.ofSeconds(1));
        properties.setMinFixedDelay(Duration.ofSeconds(1));
        properties.setMaxFixedDelay(Duration.ofSeconds(2));

        properties.setMultiplier(0.0);
        assertThrows(IllegalArgumentException.class, properties::validate);

        properties.setMultiplier(-1.0);
        assertThrows(IllegalArgumentException.class, properties::validate);
    }

    @Test
    @DisplayName("UT PollingProperties.validate() should succeed when type is ADAPTIVE and properties are valid")
    void validate_adaptiveType_success() {
        PollingProperties properties = new PollingProperties();
        properties.setType(PollingType.ADAPTIVE);
        properties.setInitialDelay(Duration.ofSeconds(1));
        properties.setMinFixedDelay(Duration.ofSeconds(1));
        properties.setMaxFixedDelay(Duration.ofSeconds(2));
        properties.setMultiplier(1.5);

        assertDoesNotThrow(properties::validate);
    }

    @Test
    @DisplayName("UT PollingProperties.validate() should throw when type is null")
    void validate_nullType_throwsNullPointerException() {
        PollingProperties properties = new PollingProperties();

        assertThrows(NullPointerException.class, properties::validate);
    }

    @Test
    @DisplayName("UT PollingProperties.toString() should contain all properties")
    void toString_containsFields() {
        PollingProperties properties = new PollingProperties();
        properties.setType(PollingType.FIXED);
        properties.setInitialDelay(Duration.ofSeconds(1));

        String str = properties.toString();

        assertTrue(str.contains("type=FIXED"));
        assertTrue(str.contains("initialDelay=PT1S"));
    }

    @Test
    @DisplayName("UT Defaults constructor should throw when type is null")
    void defaults_constructor_throwsIfTypeNull() {
        assertThrows(NullPointerException.class, () ->
                new Defaults(null, Duration.ZERO, Duration.ZERO, Duration.ZERO, Duration.ZERO, 1.0)
        );
    }

    @Test
    @DisplayName("UT Defaults.ofPollingProperties() should create correct Defaults from valid PollingProperties")
    void defaults_ofPollingProperties_createsCorrectDefaults() {
        PollingProperties properties = new PollingProperties();
        properties.setType(PollingType.ADAPTIVE);
        properties.setInitialDelay(Duration.ofSeconds(1));
        properties.setFixedDelay(Duration.ofSeconds(1));
        properties.setMinFixedDelay(Duration.ofSeconds(2));
        properties.setMaxFixedDelay(Duration.ofSeconds(5));
        properties.setMultiplier(2.0);

        Defaults defaults = Defaults.ofPollingProperties(properties);

        assertEquals(PollingType.ADAPTIVE, defaults.type());
        assertEquals(Duration.ofSeconds(1), defaults.initialDelay());
        assertEquals(Duration.ofSeconds(2), defaults.minFixedDelay());
        assertEquals(Duration.ofSeconds(1), defaults.fixedDelay());
        assertEquals(2.0, defaults.multiplier());
    }

    @Test
    @DisplayName("UT Defaults.ofPollingProperties() should throw when PollingProperties is invalid")
    void defaults_ofPollingProperties_throwsIfInvalid() {
        PollingProperties properties = new PollingProperties();
        properties.setType(PollingType.FIXED);

        assertThrows(NullPointerException.class, () -> Defaults.ofPollingProperties(properties));
    }

    @Test
    @DisplayName("UT PollingProperties.toString() should contain all fields and values")
    void toString_containsAllFields() {
        PollingProperties properties = new PollingProperties();
        properties.setType(PollingType.ADAPTIVE);
        properties.setInitialDelay(Duration.ofSeconds(1));
        properties.setFixedDelay(Duration.ofSeconds(2));
        properties.setMinFixedDelay(Duration.ofSeconds(3));
        properties.setMaxFixedDelay(Duration.ofSeconds(4));
        properties.setMultiplier(1.5);

        String result = properties.toString();

        assertTrue(result.contains("type=ADAPTIVE"));
        assertTrue(result.contains("initialDelay=PT1S"));
        assertTrue(result.contains("fixedDelay=PT2S"));
        assertTrue(result.contains("minFixedDelay=PT3S"));
        assertTrue(result.contains("maxFixedDelay=PT4S"));
        assertTrue(result.contains("multiplier=1.5"));
    }

    @Test
    @DisplayName("UT PollingProperties getters and setters should work correctly")
    void gettersAndSetters_shouldWorkCorrectly() {
        PollingProperties properties = new PollingProperties();

        properties.setType(PollingType.FIXED);
        assertEquals(PollingType.FIXED, properties.getType());

        properties.setInitialDelay(Duration.ofSeconds(10));
        assertEquals(Duration.ofSeconds(10), properties.getInitialDelay());

        properties.setFixedDelay(Duration.ofSeconds(20));
        assertEquals(Duration.ofSeconds(20), properties.getFixedDelay());

        properties.setMinFixedDelay(Duration.ofSeconds(30));
        assertEquals(Duration.ofSeconds(30), properties.getMinFixedDelay());

        properties.setMaxFixedDelay(Duration.ofSeconds(40));
        assertEquals(Duration.ofSeconds(40), properties.getMaxFixedDelay());

        properties.setMultiplier(2.5);
        assertEquals(2.5, properties.getMultiplier());
    }

    @Nested
    class DefaultsFactoryMethodsTest {

        @Test
        @DisplayName("UT ofFixed() should create Defaults successfully with zeroed adaptive fields")
        void ofFixed_success() {
            Defaults result = Defaults.ofFixed(PollingType.FIXED, Duration.ofSeconds(10), Duration.ofSeconds(5));

            assertEquals(PollingType.FIXED, result.type());
            assertEquals(Duration.ofSeconds(10), result.initialDelay());
            assertEquals(Duration.ofSeconds(5), result.fixedDelay());
            assertEquals(Duration.ZERO, result.minFixedDelay());
            assertEquals(Duration.ZERO, result.maxFixedDelay());
            assertTrue(Double.isNaN(result.multiplier()));
        }

        @Test
        @DisplayName("UT ofFixed() should throw NullPointerException when type is null")
        void ofFixed_whenTypeIsNull_throwsNPE() {
            assertThrows(NullPointerException.class, () ->
                    Defaults.ofFixed(null, Duration.ofSeconds(1), Duration.ofSeconds(2))
            );
        }

        @Test
        @DisplayName("UT ofFixed() should throw NullPointerException when initialDelay is null")
        void ofFixed_whenInitialDelayIsNull_throwsNPE() {
            assertThrows(NullPointerException.class, () ->
                    Defaults.ofFixed(PollingType.FIXED, null, Duration.ofSeconds(2))
            );
        }

        @Test
        @DisplayName("UT ofFixed() should throw NullPointerException when fixedDelay is null")
        void ofFixed_whenFixedDelayIsNull_throwsNPE() {
            assertThrows(NullPointerException.class, () ->
                    Defaults.ofFixed(PollingType.FIXED, Duration.ofSeconds(1), null)
            );
        }

        @Test
        @DisplayName("UT ofAdaptive() should create Defaults successfully with zeroed fixedDelay")
        void ofAdaptive_success() {
            Defaults result = Defaults.ofAdaptive(
                    PollingType.ADAPTIVE,
                    Duration.ofSeconds(10),
                    Duration.ofSeconds(2),
                    Duration.ofSeconds(5),
                    2.0
            );

            assertEquals(PollingType.ADAPTIVE, result.type());
            assertEquals(Duration.ofSeconds(10), result.initialDelay());
            assertEquals(Duration.ZERO, result.fixedDelay());
            assertEquals(Duration.ofSeconds(2), result.minFixedDelay());
            assertEquals(Duration.ofSeconds(5), result.maxFixedDelay());
            assertEquals(2.0, result.multiplier());
        }

        @Test
        @DisplayName("UT ofAdaptive() should throw NullPointerException when type is null")
        void ofAdaptive_whenTypeIsNull_throwsNPE() {
            assertThrows(NullPointerException.class, () ->
                    Defaults.ofAdaptive(null, Duration.ofSeconds(1), Duration.ofSeconds(1), Duration.ofSeconds(2), 1.5)
            );
        }

        @Test
        @DisplayName("UT ofAdaptive() should throw NullPointerException when initialDelay is null")
        void ofAdaptive_whenInitialDelayIsNull_throwsNPE() {
            assertThrows(NullPointerException.class, () ->
                    Defaults.ofAdaptive(PollingType.ADAPTIVE, null, Duration.ofSeconds(1), Duration.ofSeconds(2), 1.5)
            );
        }

        @Test
        @DisplayName("UT ofAdaptive() should throw NullPointerException when minFixedDelay is null")
        void ofAdaptive_whenMinFixedDelayIsNull_throwsNPE() {
            assertThrows(NullPointerException.class, () ->
                    Defaults.ofAdaptive(PollingType.ADAPTIVE, Duration.ofSeconds(1), null, Duration.ofSeconds(2), 1.5)
            );
        }

        @Test
        @DisplayName("UT ofAdaptive() should throw NullPointerException when maxFixedDelay is null")
        void ofAdaptive_whenMaxFixedDelayIsNull_throwsNPE() {
            assertThrows(NullPointerException.class, () ->
                    Defaults.ofAdaptive(PollingType.ADAPTIVE, Duration.ofSeconds(1), Duration.ofSeconds(1), null, 1.5)
            );
        }

        @Test
        @DisplayName("UT ofAdaptive() should throw IllegalArgumentException when minFixedDelay is greater than maxFixedDelay")
        void ofAdaptive_whenMinGreaterThanMax_throwsIAE() {
            assertThrows(IllegalArgumentException.class, () ->
                    Defaults.ofAdaptive(PollingType.ADAPTIVE, Duration.ofSeconds(1), Duration.ofSeconds(5), Duration.ofSeconds(2), 1.5)
            );
        }

        @Test
        @DisplayName("UT ofAdaptive() should throw NullPointerException when multiplier is null")
        void ofAdaptive_whenMultiplierIsNull_throwsNPE() {
            assertThrows(NullPointerException.class, () ->
                    Defaults.ofAdaptive(PollingType.ADAPTIVE, Duration.ofSeconds(1), Duration.ofSeconds(1), Duration.ofSeconds(2), null)
            );
        }

        @Test
        @DisplayName("UT ofAdaptive() should throw IllegalArgumentException when multiplier is zero")
        void ofAdaptive_whenMultiplierIsZero_throwsIAE() {
            assertThrows(IllegalArgumentException.class, () ->
                    Defaults.ofAdaptive(PollingType.ADAPTIVE, Duration.ofSeconds(1), Duration.ofSeconds(1), Duration.ofSeconds(2), 0.0)
            );
        }

        @Test
        @DisplayName("UT ofAdaptive() should throw IllegalArgumentException when multiplier is negative")
        void ofAdaptive_whenMultiplierIsNegative_throwsIAE() {
            assertThrows(IllegalArgumentException.class, () ->
                    Defaults.ofAdaptive(PollingType.ADAPTIVE, Duration.ofSeconds(1), Duration.ofSeconds(1), Duration.ofSeconds(2), -1.5)
            );
        }
    }

    @Nested
    class GrowthAndDelayValidationTests {

        @ParameterizedTest
        @ValueSource(longs = {0, -1})
        @DisplayName("UT applyDefaults() on adaptive polling when min-fixed-delay is 0 or negative should throw naming it")
        void applyDefaults_whenMinFixedDelayIsNotPositive_shouldThrow(long millis) {
            // given
            PollingProperties tested = adaptive(Duration.ZERO, Duration.ofMillis(millis), 2.0);

            // when / then
            assertThatThrownBy(() -> tested.applyDefaults(ADAPTIVE_DEFAULTS))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("'min-fixed-delay' cannot be negative or 0");
        }

        @ParameterizedTest
        @ValueSource(doubles = {1.0, 0.5, 0.0, -1.0, Double.NaN})
        @DisplayName("UT applyDefaults() on adaptive polling when multiplier is 1 or less should throw naming it")
        void applyDefaults_whenMultiplierIsNotAboveOne_shouldThrow(double multiplier) {
            // given
            PollingProperties tested = adaptive(Duration.ZERO, Duration.ofMillis(100), multiplier);

            // when / then
            assertThatThrownBy(() -> tested.applyDefaults(ADAPTIVE_DEFAULTS))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("'multiplier' must be greater than 1");
        }

        @Test
        @DisplayName("UT applyDefaults() on adaptive polling when multiplier is just above 1 should accept it")
        void applyDefaults_whenMultiplierIsJustAboveOne_shouldAccept() {
            // given
            PollingProperties tested = adaptive(Duration.ZERO, Duration.ofMillis(100), 1.01);

            // when / then
            assertThatCode(() -> tested.applyDefaults(ADAPTIVE_DEFAULTS)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("UT applyDefaults() when switching to adaptive polling over fixed defaults should not inherit their placeholders")
        void applyDefaults_whenSwitchingToAdaptiveOverFixedDefaults_shouldRequireAdaptiveDelays() {
            // given
            PollingProperties tested = new PollingProperties();
            tested.setType(PollingType.ADAPTIVE);

            // when / then
            assertThatThrownBy(() -> tested.applyDefaults(FIXED_DEFAULTS))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("'min-fixed-delay' cannot be null");
        }

        @Test
        @DisplayName("UT Defaults.ofAdaptive() when multiplier is 1 should throw naming it")
        void ofAdaptive_whenMultiplierIsOne_shouldThrow() {
            assertThatThrownBy(() -> Defaults.ofAdaptive(
                    PollingType.ADAPTIVE, Duration.ZERO, Duration.ofMillis(100), Duration.ofSeconds(1), 1.0
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("'multiplier' must be greater than 1");
        }

        private static final Defaults FIXED_DEFAULTS =
                Defaults.ofFixed(PollingType.FIXED, Duration.ofSeconds(1), Duration.ofSeconds(5));
        private static final Defaults ADAPTIVE_DEFAULTS = Defaults.ofAdaptive(
                PollingType.ADAPTIVE, Duration.ofSeconds(1), Duration.ofMillis(100), Duration.ofSeconds(10), 2.0
        );

        private static PollingProperties adaptive(Duration initialDelay, Duration minFixedDelay, double multiplier) {
            PollingProperties properties = new PollingProperties();
            properties.setType(PollingType.ADAPTIVE);
            properties.setInitialDelay(initialDelay);
            properties.setMinFixedDelay(minFixedDelay);
            properties.setMaxFixedDelay(Duration.ofSeconds(10));
            properties.setMultiplier(multiplier);
            return properties;
        }
    }
}
