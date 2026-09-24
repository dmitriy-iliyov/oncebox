package io.github.dmitriyiliyov.oncebox.starter.publisher.dlq;

import io.github.dmitriyiliyov.oncebox.core.utils.DefaultBytesResultSetMapper;
import io.github.dmitriyiliyov.oncebox.dlq.api.OutboxDlqApiRepository;
import io.github.dmitriyiliyov.oncebox.mysql.MySqlOutboxDlqApiRepository;
import io.github.dmitriyiliyov.oncebox.mysql.MySqlUuidHelper;
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
@ConditionalOnDatabaseType(type = DatabaseType.MYSQL)
@ConditionalOnClass(MySqlOutboxDlqApiRepository.class)
public class MySqlOutboxDlqApiRepositoryConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public OutboxDlqApiRepository mySqlOutboxDlqApiRepository(@Qualifier("outboxJdbcTemplate") JdbcTemplate jdbcTemplate,
                                                              Clock clock) {
        return new MySqlOutboxDlqApiRepository(
                jdbcTemplate,
                new MySqlUuidHelper(),
                new DefaultBytesResultSetMapper(),
                clock
        );
    }
}
