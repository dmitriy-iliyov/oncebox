package io.github.dmitriyiliyov.oncebox.tests.integration.schema;

import io.github.dmitriyiliyov.oncebox.tests.integration.DatabaseContainerFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.provider.Arguments;

import java.util.Arrays;
import java.util.stream.Stream;

// Each test starts its own application, one or more times, so no Spring test context is cached between them
@Tag("schema")
public abstract class BaseSchemaIntegrationTests {

    protected final SchemaIntegrationVerifier verifier;

    protected BaseSchemaIntegrationTests(ApplicationMigrations migrations) {
        this.verifier = new SchemaIntegrationVerifier(DatabaseContainerFactory.DB_CONTAINER, migrations);
    }

    static Stream<Arguments> modeChanges() {
        return Arrays.stream(OnceboxSchemaMode.values())
                .flatMap(from -> Arrays.stream(OnceboxSchemaMode.values())
                        .filter(to -> to != from)
                        .map(to -> Arguments.of(from, to)));
    }

    @BeforeEach
    void cleanUpBefore() {
        verifier.cleanUp();
    }

    @AfterEach
    void cleanUpAfter() {
        verifier.cleanUp();
    }
}
