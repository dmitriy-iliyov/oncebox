package io.github.dmitriyiliyov.oncebox.dlq.api;


import io.github.dmitriyiliyov.oncebox.dlq.api.it.config.WebTestsConfig;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.context.annotation.Import;

@SpringBootApplication(exclude = {
        DataSourceAutoConfiguration.class,
        DataSourceTransactionManagerAutoConfiguration.class
})
@Import(WebTestsConfig.class)
public class WebTestApplication { }
