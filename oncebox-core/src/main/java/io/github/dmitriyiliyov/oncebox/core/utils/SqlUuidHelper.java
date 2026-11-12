package io.github.dmitriyiliyov.oncebox.core.utils;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Set;
import java.util.UUID;

/**
 * Binds a {@link UUID} to a JDBC statement in the form the dialect stores it in: a native {@code UUID} on
 * PostgreSQL, sixteen bytes on MySQL and Oracle.
 */
public interface SqlUuidHelper {
    void setToPs(PreparedStatement ps, int parameterIndex, UUID id) throws SQLException;
    /**
     * Binds the ids to consecutive parameters, starting at {@code initialParameterIndex}, in the iteration order
     * of the set.
     */
    void setToPs(PreparedStatement ps, int initialParameterIndex, Set<UUID> ids) throws SQLException;
}
