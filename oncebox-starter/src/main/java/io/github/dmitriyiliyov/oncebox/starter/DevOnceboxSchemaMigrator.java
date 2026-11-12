package io.github.dmitriyiliyov.oncebox.starter;

import io.github.dmitriyiliyov.oncebox.core.OnceboxMigrationException;
import io.github.dmitriyiliyov.oncebox.core.OnceboxSchemaMigrator;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import javax.sql.DataSource;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;

/**
 * Runs the feature's {@code V<n>__*.sql} scripts straight through JDBC, in numeric order of the whole number
 * {@code n}, with no history table: every startup runs all of them again. That is safe because every oncebox script
 * is idempotent - it creates only what is missing - so a second run on an up-to-date schema changes nothing.
 * <p>
 * Meant for local development; an application that keeps its schema under version control uses
 * {@code oncebox.schema.mode} {@code FLYWAY} or {@code LIQUIBASE}.
 */
public class DevOnceboxSchemaMigrator implements OnceboxSchemaMigrator {

    private static final String ORACLE_BLOCK_SEPARATOR = "/";

    private final DataSource dataSource;
    private final String vendor;

    public DevOnceboxSchemaMigrator(DataSource dataSource, String vendor) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource cannot be null");
        this.vendor = Objects.requireNonNull(vendor, "vendor cannot be null");
    }

    @Override
    public void migrate(String feature) {
        try {
            ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
            populator.setSqlScriptEncoding(StandardCharsets.UTF_8.name());
            populator.setContinueOnError(false);
            populator.setIgnoreFailedDrops(false);
            if (DatabaseType.ORACLE == DatabaseType.fromString(vendor)) {
                populator.setSeparator(ORACLE_BLOCK_SEPARATOR);
            }
            for (Resource script : findScripts(feature)) {
                populator.addScript(script);
            }
            DatabasePopulatorUtils.execute(populator, dataSource);
        } catch (IOException | RuntimeException e) {
            throw new OnceboxMigrationException(feature, vendor, e);
        }
    }

    private Resource[] findScripts(String feature) throws IOException {
        Resource[] scripts = new PathMatchingResourcePatternResolver()
                .getResources("classpath*:db/oncebox/" + vendor + "/" + feature + "/V*__*.sql");
        Arrays.sort(scripts, Comparator.comparing(DevOnceboxSchemaMigrator::version));
        return scripts;
    }

    private static BigInteger version(Resource script) {
        String filename = Objects.requireNonNull(script.getFilename(), "filename cannot be null");
        return new BigInteger(filename.substring(1, filename.indexOf("__")));
    }
}
