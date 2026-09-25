package io.github.dmitriyiliyov.oncebox.starter.publisher;

import io.github.dmitriyiliyov.oncebox.starter.OutboxProperties;
import io.github.dmitriyiliyov.oncebox.starter.PollingType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

class OutboxPublisherPropertiesDefaultsUnitTests {

    @Nested
    @DisplayName("Defaults Class Tests")
    class DefaultsTests {

        @Test
        @DisplayName("UT applyDefaults() when all parameters are null should assign global defaults")
        void applyDefaults_whenAllNull_shouldAssignDefaults() {
            // given
            OutboxPublisherProperties.EventProperties.Defaults defaults = new OutboxPublisherProperties.EventProperties.Defaults();
            defaults.setBatchSize(null);
            defaults.setMaxRetries(null);
            defaults.setPolling(null);
            defaults.setBackoff(null);

            // when
            defaults.applyDefaults();

            // then
            assertAll(
                    () -> assertThat(defaults.getBatchSize()).isEqualTo(200),
                    () -> assertThat(defaults.getMaxRetries()).isEqualTo(3),
                    () -> assertThat(defaults.getPolling()).isNotNull(),
                    () -> assertThat(defaults.getPolling().getType()).isEqualTo(PollingType.ADAPTIVE),
                    () -> assertThat(defaults.getPolling().getInitialDelay()).isEqualTo(Duration.ofMinutes(5)),
                    () -> assertThat(defaults.getPolling().getFixedDelay()).isEqualTo(Duration.ZERO),
                    () -> assertThat(defaults.getPolling().getMinFixedDelay()).isEqualTo(Duration.ofMillis(250)),
                    () -> assertThat(defaults.getPolling().getMaxFixedDelay()).isEqualTo(Duration.ofMinutes(1)),
                    () -> assertThat(defaults.getPolling().getMultiplier()).isEqualTo(1.5),
                    () -> assertThat(defaults.getBackoff()).isNotNull(),
                    () -> assertThat(defaults.getBackoff().isEnabled()).isTrue()
            );
        }

        @Test
        @DisplayName("UT applyDefaults() should fallback to default when batchSize is invalid (<= 0)")
        void applyDefaults_whenBatchSizeInvalid_shouldAssignDefault() {
            // given
            OutboxPublisherProperties.EventProperties.Defaults defaults = new OutboxPublisherProperties.EventProperties.Defaults();
            defaults.setBatchSize(0);
            defaults.setMaxRetries(5);

            // when
            defaults.applyDefaults();

            // then
            assertThat(defaults.getBatchSize()).isEqualTo(200);
            assertThat(defaults.getMaxRetries()).isEqualTo(5);
        }

        @Test
        @DisplayName("UT applyDefaults() should fallback to default when maxRetries is invalid (< 0)")
        void applyDefaults_whenMaxRetriesInvalid_shouldAssignDefault() {
            // given
            OutboxPublisherProperties.EventProperties.Defaults defaults = new OutboxPublisherProperties.EventProperties.Defaults();
            defaults.setMaxRetries(-1);
            defaults.setBatchSize(50);

            // when
            defaults.applyDefaults();

            // then
            assertThat(defaults.getMaxRetries()).isEqualTo(3);
            assertThat(defaults.getBatchSize()).isEqualTo(50);
        }

        @Test
        @DisplayName("UT applyDefaults() should retain valid custom values")
        void applyDefaults_whenValidValues_shouldKeepProvided() {
            // given
            OutboxPublisherProperties.EventProperties.Defaults defaults = new OutboxPublisherProperties.EventProperties.Defaults();
            defaults.setBatchSize(100);
            defaults.setMaxRetries(7);

            OutboxProperties.PollingProperties customPolling = new OutboxProperties.PollingProperties();
            customPolling.setType(PollingType.FIXED);
            customPolling.setInitialDelay(Duration.ofSeconds(10));
            customPolling.setFixedDelay(Duration.ofSeconds(5));
            defaults.setPolling(customPolling);

            OutboxPublisherProperties.BackoffProperties customBackoff = new OutboxPublisherProperties.BackoffProperties();
            customBackoff.setEnabled(false);
            defaults.setBackoff(customBackoff);

            // when
            defaults.applyDefaults();

            // then
            assertAll(
                    () -> assertThat(defaults.getBatchSize()).isEqualTo(100),
                    () -> assertThat(defaults.getMaxRetries()).isEqualTo(7),
                    () -> assertThat(defaults.getPolling().getType()).isEqualTo(PollingType.FIXED),
                    () -> assertThat(defaults.getPolling().getInitialDelay()).isEqualTo(Duration.ofSeconds(10)),
                    () -> assertThat(defaults.getPolling().getFixedDelay()).isEqualTo(Duration.ofSeconds(5)),
                    () -> assertThat(defaults.getBackoff().isEnabled()).isFalse()
            );
        }

        @Test
        @DisplayName("UT getPoolingDefaults() should correctly extract defaults from current pollingDefaults properties")
        void getPoolingDefaults_shouldExtractCorrectly() {
            // given
            OutboxPublisherProperties.EventProperties.Defaults defaults = new OutboxPublisherProperties.EventProperties.Defaults();
            OutboxProperties.PollingProperties customPolling = new OutboxProperties.PollingProperties();
            customPolling.setType(PollingType.FIXED);
            customPolling.setInitialDelay(Duration.ofSeconds(10));
            customPolling.setFixedDelay(Duration.ofSeconds(5));
            customPolling.setMinFixedDelay(Duration.ofMillis(1));
            customPolling.setMaxFixedDelay(Duration.ofSeconds(1));
            customPolling.setMultiplier(2.0);
            defaults.setPolling(customPolling);

            // when
            OutboxProperties.PollingProperties.Defaults extracted = defaults.getPoolingDefaults();

            // then
            assertAll(
                    () -> assertThat(extracted.type()).isEqualTo(PollingType.FIXED),
                    () -> assertThat(extracted.initialDelay()).isEqualTo(Duration.ofSeconds(10)),
                    () -> assertThat(extracted.fixedDelay()).isEqualTo(Duration.ofSeconds(5)),
                    () -> assertThat(extracted.minFixedDelay()).isEqualTo(Duration.ofMillis(1)),
                    () -> assertThat(extracted.maxFixedDelay()).isEqualTo(Duration.ofSeconds(1)),
                    () -> assertThat(extracted.multiplier()).isEqualTo(2.0)
            );
        }
    }

    @Nested
    @DisplayName("EventProperties Class Tests")
    class EventPropertiesTests {

        private OutboxPublisherProperties.EventProperties.Defaults configuredDefaults;

        @BeforeEach
        void setUp() {
            configuredDefaults = new OutboxPublisherProperties.EventProperties.Defaults();
            configuredDefaults.setBatchSize(150);
            configuredDefaults.setMaxRetries(5);

            OutboxProperties.PollingProperties defaultPolling = new OutboxProperties.PollingProperties();
            defaultPolling.setType(PollingType.FIXED);
            defaultPolling.setInitialDelay(Duration.ofSeconds(10));
            defaultPolling.setFixedDelay(Duration.ofSeconds(20));
            configuredDefaults.setPolling(defaultPolling);

            OutboxPublisherProperties.BackoffProperties defaultBackoff = new OutboxPublisherProperties.BackoffProperties();
            defaultBackoff.setEnabled(true);
            defaultBackoff.setDelay(Duration.ofSeconds(2));
            defaultBackoff.setMultiplier(2.5);
            configuredDefaults.setBackoff(defaultBackoff);

            configuredDefaults.applyDefaults();
        }

        @Test
        @DisplayName("UT applyDefaults() should throw IllegalArgumentException when eventType is null or blank")
        void applyDefaults_whenEventTypeInvalid_shouldThrow() {
            OutboxPublisherProperties.EventProperties event = new OutboxPublisherProperties.EventProperties();
            event.setTopic("valid-topic");

            event.setEventType(null);
            assertThatThrownBy(() -> event.applyDefaults(configuredDefaults))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("'events' key cannot be null");

            event.setEventType("   ");
            assertThatThrownBy(() -> event.applyDefaults(configuredDefaults))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("'events' key cannot be blank");
        }

        @Test
        @DisplayName("UT applyDefaults() should throw IllegalArgumentException when topic is null or blank")
        void applyDefaults_whenTopicInvalid_shouldThrow() {
            OutboxPublisherProperties.EventProperties event = new OutboxPublisherProperties.EventProperties();
            event.setEventType("valid-event");

            event.setTopic(null);
            assertThatThrownBy(() -> event.applyDefaults(configuredDefaults))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("'topic' cannot be null");

            event.setTopic("   ");
            assertThatThrownBy(() -> event.applyDefaults(configuredDefaults))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("'topic' cannot be blank");
        }

        @Test
        @DisplayName("UT applyDefaults() should inherit batchSize and maxRetries from Defaults when null")
        void applyDefaults_whenScalarsNull_shouldInheritFromDefaults() {
            // given
            OutboxPublisherProperties.EventProperties event = new OutboxPublisherProperties.EventProperties();
            event.setEventType("event");
            event.setTopic("topic");
            event.setBatchSize(null);
            event.setMaxRetries(null);

            // when
            event.applyDefaults(configuredDefaults);

            // then
            assertThat(event.getBatchSize()).isEqualTo(150);
            assertThat(event.getMaxRetries()).isEqualTo(5);
        }

        @Test
        @DisplayName("UT applyDefaults() should inherit entire pollingDefaults configuration when event pollingDefaults is null")
        void applyDefaults_whenPollingNull_shouldInheritFromDefaults() {
            // given
            OutboxPublisherProperties.EventProperties event = new OutboxPublisherProperties.EventProperties();
            event.setEventType("event");
            event.setTopic("topic");
            event.setPolling(null);

            // when
            event.applyDefaults(configuredDefaults);

            // then
            assertAll(
                    () -> assertThat(event.getPolling().getType()).isEqualTo(PollingType.FIXED),
                    () -> assertThat(event.getPolling().getInitialDelay()).isEqualTo(Duration.ofSeconds(10)),
                    () -> assertThat(event.getPolling().getFixedDelay()).isEqualTo(Duration.ofSeconds(20))
            );
        }

        @Test
        @DisplayName("UT applyDefaults() should merge partial pollingDefaults properties with defaults")
        void applyDefaults_whenPollingPartiallyProvided_shouldMergeWithDefaults() {
            // given
            OutboxPublisherProperties.EventProperties event = new OutboxPublisherProperties.EventProperties();
            event.setEventType("event");
            event.setTopic("topic");

            OutboxProperties.PollingProperties eventPolling = new OutboxProperties.PollingProperties();
            eventPolling.setFixedDelay(Duration.ofSeconds(99));
            event.setPolling(eventPolling);

            // when
            event.applyDefaults(configuredDefaults);

            // then
            assertAll(
                    () -> assertThat(event.getPolling().getType()).isEqualTo(PollingType.FIXED), // from defaults
                    () -> assertThat(event.getPolling().getInitialDelay()).isEqualTo(Duration.ofSeconds(10)), // from defaults
                    () -> assertThat(event.getPolling().getFixedDelay()).isEqualTo(Duration.ofSeconds(99)) // overridden
            );
        }

        @Nested
        @DisplayName("Backoff Logic Tests")
        class BackoffTests {

            @Test
            @DisplayName("UT applyDefaults() should inherit entire backoff when event backoff is null")
            void applyDefaults_whenBackoffNull_shouldInheritDefaults() {
                OutboxPublisherProperties.EventProperties event = new OutboxPublisherProperties.EventProperties();
                event.setEventType("e");
                event.setTopic("t");
                event.setBackoff(null);

                event.applyDefaults(configuredDefaults);

                assertThat(event.getBackoff().isEnabled()).isTrue();
                assertThat(event.getBackoff().getDelay()).isEqualTo(Duration.ofSeconds(2));
                assertThat(event.getBackoff().getMultiplier()).isEqualTo(2.5);
            }

            @Test
            @DisplayName("UT applyDefaults() should reset backoff when explicitly disabled on the event")
            void applyDefaults_whenBackoffDisabled_shouldReset() {
                OutboxPublisherProperties.EventProperties event = new OutboxPublisherProperties.EventProperties();
                event.setEventType("e");
                event.setTopic("t");

                OutboxPublisherProperties.BackoffProperties disabledBackoff = new OutboxPublisherProperties.BackoffProperties();
                disabledBackoff.setEnabled(false);
                disabledBackoff.setDelay(Duration.ofSeconds(100));
                event.setBackoff(disabledBackoff);

                event.applyDefaults(configuredDefaults);

                assertThat(event.getBackoff().isEnabled()).isFalse();
                assertThat(event.getBackoff().getDelay()).isEqualTo(Duration.ofSeconds(0));
                assertThat(event.getBackoff().getMultiplier()).isEqualTo(1.0);
            }

            @Test
            @DisplayName("UT applyDefaults() should merge missing delay and multiplier from defaults")
            void applyDefaults_whenBackoffFieldsNull_shouldMergeFromDefaults() {
                OutboxPublisherProperties.EventProperties event = new OutboxPublisherProperties.EventProperties();
                event.setEventType("e");
                event.setTopic("t");

                OutboxPublisherProperties.BackoffProperties partialBackoff = new OutboxPublisherProperties.BackoffProperties();
                partialBackoff.setEnabled(true);
                partialBackoff.setDelay(null);
                partialBackoff.setMultiplier(null);
                event.setBackoff(partialBackoff);

                event.applyDefaults(configuredDefaults);

                assertThat(event.getBackoff().getDelay()).isEqualTo(Duration.ofSeconds(2));
                assertThat(event.getBackoff().getMultiplier()).isEqualTo(2.5);
            }

            @Test
            @DisplayName("UT applyDefaults() should fallback to default multiplier if event multiplier is < 1")
            void applyDefaults_whenBackoffMultiplierInvalid_shouldFallbackToDefault() {
                OutboxPublisherProperties.EventProperties event = new OutboxPublisherProperties.EventProperties();
                event.setEventType("e");
                event.setTopic("t");

                OutboxPublisherProperties.BackoffProperties invalidMultiplierBackoff = new OutboxPublisherProperties.BackoffProperties();
                invalidMultiplierBackoff.setEnabled(true);
                invalidMultiplierBackoff.setMultiplier(0.5);
                invalidMultiplierBackoff.setDelay(Duration.ofSeconds(15));
                event.setBackoff(invalidMultiplierBackoff);

                event.applyDefaults(configuredDefaults);

                assertThat(event.getBackoff().getDelay()).isEqualTo(Duration.ofSeconds(15));
                assertThat(event.getBackoff().getMultiplier()).isEqualTo(2.5);
            }

            @Test
            @DisplayName("UT applyDefaults() should retain valid custom backoff values")
            void applyDefaults_whenBackoffValid_shouldRetain() {
                OutboxPublisherProperties.EventProperties event = new OutboxPublisherProperties.EventProperties();
                event.setEventType("e");
                event.setTopic("t");

                OutboxPublisherProperties.BackoffProperties customBackoff = new OutboxPublisherProperties.BackoffProperties();
                customBackoff.setEnabled(true);
                customBackoff.setDelay(Duration.ofSeconds(8));
                customBackoff.setMultiplier(4.0);
                event.setBackoff(customBackoff);

                event.applyDefaults(configuredDefaults);

                assertThat(event.getBackoff().getDelay()).isEqualTo(Duration.ofSeconds(8));
                assertThat(event.getBackoff().getMultiplier()).isEqualTo(4.0);
            }
        }

        @Test
        @DisplayName("UT EventProperties delegation methods should return values from nested objects")
        void delegationMethods_shouldDelegateCorrectly() {
            // given
            OutboxPublisherProperties.EventProperties event = new OutboxPublisherProperties.EventProperties();
            event.setEventType("e");
            event.setTopic("t");

            OutboxProperties.PollingProperties polling = new OutboxProperties.PollingProperties();
            polling.setType(PollingType.ADAPTIVE);
            polling.setInitialDelay(Duration.ofSeconds(11));
            polling.setMinFixedDelay(Duration.ofSeconds(22));
            polling.setMaxFixedDelay(Duration.ofSeconds(33));
            polling.setFixedDelay(Duration.ofSeconds(44));
            polling.setMultiplier(1.8);
            event.setPolling(polling);

            OutboxPublisherProperties.BackoffProperties backoff = new OutboxPublisherProperties.BackoffProperties();
            backoff.setEnabled(true);
            backoff.setDelay(Duration.ofSeconds(55));
            backoff.setMultiplier(3.3);
            event.setBackoff(backoff);

            OutboxPublisherProperties.EventProperties.Defaults dummyDefaults = new OutboxPublisherProperties.EventProperties.Defaults();
            dummyDefaults.applyDefaults();
            event.applyDefaults(dummyDefaults);

            // then
            assertAll(
                    () -> assertThat(event.getInitialDelay()).isEqualTo(Duration.ofSeconds(11)),
                    () -> assertThat(event.getMinFixedDelay()).isEqualTo(Duration.ofSeconds(22)),
                    () -> assertThat(event.getMaxFixedDelay()).isEqualTo(Duration.ofSeconds(33)),
                    () -> assertThat(event.getFixedDelay()).isEqualTo(Duration.ofSeconds(0)),
                    () -> assertThat(event.getMultiplier()).isEqualTo(1.8),
                    () -> assertThat(event.backoffMultiplier()).isEqualTo(3.3),
                    () -> assertThat(event.backoffDelay()).isEqualTo(55L)
            );
        }
    }
}
