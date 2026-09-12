package io.github.dmitriyiliyov.oncebox.consumer.cache;

import io.github.dmitriyiliyov.oncebox.tests.fixtures.RedisTestContainerSingleton;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(classes = TestApplication.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public abstract class BaseRedisIntegrationTests {

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", RedisTestContainerSingleton.INSTANCE::getHost);
        registry.add("spring.data.redis.port", RedisTestContainerSingleton.INSTANCE::getFirstMappedPort);
    }
}
