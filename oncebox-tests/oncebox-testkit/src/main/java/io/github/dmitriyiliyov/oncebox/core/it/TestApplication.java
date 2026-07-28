package io.github.dmitriyiliyov.oncebox.core.it;

import io.github.dmitriyiliyov.oncebox.core.it.config.BaseIntegrationTestsConfig;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.EnableTransactionManagement;

// Dialect-specific config classes (Postgres/MySql/Oracle) live in their own modules and are no
// longer imported here; each dialect's BaseXxxSqlIntegrationTests brings its own via @Import.
@SpringBootApplication
@EnableTransactionManagement
@Import({
        BaseIntegrationTestsConfig.class
})
public class TestApplication { }
