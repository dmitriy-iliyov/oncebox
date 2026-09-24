package io.github.dmitriyiliyov.oncebox.oracle;

import io.github.dmitriyiliyov.oncebox.core.utils.BytesSqlUuidHelper;

import java.nio.ByteBuffer;
import java.util.UUID;

public final class OracleSqlUuidHelper extends BytesSqlUuidHelper {

    @Override
    public byte[] uuidToBytes(UUID id) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(new byte[16]);
        byteBuffer.putLong(id.getMostSignificantBits());
        byteBuffer.putLong(id.getLeastSignificantBits());
        return byteBuffer.array();
    }
}
