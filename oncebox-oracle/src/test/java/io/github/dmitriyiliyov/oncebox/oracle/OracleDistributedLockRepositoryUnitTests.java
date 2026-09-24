package io.github.dmitriyiliyov.oncebox.oracle;

import io.github.dmitriyiliyov.oncebox.core.utils.BytesSqlUuidHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;

import java.sql.PreparedStatement;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OracleDistributedLockRepositoryUnitTests {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private OracleDistributedLockRepository repository;

    private final BytesSqlUuidHelper uuidHelper = new OracleSqlUuidHelper();

    @BeforeEach
    void setUp() {
        repository = new OracleDistributedLockRepository(jdbcTemplate, new OracleSqlUuidHelper());
    }

    @Test
    @DisplayName("UT tryLock() should execute update with correct SQL and parameters")
    void tryLock_shouldExecuteUpdateWithCorrectSqlAndParameters() throws Exception {
        UUID workerId = UUID.randomUUID();
        String jobName = "test-job";
        when(jdbcTemplate.update(anyString(), any(PreparedStatementSetter.class))).thenReturn(1);

        boolean result = repository.tryLock(jobName, workerId);

        assertThat(result).isTrue();
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<PreparedStatementSetter> setterCaptor = ArgumentCaptor.forClass(PreparedStatementSetter.class);
        verify(jdbcTemplate).update(sqlCaptor.capture(), setterCaptor.capture());

        PreparedStatement ps = mock(PreparedStatement.class);
        setterCaptor.getValue().setValues(ps);
        verify(ps).setBytes(1, uuidHelper.uuidToBytes(workerId));
        verify(ps).setString(2, jobName);
    }

    @Test
    @DisplayName("UT tryLock() should return true when lock acquired")
    void tryLock_shouldReturnTrueWhenLockAcquired() {
        when(jdbcTemplate.update(anyString(), any(PreparedStatementSetter.class))).thenReturn(1);

        boolean result = repository.tryLock("job", UUID.randomUUID());

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("UT tryLock() should return false when lock not acquired")
    void tryLock_shouldReturnFalseWhenLockNotAcquired() {
        when(jdbcTemplate.update(anyString(), any(PreparedStatementSetter.class))).thenReturn(0);

        boolean result = repository.tryLock("job", UUID.randomUUID());

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("UT unlock() should execute update with correct SQL and parameters")
    void unlock_shouldExecuteUpdateWithCorrectSqlAndParameters() throws Exception {
        UUID workerId = UUID.randomUUID();
        String jobName = "test-job";

        repository.unlock(jobName, workerId);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<PreparedStatementSetter> setterCaptor = ArgumentCaptor.forClass(PreparedStatementSetter.class);
        verify(jdbcTemplate).update(sqlCaptor.capture(), setterCaptor.capture());

        PreparedStatement ps = mock(PreparedStatement.class);
        setterCaptor.getValue().setValues(ps);
        verify(ps).setString(1, jobName);
        verify(ps).setBytes(2, uuidHelper.uuidToBytes(workerId));
    }

    @Test
    @DisplayName("UT unlock() should call jdbcTemplate update")
    void unlock_shouldCallJdbcTemplateUpdate() {
        repository.unlock("job", UUID.randomUUID());

        verify(jdbcTemplate).update(anyString(), any(PreparedStatementSetter.class));
    }
}