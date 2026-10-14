package io.github.dmitriyiliyov.oncebox.core.utils;

import java.util.UUID;

public class DefaultBytesResultSetMapper extends BytesResultSetMapper {

    @Override
    public UUID fromBytesToUuid(byte[] bytes) {
        if (bytes.length != 16) {
            throw new IllegalArgumentException("UUID byte array must be 16 bytes long");
        }
        long mostSigBits = 0;
        long leastSigBits = 0;
        for (int i = 0; i < 8; i++) {
            mostSigBits = (mostSigBits << 8) | (bytes[i] & 0xFF);
        }
        for (int i = 8; i < 16; i++) {
            leastSigBits = (leastSigBits << 8) | (bytes[i] & 0xFF);
        }
        return new UUID(mostSigBits, leastSigBits);
    }
}
