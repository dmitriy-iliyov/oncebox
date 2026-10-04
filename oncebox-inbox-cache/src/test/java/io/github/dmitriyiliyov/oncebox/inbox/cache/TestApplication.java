package io.github.dmitriyiliyov.oncebox.inbox.cache;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import(RedisIntegrationTestsConfig.class)
public class TestApplication { }