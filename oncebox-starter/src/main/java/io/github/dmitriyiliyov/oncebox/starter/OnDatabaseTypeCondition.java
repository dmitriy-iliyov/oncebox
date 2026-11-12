package io.github.dmitriyiliyov.oncebox.starter;

import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.jdbc.DatabaseDriver;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

import java.util.Map;

public class OnDatabaseTypeCondition extends SpringBootCondition {

    @Override
    public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
        Map<String, Object> attributes = metadata.getAnnotationAttributes(ConditionalOnDatabaseType.class.getName());
        if (attributes != null) {

            DatabaseType annotationDatabaseType = (DatabaseType) attributes.get("type");

            String jdbcUrl = context.getEnvironment().getProperty("spring.datasource.url");

            if (jdbcUrl == null) {
                jdbcUrl = context.getEnvironment().getProperty("spring.datasource.hikari.jdbc-url");
            }

            if (jdbcUrl == null) {
                return ConditionOutcome.noMatch("No JDBC URL found in spring.datasource.url or spring.datasource.hikari.jdbc-url");
            }

            DatabaseDriver driver = DatabaseDriver.fromJdbcUrl(jdbcUrl);

            if (!DatabaseDriver.UNKNOWN.equals(driver)) {
                DatabaseType contextDatabaseType = DatabaseType.fromString(driver.name());
                if (contextDatabaseType.equals(annotationDatabaseType)) {
                    return ConditionOutcome.match();
                }
            }
            return ConditionOutcome.noMatch("databaseType from annotation not matched with databaseDriver from context");
        } else {
            throw new IllegalStateException("annotation hasn't any attributes");
        }
    }
}
