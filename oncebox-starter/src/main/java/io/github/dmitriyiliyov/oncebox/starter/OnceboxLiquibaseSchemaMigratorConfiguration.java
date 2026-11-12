package io.github.dmitriyiliyov.oncebox.starter;

import io.github.dmitriyiliyov.oncebox.liquibase.LiquibaseOnceboxSchemaMigrator;
import liquibase.integration.spring.SpringLiquibase;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;

import javax.sql.DataSource;

@Configuration
@ConditionalOnClass({SpringLiquibase.class, LiquibaseOnceboxSchemaMigrator.class})
public class OnceboxLiquibaseSchemaMigratorConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = "liquibaseOnceboxSchemaMigratorSupplier")
    public OnceboxSchemaMigratorSupplier liquibaseOnceboxSchemaMigratorSupplier(DataSource dataSource,
                                                                                ResourceLoader resourceLoader) {
        return new LiquibaseOnceboxSchemaMigratorSupplier(dataSource, resourceLoader);
    }
}
