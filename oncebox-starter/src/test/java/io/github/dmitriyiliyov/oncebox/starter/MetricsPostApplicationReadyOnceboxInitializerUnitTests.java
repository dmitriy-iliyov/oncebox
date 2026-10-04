package io.github.dmitriyiliyov.oncebox.starter;

import io.github.dmitriyiliyov.oncebox.metrics.outbox.OutboxMetrics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MetricsPostApplicationReadyOnceboxInitializerUnitTests {

    @Test
    @DisplayName("UT init() when metrics present should call register on each")
    void init_whenMetricsPresent_shouldCallRegisterOnEach() {
        // given
        OutboxMetrics metrics1 = mock(OutboxMetrics.class);
        OutboxMetrics metrics2 = mock(OutboxMetrics.class);

        MetricsPostApplicationReadyOnceboxInitializer initializer = new MetricsPostApplicationReadyOnceboxInitializer(
                Map.of("metrics1", metrics1, "metrics2", metrics2)
        );

        // when
        initializer.init();

        // then
        verify(metrics1).register();
        verify(metrics2).register();
    }

    @Test
    @DisplayName("UT init() when no metrics present should not throw")
    void init_whenNoMetrics_shouldNotThrow() {
        // given
        MetricsPostApplicationReadyOnceboxInitializer initializer = new MetricsPostApplicationReadyOnceboxInitializer(
                Collections.emptyMap()
        );

        // when + then (no exception is thrown)
        initializer.init();
    }
}
