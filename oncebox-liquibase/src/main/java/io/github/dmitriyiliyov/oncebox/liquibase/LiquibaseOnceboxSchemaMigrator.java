package io.github.dmitriyiliyov.oncebox.liquibase;

import io.github.dmitriyiliyov.oncebox.core.OnceboxMigrationException;
import io.github.dmitriyiliyov.oncebox.core.OnceboxSchemaMigrator;
import liquibase.exception.LiquibaseException;
import liquibase.integration.spring.SpringLiquibase;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Applies the feature's {@code V<n>__*.sql} scripts with Liquibase, one changelog per script in numeric order of
 * {@code n}, recording them in the feature's own {@code oncebox_<feature>_changelog} table. A script version is a
 * whole number: {@code V1_1__x.sql} or {@code V1.1__x.sql} fails the migration.
 */
public class LiquibaseOnceboxSchemaMigrator implements OnceboxSchemaMigrator {

    private final DataSource dataSource;
    private final ResourceLoader resourceLoader;
    private final String vendor;

    public LiquibaseOnceboxSchemaMigrator(DataSource dataSource, ResourceLoader resourceLoader, String vendor) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource cannot be null");
        this.resourceLoader = Objects.requireNonNull(resourceLoader, "resourceLoader cannot be null");
        this.vendor = Objects.requireNonNull(vendor, "vendor cannot be null");
    }

    @Override
    public void migrate(String feature) {
        try {
            for (String path : findMigrations(feature)) {
                SpringLiquibase liquibase = new SpringLiquibase();
                liquibase.setDataSource(dataSource);
                liquibase.setResourceLoader(resourceLoader);
                liquibase.setChangeLog("classpath:" + path);
                liquibase.setDatabaseChangeLogTable("oncebox_" + feature + "_changelog");
                liquibase.setDatabaseChangeLogLockTable("oncebox_" + feature + "_changelog_lock");
                liquibase.afterPropertiesSet();
            }
        } catch (LiquibaseException | RuntimeException e) {
            throw new OnceboxMigrationException(feature, vendor, e);
        }
    }

    private List<String> findMigrations(String feature) {
        String location = "db/oncebox/" + vendor + "/" + feature + "/";
        try {
            Resource[] resources = new PathMatchingResourcePatternResolver(resourceLoader)
                    .getResources("classpath*:" + location + "V*__*.sql");
            return Arrays.stream(resources)
                    .map(Resource::getFilename)
                    .sorted(Comparator.comparing(LiquibaseOnceboxSchemaMigrator::version))
                    .map(filename -> location + filename)
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot list migrations in " + location, e);
        }
    }

    private static BigInteger version(String filename) {
        Objects.requireNonNull(filename, "filename cannot be null");
        return new BigInteger(filename.substring(1, filename.indexOf("__")));
    }
}
