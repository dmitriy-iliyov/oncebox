package io.github.dmitriyiliyov.oncebox.core.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.ByteBuffer;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BytesSqlUuidHelperUnitTests {

    private static final UUID FIRST = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID SECOND = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    @Mock
    private PreparedStatement ps;

    private final BytesSqlUuidHelper tested = new TestBytesSqlUuidHelper();

    @Test
    @DisplayName("UT setIdToPs() should bind the id as bytes at the given index")
    void setToPs_shouldBindBytesAtIndex() throws SQLException {
        // when
        tested.setToPs(ps, 3, FIRST);

        // then
        verify(ps).setBytes(3, bytes(FIRST));
    }

    @Test
    @DisplayName("UT setIdsToPs() should bind the ids to consecutive indexes starting at the given one")
    void setToPs_shouldBindConsecutiveIndexes() throws SQLException {
        // when
        tested.setToPs(ps, 2, new LinkedHashSet<>(List.of(FIRST, SECOND)));

        // then
        InOrder order = inOrder(ps);
        order.verify(ps).setBytes(2, bytes(FIRST));
        order.verify(ps).setBytes(3, bytes(SECOND));
    }

    private static byte[] bytes(UUID id) {
        return ByteBuffer.allocate(16)
                .putLong(id.getMostSignificantBits())
                .putLong(id.getLeastSignificantBits())
                .array();
    }

    private static class TestBytesSqlUuidHelper extends BytesSqlUuidHelper {

        @Override
        public byte[] uuidToBytes(UUID id) {
            return bytes(id);
        }
    }
}
