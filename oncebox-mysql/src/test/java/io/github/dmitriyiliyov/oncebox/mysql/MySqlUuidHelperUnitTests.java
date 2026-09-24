package io.github.dmitriyiliyov.oncebox.mysql;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.ByteBuffer;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MySqlUuidHelperUnitTests {

    @Mock
    private PreparedStatement ps;

    private MySqlUuidHelper helper;

    @BeforeEach
    void setUp() {
        helper = new MySqlUuidHelper();
    }

    @Test
    @DisplayName("UT uuidToBytes() should convert UUID to 16 bytes array correctly")
    void uuidToBytes_shouldConvertUuidToBytesCorrectly() {
        // given
        UUID id = UUID.randomUUID();
        ByteBuffer expectedBuffer = ByteBuffer.wrap(new byte[16]);
        expectedBuffer.putLong(id.getMostSignificantBits());
        expectedBuffer.putLong(id.getLeastSignificantBits());
        byte[] expectedBytes = expectedBuffer.array();

        // when
        byte[] result = helper.uuidToBytes(id);

        // then
        assertThat(result).isEqualTo(expectedBytes);
    }

    @Test
    @DisplayName("UT setIdToPs() should call ps.setBytes with correct index and bytes")
    void setIdToPs_shouldCallSetBytesWithCorrectIndexAndBytes() throws SQLException {
        // given
        UUID id = UUID.randomUUID();
        int parameterIndex = 1;

        // when
        helper.setToPs(ps, parameterIndex, id);

        // then
        verify(ps).setBytes(eq(parameterIndex), any(byte[].class));
    }

    @Test
    @DisplayName("UT setIdsToPs() should call ps.setBytes for each id with incrementing index")
    void setIdsToPs_shouldCallSetBytesForEachIdWithIncrementingIndex() throws SQLException {
        // given
        Set<UUID> ids = Set.of(UUID.randomUUID(), UUID.randomUUID());
        int initialParameterIndex = 1;

        // when
        helper.setToPs(ps, initialParameterIndex, ids);

        // then
        verify(ps, times(2)).setBytes(any(Integer.class), any(byte[].class));
    }
}
