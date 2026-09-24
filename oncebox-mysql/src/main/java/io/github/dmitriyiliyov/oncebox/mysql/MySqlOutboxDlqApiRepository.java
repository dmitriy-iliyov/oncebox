package io.github.dmitriyiliyov.oncebox.mysql;

import io.github.dmitriyiliyov.oncebox.core.utils.BytesResultSetMapper;
import io.github.dmitriyiliyov.oncebox.core.utils.BytesSqlUuidHelper;
import io.github.dmitriyiliyov.oncebox.dlq.api.AbstractOutboxDlqApiRepository;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Clock;
import java.util.Objects;
import java.util.UUID;

public class MySqlOutboxDlqApiRepository extends AbstractOutboxDlqApiRepository {

    private final BytesSqlUuidHelper localUuidHelper;

    public MySqlOutboxDlqApiRepository(JdbcTemplate jdbcTemplate, BytesSqlUuidHelper uuidHelper, BytesResultSetMapper mapper,
                                       Clock clock) {
        super(jdbcTemplate, uuidHelper, mapper, clock);
        this.localUuidHelper = Objects.requireNonNull(uuidHelper, "uuidHelper cannot be null");
    }

    @Override
    protected Object convertIdParameter(UUID id) {
        return localUuidHelper.uuidToBytes(id);
    }
}