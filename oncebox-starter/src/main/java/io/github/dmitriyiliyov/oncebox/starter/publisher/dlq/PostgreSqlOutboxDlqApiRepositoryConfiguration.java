package io.github.dmitriyiliyov.oncebox.starter.publisher.dlq;


import io.github.dmitriyiliyov.oncebox.core.utils.DefaultResultSetMapper;
import io.github.dmitriyiliyov.oncebox.dlq.api.OutboxDlqApiRepository;
import io.github.dmitriyiliyov.oncebox.postgresql.PostgreSqlOutboxDlqApiRepository;
import io.github.dmitriyiliyov.oncebox.postgresql.PostgreSqlUuidHelper;
import io.github.dmitriyiliyov.oncebox.starter.ConditionalOnDatabaseType;
import io.github.dmitriyiliyov.oncebox.starter.DatabaseType;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Clock;

@Configuration
@ConditionalOnDatabaseType(type = DatabaseType.POSTGRESQL)
@ConditionalOnClass(PostgreSqlOutboxDlqApiRepository.class)
public class PostgreSqlOutboxDlqApiRepositoryConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public OutboxDlqApiRepository postgreSqlOutboxDlqApiRepository(@Qualifier("outboxJdbcTemplate") JdbcTemplate jdbcTemplate,
                                                                   Clock clock) {
        return new PostgreSqlOutboxDlqApiRepository(
                jdbcTemplate,
                new PostgreSqlUuidHelper(),
                new DefaultResultSetMapper(),
                clock
        );
    }
}
