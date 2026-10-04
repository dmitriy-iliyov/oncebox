package io.github.dmitriyiliyov.oncebox.starter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DefaultOnceboxJobCreateCommandUnitTests {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private Clock clock;
    private DefaultOnceboxJobCreateCommand command;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(Instant.now(), ZoneId.of("UTC"));
        command = new DefaultOnceboxJobCreateCommand(jdbcTemplate, clock, "test_job", 10L, 50L);
    }

    @Test
    @DisplayName("UT should execute insert statement with correct arguments")
    void shouldCreateJobSuccessfully() throws SQLException {
        ArgumentCaptor<PreparedStatementSetter> setterCaptor = ArgumentCaptor.forClass(PreparedStatementSetter.class);
        doReturn(1).when(jdbcTemplate).update(anyString(), setterCaptor.capture());

        command.create();

        PreparedStatement ps = mock(PreparedStatement.class);
        setterCaptor.getValue().setValues(ps);

        Timestamp stub = Timestamp.from(clock.instant().minus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MILLIS));
        verify(ps).setString(1, "test_job");
        verify(ps).setTimestamp(eq(2), eq(stub));
        verify(ps).setTimestamp(eq(3), eq(stub));
        verify(ps).setLong(4, 10L);
        verify(ps).setLong(5, 50L);
    }

    @Test
    @DisplayName("UT should swallow and log DuplicateKeyException")
    void shouldHandleDuplicateKeyException() {
        doThrow(new DuplicateKeyException("Duplicate")).when(jdbcTemplate).update(anyString(), any(PreparedStatementSetter.class));

        assertThatCode(() -> command.create()).doesNotThrowAnyException();
    }
}