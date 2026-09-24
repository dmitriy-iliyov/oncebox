package io.github.dmitriyiliyov.oncebox.core.publisher.dlq;

import io.github.dmitriyiliyov.oncebox.core.utils.ResultSetMapper;
import io.github.dmitriyiliyov.oncebox.core.utils.SqlUuidHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class AbstractOutboxDlqRepositoryUnitTests {

    @Mock
    JdbcTemplate jdbcTemplate;

    @Mock
    SqlUuidHelper uuidHelper;

    @Mock
    ResultSetMapper mapper;

    AbstractOutboxDlqRepository tested;

    @BeforeEach
    void setUp() {
        tested = Mockito.spy(
                new AbstractOutboxDlqRepository(jdbcTemplate, uuidHelper, mapper) {
                    @Override
                    public List<OutboxDlqEvent> findAndLockBatchByStatus(DlqStatus status, int batchSize, DlqStatus lockStatus) {
                        return List.of();
                    }

                    @Override
                    public int deleteBatchByStatusAndThreshold(DlqStatus status, Instant threshold, int batchSize) {
                        return 0;
                    }
                }
        );
    }

    @Test
    @DisplayName("UT constructor when jdbcTemplate is null should throw NullPointerException")
    void constructor_whenJdbcTemplateIsNull_shouldThrowNullPointerException() {
        assertThatThrownBy(() -> new AbstractOutboxDlqRepository(null, uuidHelper, mapper) {
            @Override
            public List<OutboxDlqEvent> findAndLockBatchByStatus(DlqStatus status, int batchSize, DlqStatus lockStatus) { return null; }

            @Override
            public int deleteBatchByStatusAndThreshold(DlqStatus status, Instant threshold, int batchSize) { return 0; }
        }).isInstanceOf(NullPointerException.class).hasMessageContaining("jdbcTemplate cannot be null");
    }

    @Test
    @DisplayName("UT constructor when uuidHelper is null should throw NullPointerException")
    void constructor_whenUuidHelperIsNull_shouldThrowNullPointerException() {
        assertThatThrownBy(() -> new AbstractOutboxDlqRepository(jdbcTemplate, null, mapper) {
            @Override
            public List<OutboxDlqEvent> findAndLockBatchByStatus(DlqStatus status, int batchSize, DlqStatus lockStatus) { return null; }

            @Override
            public int deleteBatchByStatusAndThreshold(DlqStatus status, Instant threshold, int batchSize) { return 0; }
        }).isInstanceOf(NullPointerException.class).hasMessageContaining("uuidHelper cannot be null");
    }

    @Test
    @DisplayName("UT constructor when mapper is null should throw NullPointerException")
    void constructor_whenMapperIsNull_shouldThrowNullPointerException() {
        assertThatThrownBy(() -> new AbstractOutboxDlqRepository(jdbcTemplate, uuidHelper, null) {
            @Override
            public List<OutboxDlqEvent> findAndLockBatchByStatus(DlqStatus status, int batchSize, DlqStatus lockStatus) { return null; }

            @Override
            public int deleteBatchByStatusAndThreshold(DlqStatus status, Instant threshold, int batchSize) { return 0; }
        }).isInstanceOf(NullPointerException.class).hasMessageContaining("mapper cannot be null");
    }

    @Test
    @DisplayName("UT deleteBatch() when ids is empty should early return")
    void deleteBatch_whenIdsIsEmpty_shouldEarlyReturn() {
        // given
        Set<UUID> ids = Set.of();

        // when
        tested.deleteBatch(ids);

        // then
        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    @DisplayName("UT deleteBatch() when ids is null should throw NPE")
    void deleteBatch_whenIdsIsNull_shouldThrow() {
        // given
        Set<UUID> ids = null;

        // when + then
        assertThrows(NullPointerException.class, () -> tested.deleteBatch(ids));
        verifyNoInteractions(jdbcTemplate);
    }
}