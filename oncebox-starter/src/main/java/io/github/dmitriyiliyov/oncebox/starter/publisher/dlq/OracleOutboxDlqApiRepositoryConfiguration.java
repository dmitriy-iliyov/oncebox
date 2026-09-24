package io.github.dmitriyiliyov.oncebox.starter.publisher.dlq;

import io.github.dmitriyiliyov.oncebox.core.utils.DefaultBytesResultSetMapper;
import io.github.dmitriyiliyov.oncebox.dlq.api.OutboxDlqApiRepository;
import io.github.dmitriyiliyov.oncebox.oracle.OracleOutboxDlqApiRepository;
import io.github.dmitriyiliyov.oncebox.oracle.OracleSqlUuidHelper;
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
@ConditionalOnDatabaseType(type = DatabaseType.ORACLE)
@ConditionalOnClass(OracleOutboxDlqApiRepository.class)
public class OracleOutboxDlqApiRepositoryConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public OutboxDlqApiRepository oracleOutboxDlqApiRepository(@Qualifier("outboxJdbcTemplate") JdbcTemplate jdbcTemplate,
                                                               Clock clock) {
        return new OracleOutboxDlqApiRepository(
                jdbcTemplate,
                new OracleSqlUuidHelper(),
                new DefaultBytesResultSetMapper(),
                clock
        );
    }
}
