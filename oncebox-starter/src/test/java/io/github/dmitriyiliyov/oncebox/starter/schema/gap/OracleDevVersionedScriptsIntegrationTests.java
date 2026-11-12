package io.github.dmitriyiliyov.oncebox.starter.schema.gap;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OracleDevVersionedScriptsIntegrationTests {

    private final DevVersionedScriptsVerifier verifier = new DevVersionedScriptsVerifier(GapDialect.ORACLE);

    @BeforeEach
    void setUp() {
        verifier.cleanUp();
    }

    @AfterEach
    void tearDown() {
        verifier.cleanUp();
    }

    @Test
    @DisplayName("IT migrate() when the feature has V1, V2 and V10 should apply all of them in numeric order")
    void migrate_whenFeatureHasSeveralVersions_shouldApplyAllInNumericOrder() throws Exception {
        verifier.migrate_whenFeatureHasSeveralVersions_shouldApplyAllInNumericOrder();
    }

    @Test
    @DisplayName("IT migrate() when run again over the applied versions should change nothing")
    void migrate_whenRunAgain_shouldChangeNothing() throws Exception {
        verifier.migrate_whenRunAgain_shouldChangeNothing();
    }
}
