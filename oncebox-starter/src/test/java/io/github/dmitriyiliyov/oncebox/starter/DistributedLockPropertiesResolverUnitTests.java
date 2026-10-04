package io.github.dmitriyiliyov.oncebox.starter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DistributedLockPropertiesResolverUnitTests {

    @Test
    @DisplayName("UT should resolve durations using ADAPTIVE polling properties")
    void shouldResolveDurations_whenAdaptivePollingEnabled() {
        OnceboxProperties.DistributedLockProperties lockProps = mock(OnceboxProperties.DistributedLockProperties.class);
        OnceboxProperties.PollingProperties pollProps = mock(OnceboxProperties.PollingProperties.class);

        when(lockProps.isResolveByPollingProperties()).thenReturn(true);
        when(pollProps.getType()).thenReturn(PollingType.ADAPTIVE);
        when(pollProps.getMinFixedDelay()).thenReturn(Duration.ofMillis(150));
        when(pollProps.getMaxFixedDelay()).thenReturn(Duration.ofMillis(500));

        DistributedLockPropertiesResolver.LockDurations result =
                DistributedLockPropertiesResolver.resolve(lockProps, pollProps);

        assertThat(result.atLeastFor()).isEqualTo(150L);
        assertThat(result.atMostFor()).isEqualTo(500L);
    }

    @Test
    @DisplayName("UT should resolve durations using FIXED polling properties")
    void shouldResolveDurations_whenFixedPollingEnabled() {
        OnceboxProperties.DistributedLockProperties lockProps = mock(OnceboxProperties.DistributedLockProperties.class);
        OnceboxProperties.PollingProperties pollProps = mock(OnceboxProperties.PollingProperties.class);

        when(lockProps.isResolveByPollingProperties()).thenReturn(true);
        when(pollProps.getType()).thenReturn(PollingType.FIXED);
        when(pollProps.getFixedDelay()).thenReturn(Duration.ofMillis(300));

        DistributedLockPropertiesResolver.LockDurations result =
                DistributedLockPropertiesResolver.resolve(lockProps, pollProps);

        assertThat(result.atLeastFor()).isEqualTo(300L);
        assertThat(result.atMostFor()).isEqualTo(300L);
    }

    @Test
    @DisplayName("UT should resolve durations using explicit distributed lock properties")
    void shouldResolveDurations_whenExplicitLockPropertiesProvided() {
        OnceboxProperties.DistributedLockProperties lockProps = mock(OnceboxProperties.DistributedLockProperties.class);
        OnceboxProperties.PollingProperties pollProps = mock(OnceboxProperties.PollingProperties.class);

        when(lockProps.isResolveByPollingProperties()).thenReturn(false);
        when(lockProps.getLockAtLeastFor()).thenReturn(Duration.ofMillis(100));
        when(lockProps.getLockAtMostFor()).thenReturn(Duration.ofMillis(1000));

        DistributedLockPropertiesResolver.LockDurations result =
                DistributedLockPropertiesResolver.resolve(lockProps, pollProps);

        assertThat(result.atLeastFor()).isEqualTo(100L);
        assertThat(result.atMostFor()).isEqualTo(1000L);
    }
}