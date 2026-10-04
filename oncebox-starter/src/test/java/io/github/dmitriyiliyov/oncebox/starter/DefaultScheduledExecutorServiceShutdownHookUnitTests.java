package io.github.dmitriyiliyov.oncebox.starter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ScheduledExecutorService;

import static io.github.dmitriyiliyov.oncebox.starter.OnceboxScheduledExecutorServiceConstants.TIMEOUT;
import static io.github.dmitriyiliyov.oncebox.starter.OnceboxScheduledExecutorServiceConstants.TIME_UNIT;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class DefaultScheduledExecutorServiceShutdownHookUnitTests {

    @Test
    @DisplayName("UT constructor should throw NullPointerException when shutdownTarget is null")
    public void constructor_whenShutdownTargetIsNull_shouldThrowNullPointerException() {
        // when / then
        NullPointerException e = assertThrows(NullPointerException.class,
                () -> new DefaultScheduledExecutorServiceShutdownHook(null));

        assertEquals("shutdownTarget cannot be null", e.getMessage());
    }

    @Test
    @DisplayName("UT shutdown() should call shutdown() and awaitTermination() without forcing shutdown when executor terminates in time")
    public void shutdown_whenExecutorTerminatesInTime_shouldCallShutdownAndAwaitTerminationWithoutForcingShutdown() throws InterruptedException {
        // given
        ScheduledExecutorService executorMock = mock(ScheduledExecutorService.class);
        when(executorMock.awaitTermination(TIMEOUT, TIME_UNIT)).thenReturn(true);

        DefaultScheduledExecutorServiceShutdownHook hook =
                new DefaultScheduledExecutorServiceShutdownHook(executorMock);

        // when
        hook.shutdown();

        // then
        verify(executorMock).shutdown();
        verify(executorMock).awaitTermination(TIMEOUT, TIME_UNIT);
        verify(executorMock, never()).shutdownNow();
    }

    @Test
    @DisplayName("UT shutdown() should call shutdownNow() when awaitTermination() returns false")
    public void shutdown_whenAwaitTerminationReturnsFalse_shouldForceShutdown() throws InterruptedException {
        // given
        ScheduledExecutorService executorMock = mock(ScheduledExecutorService.class);
        when(executorMock.awaitTermination(TIMEOUT, TIME_UNIT)).thenReturn(false);

        DefaultScheduledExecutorServiceShutdownHook hook =
                new DefaultScheduledExecutorServiceShutdownHook(executorMock);

        // when
        hook.shutdown();

        // then
        verify(executorMock).shutdown();
        verify(executorMock).awaitTermination(TIMEOUT, TIME_UNIT);
        verify(executorMock).shutdownNow();
    }

    @Test
    @DisplayName("UT shutdown() should call shutdownNow() and interrupt current thread when InterruptedException is thrown")
    public void shutdown_whenInterruptedExceptionThrown_shouldForceShutdownAndInterruptCurrentThread() throws InterruptedException {
        // given
        ScheduledExecutorService executorMock = mock(ScheduledExecutorService.class);
        when(executorMock.awaitTermination(TIMEOUT, TIME_UNIT)).thenThrow(new InterruptedException());

        DefaultScheduledExecutorServiceShutdownHook hook =
                new DefaultScheduledExecutorServiceShutdownHook(executorMock);

        // when
        hook.shutdown();

        // then
        verify(executorMock).shutdown();
        verify(executorMock).awaitTermination(TIMEOUT, TIME_UNIT);
        verify(executorMock).shutdownNow();
        assertTrue(Thread.currentThread().isInterrupted());

        Thread.interrupted();
    }
}