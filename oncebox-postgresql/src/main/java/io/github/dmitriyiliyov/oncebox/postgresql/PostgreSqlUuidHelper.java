package io.github.dmitriyiliyov.oncebox.postgresql;

import io.github.dmitriyiliyov.oncebox.core.utils.SqlUuidHelper;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Set;
import java.util.UUID;

public final class PostgreSqlUuidHelper implements SqlUuidHelper {

    @Override
    public void setToPs(PreparedStatement ps, int parameterIndex, UUID id) throws SQLException {
        ps.setObject(parameterIndex, id);
    }

    @Override
    public void setToPs(PreparedStatement ps, int initialParameterIndex, Set<UUID> ids) throws SQLException {
        for (UUID id : ids) {
            ps.setObject(initialParameterIndex++, id);
        }
    }
}
