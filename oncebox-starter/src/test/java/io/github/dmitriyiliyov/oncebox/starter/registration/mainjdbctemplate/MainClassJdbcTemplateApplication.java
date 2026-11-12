package io.github.dmitriyiliyov.oncebox.starter.registration.mainjdbctemplate;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@SpringBootApplication
public class MainClassJdbcTemplateApplication {

    @Bean(defaultCandidate = false)
    JdbcTemplate onceboxJdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}
