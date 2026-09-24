package io.github.dmitriyiliyov.oncebox.postgresql;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PostgreSqlUuidHelperUnitTests {

    @Mock
    private PreparedStatement ps;

    private PostgreSqlUuidHelper helper;

    @BeforeEach
    void setUp() {
        helper = new PostgreSqlUuidHelper();
    }

    @Test
    @DisplayName("UT setIdToPs() should call ps.setObject with correct index and UUID")
    void setIdToPs_shouldCallSetObjectWithCorrectIndexAndUuid() throws SQLException {
        // given
        UUID id = UUID.randomUUID();
        int parameterIndex = 1;

        // when
        helper.setToPs(ps, parameterIndex, id);

        // then
        verify(ps).setObject(eq(parameterIndex), eq(id));
    }

    @Test
    @DisplayName("UT setIdsToPs() should call ps.setObject for each id with incrementing index")
    void setIdsToPs_shouldCallSetObjectForEachIdWithIncrementingIndex() throws SQLException {
        // given
        Set<UUID> ids = Set.of(UUID.randomUUID(), UUID.randomUUID());
        int initialParameterIndex = 1;

        // when
        helper.setToPs(ps, initialParameterIndex, ids);

        // then
        verify(ps, times(2)).setObject(any(Integer.class), any(UUID.class));
    }
}
