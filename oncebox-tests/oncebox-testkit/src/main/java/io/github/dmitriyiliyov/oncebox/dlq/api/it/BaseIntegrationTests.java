package io.github.dmitriyiliyov.oncebox.dlq.api.it;

import io.github.dmitriyiliyov.oncebox.dlq.api.SqlTestApplication;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = SqlTestApplication.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public abstract class BaseIntegrationTests { }