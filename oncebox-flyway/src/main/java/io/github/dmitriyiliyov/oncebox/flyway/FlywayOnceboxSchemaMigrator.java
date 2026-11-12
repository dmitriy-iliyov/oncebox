package io.github.dmitriyiliyov.oncebox.flyway;

import io.github.dmitriyiliyov.oncebox.core.OnceboxMigrationException;
import io.github.dmitriyiliyov.oncebox.core.OnceboxSchemaMigrator;
import org.flywaydb.core.Flyway;

import javax.sql.DataSource;
import java.util.Objects;

public class FlywayOnceboxSchemaMigrator implements OnceboxSchemaMigrator {

    private final DataSource dataSource;
    private final String vendor;

    public FlywayOnceboxSchemaMigrator(DataSource dataSource, String vendor) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource cannot be null");
        this.vendor = Objects.requireNonNull(vendor, "vendor cannot be null");
    }

    @Override
    public void migrate(String feature) {
        try {
            Flyway.configure()
                    .dataSource(dataSource)
                    .locations("classpath:db/oncebox/" + vendor + "/" + feature)
                    .table("oncebox_" + feature + "_schema_history")
                    .baselineOnMigrate(true)
                    .baselineVersion("0")
                    .load()
                    .migrate();
        } catch (RuntimeException e) {
            throw new OnceboxMigrationException(feature, vendor, e);
        }
    }
}
