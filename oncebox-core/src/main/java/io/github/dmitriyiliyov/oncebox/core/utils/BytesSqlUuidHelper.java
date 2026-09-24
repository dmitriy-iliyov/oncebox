package io.github.dmitriyiliyov.oncebox.core.utils;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Set;
import java.util.UUID;

public abstract class BytesSqlUuidHelper implements SqlUuidHelper {

    public abstract byte[] uuidToBytes(UUID id);

    @Override
    public void setToPs(PreparedStatement ps, int parameterIndex, UUID id) throws SQLException {
        ps.setBytes(parameterIndex, uuidToBytes(id));
    }

    @Override
    public void setToPs(PreparedStatement ps, int initialParameterIndex, Set<UUID> ids) throws SQLException {
        for (UUID id : ids) {
            ps.setBytes(initialParameterIndex++, uuidToBytes(id));
        }
    }
}
