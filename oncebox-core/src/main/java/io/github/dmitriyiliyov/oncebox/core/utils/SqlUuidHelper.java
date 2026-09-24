package io.github.dmitriyiliyov.oncebox.core.utils;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Set;
import java.util.UUID;

public interface SqlUuidHelper {
    void setToPs(PreparedStatement ps, int parameterIndex, UUID id) throws SQLException;
    void setToPs(PreparedStatement ps, int initialParameterIndex, Set<UUID> ids) throws SQLException;
}
