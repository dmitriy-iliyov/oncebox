package io.github.dmitriyiliyov.oncebox.starter;

import io.github.dmitriyiliyov.oncebox.flyway.FlywayOnceboxSchemaMigrator;
import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
@ConditionalOnClass({Flyway.class, FlywayOnceboxSchemaMigrator.class})
public class OnceboxFlywaySchemaMigratorConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = "flywayOnceboxSchemaMigratorSupplier")
    public OnceboxSchemaMigratorSupplier flywayOnceboxSchemaMigratorSupplier(DataSource dataSource) {
        return new FlywayOnceboxSchemaMigratorSupplier(dataSource);
    }
}
