package io.github.dmitriyiliyov.oncebox.mysql;

import io.github.dmitriyiliyov.oncebox.core.utils.BytesResultSetMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class MySqlOutboxDlqApiRepositoryUnitTests {

    @Test
    @DisplayName("UT constructor should throw NPE when uuidHelper is null")
    void constructor_shouldThrowNPE_whenUuidHelperIsNull() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        BytesResultSetMapper mapper = mock(BytesResultSetMapper.class);
        Clock clock = mock(Clock.class);

        assertThatThrownBy(() -> new MySqlOutboxDlqApiRepository(jdbcTemplate, null, mapper, clock))
                .isInstanceOf(NullPointerException.class);
    }
}
