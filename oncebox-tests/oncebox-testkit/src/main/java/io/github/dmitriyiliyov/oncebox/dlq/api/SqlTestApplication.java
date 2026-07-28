package io.github.dmitriyiliyov.oncebox.dlq.api;


import io.github.dmitriyiliyov.oncebox.dlq.api.it.config.BaseIntegrationTestsConfig;
import io.github.dmitriyiliyov.oncebox.dlq.api.it.config.ClockConfig;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Profile("postgres-it | mysql-it | oracle-it")
@SpringBootApplication
@ComponentScan(
        excludeFilters = {
                @ComponentScan.Filter(
                        type = FilterType.ANNOTATION,
                        classes = {RestController.class, Controller.class, ControllerAdvice.class, RestControllerAdvice.class}
                )
        }
)
@EnableTransactionManagement
@Import({
        ClockConfig.class,
        BaseIntegrationTestsConfig.class
})
public class SqlTestApplication { }
