package io.github.dmitriyiliyov.oncebox.starter.schema;

import io.github.dmitriyiliyov.oncebox.core.OnceboxSchemaMigrator;
import io.github.dmitriyiliyov.oncebox.core.inbox.InboxRepository;
import io.github.dmitriyiliyov.oncebox.core.locks.DistributedLockRepository;
import io.github.dmitriyiliyov.oncebox.core.outbox.EventStatus;
import io.github.dmitriyiliyov.oncebox.core.outbox.OutboxEvent;
import io.github.dmitriyiliyov.oncebox.core.outbox.OutboxRepository;
import io.github.dmitriyiliyov.oncebox.core.outbox.dlq.DlqStatus;
import io.github.dmitriyiliyov.oncebox.core.outbox.dlq.OutboxDlqEvent;
import io.github.dmitriyiliyov.oncebox.core.outbox.dlq.OutboxDlqRepository;
import io.github.dmitriyiliyov.oncebox.starter.DefaultOnceboxJobCreateCommand;
import io.github.dmitriyiliyov.oncebox.starter.OnceboxFeature;
import io.github.dmitriyiliyov.oncebox.starter.OnceboxRepositoryFactory;

import javax.sql.DataSource;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

// Scenarios of the real db/oncebox/<vendor>/<feature> scripts, run by one migrator over one database
public class OnceboxSchemaIntegrationVerifier {

    private static final UUID OUTBOX_EVENT_ID = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    private static final UUID DLQ_EVENT_ID = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000002");
    private static final UUID INBOX_EVENT_ID = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000003");
    private static final UUID WORKER_ID = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000001");
    private static final String JOB_NAME = "schema-it-job";
    private static final List<OnceboxFeature> ALL_FEATURES = List.of(
            OnceboxFeature.OUTBOX,
            OnceboxFeature.DLQ,
            OnceboxFeature.INBOX,
            OnceboxFeature.JOBS
    );

    private final SchemaDialect dialect;
    private final SchemaDatabase database;
    private final OnceboxSchemaMigrator tested;
    private final OnceboxRepositoryFactory repositories;

    public OnceboxSchemaIntegrationVerifier(SchemaDialect dialect,
                                            Function<DataSource, OnceboxSchemaMigrator> migratorFactory) {
        this.dialect = Objects.requireNonNull(dialect, "dialect cannot be null");
        this.database = new SchemaDatabase(dialect);
        this.tested = migratorFactory.apply(database.dataSource());
        this.repositories = dialect.repositoryFactory(database.jdbcTemplate());
    }

    public void cleanUp() {
        database.dropTestTables();
    }

    public void migrate_whenSchemaIsEmpty_shouldCreateOutboxTableTheRepositoryWorksOn() {
        // when
        tested.migrate(OnceboxFeature.OUTBOX.getValue());
        repositories.createOutboxRepository().save(outboxEvent());

        // then
        assertThat(database.tableExists("outbox_events")).isTrue();
        assertOutboxEventFound();
    }

    public void migrate_whenSchemaIsEmpty_shouldCreateDlqTableTheRepositoryWorksOn() {
        // when
        tested.migrate(OnceboxFeature.DLQ.getValue());
        repositories.createOutboxDlqRepository().saveBatch(List.of(dlqEvent()));

        // then
        assertThat(database.tableExists("outbox_dlq_events")).isTrue();
        assertDlqEventFound();
    }

    public void migrate_whenSchemaIsEmpty_shouldCreateInboxTableTheRepositoryWorksOn() {
        // when
        tested.migrate(OnceboxFeature.INBOX.getValue());
        InboxRepository inbox = repositories.createInboxRepository();
        int firstConsumption = inbox.saveIfAbsent(INBOX_EVENT_ID);
        int redelivery = inbox.saveIfAbsent(INBOX_EVENT_ID);

        // then
        assertThat(database.tableExists("inbox_events")).isTrue();
        assertThat(firstConsumption).as("first consumption").isEqualTo(1);
        assertThat(redelivery).as("redelivery").isZero();
    }

    public void migrate_whenSchemaIsEmpty_shouldCreateJobsTableTheLockRepositoryWorksOn() {
        // when
        tested.migrate(OnceboxFeature.JOBS.getValue());
        createJob();
        boolean locked = repositories.createDistributedLockRepository().tryLock(JOB_NAME, WORKER_ID);

        // then
        assertThat(database.tableExists("oncebox_jobs")).isTrue();
        assertThat(locked).isTrue();
    }

    public void migrate_whenRunTwice_shouldChangeNothingAndNotFail() {
        // given
        migrateAll();
        saveOneRowPerFeature();
        Map<String, Integer> afterFirstRun = database.snapshot();

        // when
        assertThatCode(this::migrateAll).doesNotThrowAnyException();

        // then
        assertThat(database.snapshot()).isEqualTo(afterFirstRun);
    }

    public void migrate_whenTablesOfPreviousReleaseHoldData_shouldMigrateAndKeepTheRows() {
        // given
        dialect.previousReleaseScripts().forEach(database::runScript);
        saveOneRowPerFeature();

        // when
        assertThatCode(this::migrateAll).doesNotThrowAnyException();

        // then
        assertRowsOfEveryFeatureReadBack();
    }

    public void migrate_whenSchemaHoldsUnrelatedTables_shouldCreateOnceboxTablesAndLeaveThemAlone() {
        // given
        database.createUnrelatedTableWithRow();

        // when
        assertThatCode(this::migrateAll).doesNotThrowAnyException();

        // then
        assertThat(SchemaDatabase.ONCEBOX_TABLES).allSatisfy(table ->
                assertThat(database.tableExists(table)).as(table).isTrue());
        assertThat(database.rowCount(SchemaDatabase.UNRELATED_TABLE)).isEqualTo(1);
    }

    private void migrateAll() {
        ALL_FEATURES.forEach(feature -> tested.migrate(feature.getValue()));
    }

    private void saveOneRowPerFeature() {
        repositories.createOutboxRepository().save(outboxEvent());
        repositories.createOutboxDlqRepository().saveBatch(List.of(dlqEvent()));
        repositories.createInboxRepository().saveIfAbsent(INBOX_EVENT_ID);
        createJob();
    }

    private void assertRowsOfEveryFeatureReadBack() {
        assertThat(database.rowCount("outbox_events")).as("outbox_events").isEqualTo(1);
        assertThat(database.rowCount("outbox_dlq_events")).as("outbox_dlq_events").isEqualTo(1);
        assertThat(database.rowCount("inbox_events")).as("inbox_events").isEqualTo(1);
        assertThat(database.rowCount("oncebox_jobs")).as("oncebox_jobs").isEqualTo(1);
        assertOutboxEventFound();
        assertDlqEventFound();
        assertThat(repositories.createInboxRepository().saveIfAbsent(INBOX_EVENT_ID))
                .as("inbox still knows the consumed id").isZero();
        DistributedLockRepository locks = repositories.createDistributedLockRepository();
        assertThat(locks.tryLock(JOB_NAME, WORKER_ID)).as("job row still there").isTrue();
    }

    private void assertOutboxEventFound() {
        OutboxRepository outbox = repositories.createOutboxRepository();
        assertThat(outbox.findAndLockBatchByStatus(EventStatus.PENDING, 10, EventStatus.IN_PROCESS))
                .extracting(OutboxEvent::getId)
                .containsExactly(OUTBOX_EVENT_ID);
    }

    private void assertDlqEventFound() {
        OutboxDlqRepository dlq = repositories.createOutboxDlqRepository();
        assertThat(dlq.findAndLockBatchByStatus(DlqStatus.MOVED, 10, DlqStatus.IN_PROCESS))
                .extracting(OutboxDlqEvent::getId)
                .containsExactly(DLQ_EVENT_ID);
    }

    private void createJob() {
        new DefaultOnceboxJobCreateCommand(database.jdbcTemplate(), JOB_NAME, 1_000L, 60_000L).create();
    }

    private static OutboxEvent outboxEvent() {
        return new OutboxEvent(OUTBOX_EVENT_ID, "schema-it", "io.example.OrderCreated", "{\"orderId\":\"1\"}");
    }

    private static OutboxDlqEvent dlqEvent() {
        Instant at = Instant.parse("2026-01-01T10:00:00Z");
        return OutboxDlqEvent.moved(new OutboxEvent(
                DLQ_EVENT_ID,
                EventStatus.FAILED,
                "schema-it",
                "io.example.OrderCreated",
                "{\"orderId\":\"2\"}",
                3,
                at,
                at,
                at
        ));
    }
}
