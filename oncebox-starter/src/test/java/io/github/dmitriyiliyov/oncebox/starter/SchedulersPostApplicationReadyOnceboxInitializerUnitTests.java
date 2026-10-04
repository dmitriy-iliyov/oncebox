package io.github.dmitriyiliyov.oncebox.starter;

import io.github.dmitriyiliyov.oncebox.core.OnceboxScheduler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SchedulersPostApplicationReadyOnceboxInitializerUnitTests {

    @Test
    @DisplayName("UT init() when schedulers present should call schedule on each")
    void init_whenSchedulersPresent_shouldCallScheduleOnEach() {
        // given
        OnceboxScheduler scheduler1 = mock(OnceboxScheduler.class);
        OnceboxScheduler scheduler2 = mock(OnceboxScheduler.class);

        SchedulersPostApplicationReadyOnceboxInitializer initializer = new SchedulersPostApplicationReadyOnceboxInitializer(
                Map.of("scheduler1", scheduler1, "scheduler2", scheduler2)
        );

        // when
        initializer.init();

        // then
        verify(scheduler1).schedule();
        verify(scheduler2).schedule();
    }

    @Test
    @DisplayName("UT init() when no schedulers present should not throw")
    void init_whenNoSchedulers_shouldNotThrow() {
        // given
        SchedulersPostApplicationReadyOnceboxInitializer initializer = new SchedulersPostApplicationReadyOnceboxInitializer(
                Collections.emptyMap()
        );

        // when + then (no exception is thrown)
        initializer.init();
    }
}