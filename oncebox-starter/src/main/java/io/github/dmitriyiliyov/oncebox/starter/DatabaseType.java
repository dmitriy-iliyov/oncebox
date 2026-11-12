package io.github.dmitriyiliyov.oncebox.starter;

import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.jdbc.support.MetaDataAccessException;

import javax.sql.DataSource;
import java.sql.DatabaseMetaData;

/**
 * Databases oncebox supports. The vendor name selects the dialect's directory under {@code db/oncebox}.
 */
public enum DatabaseType {
    POSTGRESQL("postgresql"),
    MYSQL("mysql"),
    ORACLE("oracle");

    private final String vendorName;

    DatabaseType(String vendorName) {
        this.vendorName = vendorName;
    }

    public String getVendorName() {
        return vendorName;
    }

    /**
     * Resolves a database type by its name, ignoring case.
     *
     * @throws IllegalArgumentException if the value is {@code null}, blank or names an unsupported database
     */
    public static DatabaseType fromString(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("DatabaseType is null, empty or blank");
        }
        try {
            return valueOf(value.toUpperCase());
        } catch (Exception e) {
            throw new IllegalArgumentException("Unsupported database '%s'".formatted(value));
        }
    }

    /**
     * Resolves the database type from the product name the JDBC driver reports.
     *
     * @throws IllegalArgumentException if the database is not supported
     */
    public static DatabaseType fromDataSource(DataSource dataSource) {
        try {
            String dbProductName = JdbcUtils.extractDatabaseMetaData(
                    dataSource, DatabaseMetaData::getDatabaseProductName
            );
            return DatabaseType.fromString(dbProductName);
        } catch (MetaDataAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
