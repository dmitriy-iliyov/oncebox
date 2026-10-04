package io.github.dmitriyiliyov.oncebox.tests.e2e.repository;

import io.github.dmitriyiliyov.oncebox.core.outbox.dlq.DlqStatus;
import io.github.dmitriyiliyov.oncebox.core.outbox.domain.EventStatus;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Holds all database-agnostic query logic and delegates the few dialect-specific pieces
 * (id binding/reading, timestamp arithmetic, truncation, business-key binding) to subclasses.
 */
public abstract class AbstractTestOutboxRepository implements TestOutboxRepository {

    protected final JdbcTemplate jdbcTemplate;

    protected AbstractTestOutboxRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Binds a UUID to the library-owned id columns, whose storage type differs per database
     * (native uuid, BINARY(16), RAW(16)).
     */
    protected abstract Object idParam(UUID id);

    /**
     * Binds a business verify_id, stored in the test-owned tables whose column type we control.
     */
    protected abstract Object verifyIdParam(UUID id);

    /**
     * Reads an id column back into a UUID, mirroring how {@link #idParam(UUID)} stored it.
     */
    protected abstract UUID readId(java.sql.ResultSet rs, String column) throws java.sql.SQLException;

    /**
     * Returns an SQL expression that subtracts a bound number of seconds from a timestamp column,
     * with exactly one bind placeholder.
     */
    protected abstract String minusSeconds(String column);

    /**
     * Returns the statements used to clear all outbox and business tables between tests.
     */
    protected abstract List<String> truncateStatements();

    protected List<UUID> queryIds(String sql, Object... args) {
        return jdbcTemplate.query(sql, (rs, rowNum) -> readId(rs, "id"), args);
    }

    @Override
    public int countEvents() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM outbox_events", Integer.class);
    }

    @Override
    public int countEventsByStatus(EventStatus status) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM outbox_events WHERE status = ?", Integer.class, status.name()
        );
    }

    @Override
    public List<UUID> findEventIdsByStatus(EventStatus status) {
        return queryIds("SELECT id FROM outbox_events WHERE status = ?", status.name());
    }

    @Override
    public List<UUID> findEventIds() {
        return queryIds("SELECT id FROM outbox_events");
    }

    @Override
    public int findRetryCount(UUID id) {
        return jdbcTemplate.queryForObject(
                "SELECT retry_count FROM outbox_events WHERE id = ?", Integer.class, idParam(id)
        );
    }

    @Override
    public UUID insertEvent(String eventType, EventStatus status, String payloadType, String payload, Duration age) {
        UUID id = UUID.randomUUID();
        Timestamp timestamp = Timestamp.from(Instant.now().minus(age));
        jdbcTemplate.update("""
                INSERT INTO outbox_events (id, status, event_type, payload_type, payload, retry_count, next_retry_at, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, 0, ?, ?, ?)
                """,
                idParam(id), status.name(), eventType, payloadType, payload, timestamp, timestamp, timestamp
        );
        return id;
    }

    @Override
    public void shiftEventTimestamps(UUID id, Duration shift) {
        String sql = "UPDATE outbox_events SET "
                + "next_retry_at = " + minusSeconds("next_retry_at") + ", "
                + "created_at = " + minusSeconds("created_at") + ", "
                + "updated_at = " + minusSeconds("updated_at") + " "
                + "WHERE id = ?";
        long secs = shift.toSeconds();
        jdbcTemplate.update(sql, secs, secs, secs, idParam(id));
    }

    @Override
    public int countDlqEvents() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM outbox_dlq_events", Integer.class);
    }

    @Override
    public int countDlqEventsByStatus(DlqStatus status) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM outbox_dlq_events WHERE dlq_status = ?", Integer.class, status.name()
        );
    }

    @Override
    public List<UUID> findDlqEventIdsByStatus(DlqStatus status) {
        return queryIds("SELECT id FROM outbox_dlq_events WHERE dlq_status = ?", status.name());
    }

    @Override
    public UUID insertDlqEvent(String eventType, DlqStatus dlqStatus, String payloadType, String payload) {
        UUID id = UUID.randomUUID();
        Timestamp timestamp = Timestamp.from(Instant.now());
        jdbcTemplate.update("""
                INSERT INTO outbox_dlq_events (id, status, dlq_status, event_type, payload_type, payload, retry_count, next_retry_at, created_at, updated_at, moved_at)
                VALUES (?, ?, ?, ?, ?, ?, 0, ?, ?, ?, ?)
                """,
                idParam(id), EventStatus.FAILED.name(), dlqStatus.name(), eventType, payloadType, payload,
                timestamp, timestamp, timestamp, timestamp
        );
        return id;
    }

    @Override
    public void shiftDlqEventTimestamps(UUID id, Duration shift) {
        String sql = "UPDATE outbox_dlq_events SET "
                + "next_retry_at = " + minusSeconds("next_retry_at") + ", "
                + "created_at = " + minusSeconds("created_at") + ", "
                + "updated_at = " + minusSeconds("updated_at") + ", "
                + "moved_at = " + minusSeconds("moved_at") + " "
                + "WHERE id = ?";
        long secs = shift.toSeconds();
        jdbcTemplate.update(sql, secs, secs, secs, secs, idParam(id));
    }

    @Override
    public boolean isConsumed(UUID eventId) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM inbox_events WHERE id = ?", Integer.class, idParam(eventId)
        ) > 0;
    }

    @Override
    public void shiftConsumedEventTimestamp(UUID eventId, Duration shift) {
        String sql = "UPDATE inbox_events SET consumed_at = " + minusSeconds("consumed_at") + " WHERE id = ?";
        jdbcTemplate.update(sql, shift.toSeconds(), idParam(eventId));
    }

    @Override
    public void saveProducedBusiness(UUID verifyId) {
        jdbcTemplate.update("INSERT INTO e2e_produced_events (verify_id) VALUES (?)", verifyIdParam(verifyId));
    }

    @Override
    public int countProducedBusiness(UUID verifyId) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM e2e_produced_events WHERE verify_id = ?", Integer.class, verifyIdParam(verifyId)
        );
    }

    @Override
    public void saveConsumedBusiness(UUID verifyId) {
        jdbcTemplate.update("INSERT INTO e2e_consumed_events (verify_id) VALUES (?)", verifyIdParam(verifyId));
    }

    @Override
    public int countConsumedBusiness(UUID verifyId) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM e2e_consumed_events WHERE verify_id = ?", Integer.class, verifyIdParam(verifyId)
        );
    }

    @Override
    public int countConsumedBusiness() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM e2e_consumed_events", Integer.class);
    }

    @Override
    public void truncateAll() {
        truncateStatements().forEach(jdbcTemplate::execute);
    }
}
