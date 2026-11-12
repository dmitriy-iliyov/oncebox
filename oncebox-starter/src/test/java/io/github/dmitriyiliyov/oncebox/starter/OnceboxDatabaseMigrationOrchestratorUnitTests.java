package io.github.dmitriyiliyov.oncebox.starter;

import io.github.dmitriyiliyov.oncebox.core.OnceboxSchemaMigrator;
import io.github.dmitriyiliyov.oncebox.starter.inbox.InboxProperties;
import io.github.dmitriyiliyov.oncebox.starter.outbox.OutboxProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// The order the features are migrated in is not a contract (the scripts have no foreign keys), so it is not asserted.
class OnceboxDatabaseMigrationOrchestratorUnitTests {

    private static final String OUTBOX = OnceboxFeature.OUTBOX.getValue();
    private static final String DLQ = OnceboxFeature.DLQ.getValue();
    private static final String INBOX = OnceboxFeature.INBOX.getValue();
    private static final String JOBS = OnceboxFeature.JOBS.getValue();

    private final RecordingSchemaMigrator migrator = new RecordingSchemaMigrator();

    @Test
    @DisplayName("UT migrate() when migrator is null should throw NullPointerException")
    void migrate_whenMigratorIsNull_shouldThrowNullPointerException() {
        // given
        OnceboxProperties properties = properties(null, null);

        // when / then
        assertThatThrownBy(() -> OnceboxDatabaseMigrationOrchestrator.migrate(null, properties))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("migrator cannot be null");
    }

    @Test
    @DisplayName("UT migrate() when properties is null should throw NullPointerException")
    void migrate_whenPropertiesIsNull_shouldThrowNullPointerException() {
        // when / then
        assertThatThrownBy(() -> OnceboxDatabaseMigrationOrchestrator.migrate(migrator, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("properties cannot be null");
        assertThat(migrator.migrated).isEmpty();
    }

    @Test
    @DisplayName("UT migrate() when outbox and inbox are null should migrate nothing")
    void migrate_whenOutboxAndInboxAreNull_shouldMigrateNothing() {
        // when
        OnceboxDatabaseMigrationOrchestrator.migrate(migrator, properties(null, null));

        // then
        assertThat(migrator.migrated).isEmpty();
    }

    @Test
    @DisplayName("UT migrate() when only outbox is enabled should migrate only outbox")
    void migrate_whenOnlyOutboxEnabled_shouldMigrateOnlyOutbox() {
        // when
        OnceboxDatabaseMigrationOrchestrator.migrate(migrator, properties(outbox(true), null));

        // then
        assertThat(migrator.migrated).containsExactly(OUTBOX);
    }

    @ParameterizedTest(name = "outbox enabled = {0}")
    @NullSource
    @ValueSource(booleans = false)
    @DisplayName("UT migrate() when outbox is not enabled should migrate nothing even with dlq and clean-up enabled")
    void migrate_whenOutboxNotEnabled_shouldMigrateNothing(Boolean enabled) {
        // given
        OutboxProperties outbox = outbox(enabled);
        outbox.setDlq(dlq(true));
        outbox.setCleanUp(cleanUp(true));

        // when
        OnceboxDatabaseMigrationOrchestrator.migrate(migrator, properties(outbox, null));

        // then
        assertThat(migrator.migrated).isEmpty();
    }

    @Test
    @DisplayName("UT migrate() when outbox and dlq are enabled should migrate outbox and dlq")
    void migrate_whenOutboxAndDlqEnabled_shouldMigrateOutboxAndDlq() {
        // given
        OutboxProperties outbox = outbox(true);
        outbox.setDlq(dlq(true));

        // when
        OnceboxDatabaseMigrationOrchestrator.migrate(migrator, properties(outbox, null));

        // then
        assertThat(migrator.migrated).containsExactlyInAnyOrder(OUTBOX, DLQ);
    }

    @ParameterizedTest(name = "dlq enabled = {0}")
    @NullSource
    @ValueSource(booleans = false)
    @DisplayName("UT migrate() when dlq is not enabled should migrate outbox without dlq")
    void migrate_whenDlqNotEnabled_shouldMigrateOutboxWithoutDlq(Boolean enabled) {
        // given
        OutboxProperties outbox = outbox(true);
        outbox.setDlq(dlq(enabled));

        // when
        OnceboxDatabaseMigrationOrchestrator.migrate(migrator, properties(outbox, null));

        // then
        assertThat(migrator.migrated).containsExactly(OUTBOX);
    }

    @Test
    @DisplayName("UT migrate() when outbox clean-up is enabled should migrate outbox and jobs")
    void migrate_whenOutboxCleanUpEnabled_shouldMigrateOutboxAndJobs() {
        // given
        OutboxProperties outbox = outbox(true);
        outbox.setCleanUp(cleanUp(true));

        // when
        OnceboxDatabaseMigrationOrchestrator.migrate(migrator, properties(outbox, null));

        // then
        assertThat(migrator.migrated).containsExactlyInAnyOrder(OUTBOX, JOBS);
    }

    @ParameterizedTest(name = "clean-up enabled = {0}")
    @NullSource
    @ValueSource(booleans = false)
    @DisplayName("UT migrate() when outbox clean-up is not enabled should not migrate jobs")
    void migrate_whenOutboxCleanUpNotEnabled_shouldNotMigrateJobs(Boolean enabled) {
        // given
        OutboxProperties outbox = outbox(true);
        outbox.setCleanUp(cleanUp(enabled));

        // when
        OnceboxDatabaseMigrationOrchestrator.migrate(migrator, properties(outbox, null));

        // then
        assertThat(migrator.migrated).containsExactly(OUTBOX);
    }

    @Test
    @DisplayName("UT migrate() when only dlq clean-up is enabled should not migrate jobs")
    void migrate_whenOnlyDlqCleanUpEnabled_shouldNotMigrateJobs() {
        // given
        // the dlq clean-up scheduler exists only under 'outbox.clean-up.enabled', so its own flag needs no lock table
        OutboxProperties outbox = outbox(true);
        OutboxProperties.DlqProperties dlq = dlq(true);
        dlq.setCleanUp(cleanUp(true));
        outbox.setDlq(dlq);
        outbox.setCleanUp(cleanUp(false));

        // when
        OnceboxDatabaseMigrationOrchestrator.migrate(migrator, properties(outbox, null));

        // then
        assertThat(migrator.migrated).containsExactlyInAnyOrder(OUTBOX, DLQ);
    }

    @Test
    @DisplayName("UT migrate() when only inbox is enabled should migrate only inbox")
    void migrate_whenOnlyInboxEnabled_shouldMigrateOnlyInbox() {
        // when
        OnceboxDatabaseMigrationOrchestrator.migrate(migrator, properties(null, inbox(true)));

        // then
        assertThat(migrator.migrated).containsExactly(INBOX);
    }

    @ParameterizedTest(name = "inbox enabled = {0}")
    @NullSource
    @ValueSource(booleans = false)
    @DisplayName("UT migrate() when inbox is not enabled should migrate nothing even with clean-up enabled")
    void migrate_whenInboxNotEnabled_shouldMigrateNothing(Boolean enabled) {
        // given
        InboxProperties inbox = inbox(enabled);
        inbox.setCleanUp(cleanUp(true));

        // when
        OnceboxDatabaseMigrationOrchestrator.migrate(migrator, properties(null, inbox));

        // then
        assertThat(migrator.migrated).isEmpty();
    }

    @Test
    @DisplayName("UT migrate() when inbox clean-up is enabled should migrate inbox and jobs")
    void migrate_whenInboxCleanUpEnabled_shouldMigrateInboxAndJobs() {
        // given
        InboxProperties inbox = inbox(true);
        inbox.setCleanUp(cleanUp(true));

        // when
        OnceboxDatabaseMigrationOrchestrator.migrate(migrator, properties(null, inbox));

        // then
        assertThat(migrator.migrated).containsExactlyInAnyOrder(INBOX, JOBS);
    }

    @ParameterizedTest(name = "clean-up enabled = {0}")
    @NullSource
    @ValueSource(booleans = false)
    @DisplayName("UT migrate() when inbox clean-up is not enabled should not migrate jobs")
    void migrate_whenInboxCleanUpNotEnabled_shouldNotMigrateJobs(Boolean enabled) {
        // given
        InboxProperties inbox = inbox(true);
        inbox.setCleanUp(cleanUp(enabled));

        // when
        OnceboxDatabaseMigrationOrchestrator.migrate(migrator, properties(null, inbox));

        // then
        assertThat(migrator.migrated).containsExactly(INBOX);
    }

    @Test
    @DisplayName("UT migrate() when clean-up is enabled only on a disabled outbox should not migrate jobs")
    void migrate_whenCleanUpEnabledOnlyOnDisabledOutbox_shouldNotMigrateJobs() {
        // given
        OutboxProperties outbox = outbox(false);
        outbox.setCleanUp(cleanUp(true));

        // when
        OnceboxDatabaseMigrationOrchestrator.migrate(migrator, properties(outbox, inbox(true)));

        // then
        assertThat(migrator.migrated).containsExactly(INBOX);
    }

    @Test
    @DisplayName("UT migrate() when clean-up is enabled only on a disabled inbox should not migrate jobs")
    void migrate_whenCleanUpEnabledOnlyOnDisabledInbox_shouldNotMigrateJobs() {
        // given
        InboxProperties inbox = inbox(false);
        inbox.setCleanUp(cleanUp(true));

        // when
        OnceboxDatabaseMigrationOrchestrator.migrate(migrator, properties(outbox(true), inbox));

        // then
        assertThat(migrator.migrated).containsExactly(OUTBOX);
    }

    @Test
    @DisplayName("UT migrate() when everything is enabled should migrate outbox, dlq, inbox and jobs once each")
    void migrate_whenEverythingEnabled_shouldMigrateEveryFeatureOnce() {
        // given
        OutboxProperties outbox = outbox(true);
        outbox.setDlq(dlq(true));
        outbox.setCleanUp(cleanUp(true));
        InboxProperties inbox = inbox(true);
        inbox.setCleanUp(cleanUp(true));

        // when
        OnceboxDatabaseMigrationOrchestrator.migrate(migrator, properties(outbox, inbox));

        // then
        assertThat(migrator.migrated).containsExactlyInAnyOrder(OUTBOX, DLQ, INBOX, JOBS);
    }

    @Test
    @DisplayName("UT migrate() when outbox and inbox are enabled without dlq and clean-up should migrate both")
    void migrate_whenOutboxAndInboxEnabled_shouldMigrateOutboxAndInbox() {
        // when
        OnceboxDatabaseMigrationOrchestrator.migrate(migrator, properties(outbox(true), inbox(true)));

        // then
        assertThat(migrator.migrated).containsExactlyInAnyOrder(OUTBOX, INBOX);
    }

    private static OnceboxProperties properties(OutboxProperties outbox, InboxProperties inbox) {
        OnceboxProperties properties = new OnceboxProperties();
        properties.setOutbox(outbox);
        properties.setInbox(inbox);
        return properties;
    }

    private static OutboxProperties outbox(Boolean enabled) {
        OutboxProperties outbox = new OutboxProperties();
        outbox.setEnabled(enabled);
        return outbox;
    }

    private static OutboxProperties.DlqProperties dlq(Boolean enabled) {
        OutboxProperties.DlqProperties dlq = new OutboxProperties.DlqProperties();
        dlq.setEnabled(enabled);
        return dlq;
    }

    private static InboxProperties inbox(Boolean enabled) {
        InboxProperties inbox = new InboxProperties();
        inbox.setEnabled(enabled);
        return inbox;
    }

    private static OnceboxProperties.CleanUpProperties cleanUp(Boolean enabled) {
        OnceboxProperties.CleanUpProperties cleanUp = new OnceboxProperties.CleanUpProperties();
        cleanUp.setEnabled(enabled);
        return cleanUp;
    }

    private static final class RecordingSchemaMigrator implements OnceboxSchemaMigrator {

        private final List<String> migrated = new ArrayList<>();

        @Override
        public void migrate(String feature) {
            migrated.add(feature);
        }
    }
}
