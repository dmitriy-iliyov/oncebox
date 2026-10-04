package io.github.dmitriyiliyov.oncebox.starter.outbox.dlq;

import io.github.dmitriyiliyov.oncebox.starter.OnceboxProperties;
import io.github.dmitriyiliyov.oncebox.starter.PollingType;
import io.github.dmitriyiliyov.oncebox.starter.outbox.OutboxProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class OutboxPropertiesDlqPropertiesUnitTests {

    @Nested
    @DisplayName("Enabled Flag Logic Tests")
    class EnabledFlagTests {

        @Test
        @DisplayName("UT applyDefaults() when enabled is null should disable and initialize empty transfers")
        void applyDefaults_whenEnabledNull_shouldDisableAndInitEmpty() {
            // given
            OutboxProperties.DlqProperties dlq = new OutboxProperties.DlqProperties();
            dlq.setEnabled(null);

            // when
            dlq.applyDefaults();

            // then
            assertThat(dlq.isEnabled()).isFalse();
            assertThat(dlq.getTransferTo()).isNotNull();
            assertThat(dlq.getTransferTo().getBatchSize()).isNull();
            assertThat(dlq.getTransferTo().getPolling()).isNull();

            assertThat(dlq.getTransferFrom()).isNotNull();
            assertThat(dlq.getTransferFrom().getBatchSize()).isNull();
            assertThat(dlq.getTransferFrom().getPolling()).isNull();
        }

        @Test
        @DisplayName("UT applyDefaults() when enabled is false should clear existing transfers")
        void applyDefaults_whenEnabledFalse_shouldDisableAndInitEmpty() {
            // given
            OutboxProperties.DlqProperties dlq = new OutboxProperties.DlqProperties();
            dlq.setEnabled(false);

            OutboxProperties.DlqProperties.TransferProperties dummyTransfer = new OutboxProperties.DlqProperties.TransferProperties();
            dummyTransfer.setBatchSize(999);
            dlq.setTransferTo(dummyTransfer);

            // when
            dlq.applyDefaults();

            // then
            assertThat(dlq.isEnabled()).isFalse();
            assertThat(dlq.getTransferTo().getBatchSize()).isNull();
        }
    }

    @Nested
    @DisplayName("Global Defaults Application Tests")
    class GlobalDefaultsTests {

        @Test
        @DisplayName("UT applyDefaults() when enabled and all properties are null should apply global DEFAULTS")
        void applyDefaults_whenEnabledTrueAndAllNull_shouldApplyGlobalDefaults() {
            // given
            OutboxProperties.DlqProperties dlq = new OutboxProperties.DlqProperties();
            dlq.setEnabled(true);

            // when
            dlq.applyDefaults();

            // then
            assertAll(
                    () -> assertThat(dlq.isEnabled()).isTrue(),
                    () -> assertThat(dlq.getTransferTo().getBatchSize()).isEqualTo(500),
                    () -> assertThat(dlq.getTransferTo().getPolling().getType()).isEqualTo(PollingType.ADAPTIVE),
                    () -> assertThat(dlq.getTransferTo().getInitialDelay()).isEqualTo(Duration.ofMinutes(5)),
                    () -> assertThat(dlq.getTransferTo().getMinFixedDelay()).isEqualTo(Duration.ofSeconds(1)),
                    () -> assertThat(dlq.getTransferTo().getMaxFixedDelay()).isEqualTo(Duration.ofMinutes(2)),
                    () -> assertThat(dlq.getTransferTo().getMultiplier()).isEqualTo(10.0),
                    () -> assertThat(dlq.getTransferFrom().getBatchSize()).isEqualTo(500),
                    () -> assertThat(dlq.getTransferFrom().getPolling().getType()).isEqualTo(PollingType.ADAPTIVE)
            );
        }
    }

    @Nested
    @DisplayName("Shared Properties Inheritance Tests")
    class SharedPropertiesTests {

        @Test
        @DisplayName("UT applyDefaults() should propagate shared overrides to both transfers")
        void applyDefaults_whenSharedOverridesProvided_shouldPropagateToTransfers() {
            // given
            OutboxProperties.DlqProperties dlq = new OutboxProperties.DlqProperties();
            dlq.setEnabled(true);
            dlq.setBatchSize(1000);
            OnceboxProperties.PollingProperties sharedPolling = new OnceboxProperties.PollingProperties();
            sharedPolling.setType(PollingType.FIXED);
            sharedPolling.setFixedDelay(Duration.ofSeconds(42));
            sharedPolling.setInitialDelay(Duration.ofSeconds(10));
            dlq.setPolling(sharedPolling);

            // when
            dlq.applyDefaults();

            // then
            assertAll(
                    () -> assertThat(dlq.getTransferTo().getBatchSize()).isEqualTo(1000),
                    () -> assertThat(dlq.getTransferTo().getPolling().getType()).isEqualTo(PollingType.FIXED),
                    () -> assertThat(dlq.getTransferTo().getFixedDelay()).isEqualTo(Duration.ofSeconds(42)),
                    () -> assertThat(dlq.getTransferTo().getInitialDelay()).isEqualTo(Duration.ofSeconds(10)),

                    () -> assertThat(dlq.getTransferFrom().getBatchSize()).isEqualTo(1000),
                    () -> assertThat(dlq.getTransferFrom().getPolling().getType()).isEqualTo(PollingType.FIXED)
            );
        }

        @Test
        @DisplayName("UT applyDefaults() should fallback to global default when sharedBatchSize is invalid (<= 0)")
        void applyDefaults_whenSharedBatchSizeInvalid_shouldFallbackToGlobalDefault() {
            // given
            OutboxProperties.DlqProperties dlq = new OutboxProperties.DlqProperties();
            dlq.setEnabled(true);
            dlq.setBatchSize(-15);

            // when
            dlq.applyDefaults();

            // then
            assertThat(dlq.getTransferTo().getBatchSize()).isEqualTo(500);
            assertThat(dlq.getTransferFrom().getBatchSize()).isEqualTo(500);
        }
    }

    @Nested
    @DisplayName("Transfer-Specific Overrides and Edge Cases")
    class TransferSpecificOverridesTests {

        @Test
        @DisplayName("UT applyDefaults() should prioritize specific transfer settings over shared settings")
        void applyDefaults_whenTransferOverridesProvided_shouldNotUseShared() {
            // given
            OutboxProperties.DlqProperties dlq = new OutboxProperties.DlqProperties();
            dlq.setEnabled(true);
            dlq.setBatchSize(1000);

            OutboxProperties.DlqProperties.TransferProperties to = new OutboxProperties.DlqProperties.TransferProperties();
            to.setBatchSize(200);
            OnceboxProperties.PollingProperties toPolling = new OnceboxProperties.PollingProperties();
            toPolling.setType(PollingType.FIXED);
            toPolling.setFixedDelay(Duration.ofSeconds(15));
            to.setPolling(toPolling);
            dlq.setTransferTo(to);

            OutboxProperties.DlqProperties.TransferProperties from = new OutboxProperties.DlqProperties.TransferProperties();
            from.setBatchSize(300);
            dlq.setTransferFrom(from);

            // when
            dlq.applyDefaults();

            // then
            assertAll(
                    () -> assertThat(dlq.getTransferTo().getBatchSize()).isEqualTo(200),
                    () -> assertThat(dlq.getTransferTo().getPolling().getType()).isEqualTo(PollingType.FIXED),
                    () -> assertThat(dlq.getTransferTo().getFixedDelay()).isEqualTo(Duration.ofSeconds(15)),

                    () -> assertThat(dlq.getTransferFrom().getBatchSize()).isEqualTo(300),
                    () -> assertThat(dlq.getTransferFrom().getPolling().getType()).isEqualTo(PollingType.ADAPTIVE)
            );
        }

        @Test
        @DisplayName("UT applyDefaults() should fallback to shared batch size when transfer batch size is invalid (<= 0)")
        void applyDefaults_whenTransferBatchSizeInvalid_shouldFallbackToShared() {
            // given
            OutboxProperties.DlqProperties dlq = new OutboxProperties.DlqProperties();
            dlq.setEnabled(true);
            dlq.setBatchSize(777);

            OutboxProperties.DlqProperties.TransferProperties to = new OutboxProperties.DlqProperties.TransferProperties();
            to.setBatchSize(0);
            dlq.setTransferTo(to);

            // when
            dlq.applyDefaults();

            // then
            assertThat(dlq.getTransferTo().getBatchSize()).isEqualTo(777);
        }

        @Test
        @DisplayName("UT applyDefaults() [EDGE CASE] should handle asymmetric configurations correctly")
        void applyDefaults_whenAsymmetricConfigured_shouldHandleCorrectly() {
            // given
            OutboxProperties.DlqProperties dlq = new OutboxProperties.DlqProperties();
            dlq.setEnabled(true);
            dlq.setBatchSize(800);

            OutboxProperties.DlqProperties.TransferProperties from = new OutboxProperties.DlqProperties.TransferProperties();
            from.setBatchSize(250);
            dlq.setTransferFrom(from);

            // when
            dlq.applyDefaults();

            // then
            assertAll(
                    () -> assertThat(dlq.getTransferTo().getBatchSize()).isEqualTo(800),
                    () -> assertThat(dlq.getTransferFrom().getBatchSize()).isEqualTo(250)
            );
        }

        @Test
        @DisplayName("UT applyDefaults() [EDGE CASE] should handle partial transfer property overrides")
        void applyDefaults_whenPartialTransferOverrides_shouldMergeWithShared() {
            // given
            OutboxProperties.DlqProperties dlq = new OutboxProperties.DlqProperties();
            dlq.setEnabled(true);

            OnceboxProperties.PollingProperties sharedPolling = new OnceboxProperties.PollingProperties();
            sharedPolling.setType(PollingType.FIXED);
            sharedPolling.setFixedDelay(Duration.ofSeconds(60));
            dlq.setPolling(sharedPolling);

            OutboxProperties.DlqProperties.TransferProperties to = new OutboxProperties.DlqProperties.TransferProperties();
            to.setBatchSize(100);
            dlq.setTransferTo(to);

            // when
            dlq.applyDefaults();

            // then
            assertAll(
                    () -> assertThat(dlq.getTransferTo().getBatchSize()).isEqualTo(100),
                    () -> assertThat(dlq.getTransferTo().getPolling().getType()).isEqualTo(PollingType.FIXED),
                    () -> assertThat(dlq.getTransferTo().getFixedDelay()).isEqualTo(Duration.ofSeconds(60))
            );
        }
    }

    @Nested
    @DisplayName("TransferProperties Delegation and General Utility Tests")
    class DelegationAndUtilityTests {

        @Test
        @DisplayName("UT TransferProperties getters should strictly delegate to the underlying PollingProperties")
        void transferProperties_getters_shouldDelegateToPolling() {
            // given
            OutboxProperties.DlqProperties.TransferProperties transfer = new OutboxProperties.DlqProperties.TransferProperties();
            OnceboxProperties.PollingProperties polling = new OnceboxProperties.PollingProperties();

            polling.setMinFixedDelay(Duration.ofSeconds(1));
            polling.setMaxFixedDelay(Duration.ofSeconds(2));
            polling.setInitialDelay(Duration.ofSeconds(3));
            polling.setFixedDelay(Duration.ofSeconds(4));
            polling.setMultiplier(2.5);

            transfer.setPolling(polling);

            // when + then
            assertAll(
                    () -> assertThat(transfer.getMinFixedDelay()).isEqualTo(Duration.ofSeconds(1)),
                    () -> assertThat(transfer.getMaxFixedDelay()).isEqualTo(Duration.ofSeconds(2)),
                    () -> assertThat(transfer.getInitialDelay()).isEqualTo(Duration.ofSeconds(3)),
                    () -> assertThat(transfer.getFixedDelay()).isEqualTo(Duration.ofSeconds(4)),
                    () -> assertThat(transfer.getMultiplier()).isEqualTo(2.5)
            );
        }

        @Test
        @DisplayName("UT toString() should execute without exceptions and contain field info")
        void toString_shouldReturnValidString() {
            // given
            OutboxProperties.DlqProperties dlq = new OutboxProperties.DlqProperties();
            dlq.setEnabled(true);
            dlq.applyDefaults();

            // when
            String result = dlq.toString();
            String transferResult = dlq.getTransferTo().toString();

            // then
            assertThat(result).contains("DlqProperties", "enabled=true");
            assertThat(transferResult).contains("TransferProperties", "batchSize=500");
        }

        @Test
        @DisplayName("UT applyDefaults() [EDGE CASE] Idempotency check: calling multiple times should not mutate state unexpectedly")
        void applyDefaults_whenCalledMultipleTimes_shouldBeIdempotent() {
            // given
            OutboxProperties.DlqProperties dlq = new OutboxProperties.DlqProperties();
            dlq.setEnabled(true);
            dlq.setBatchSize(350);

            // when
            dlq.applyDefaults();

            int transferToBatchSizePass1 = dlq.getTransferTo().getBatchSize();
            PollingType pollingTypePass1 = dlq.getTransferTo().getPolling().getType();

            // act again
            dlq.applyDefaults();

            // then
            assertAll(
                    () -> assertThat(dlq.getTransferTo().getBatchSize()).isEqualTo(transferToBatchSizePass1).isEqualTo(350),
                    () -> assertThat(dlq.getTransferTo().getPolling().getType()).isEqualTo(pollingTypePass1).isEqualTo(PollingType.ADAPTIVE)
            );
        }
    }
}
