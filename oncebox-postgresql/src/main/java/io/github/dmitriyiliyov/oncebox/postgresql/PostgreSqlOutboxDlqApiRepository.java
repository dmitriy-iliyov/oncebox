package io.github.dmitriyiliyov.oncebox.postgresql;

import io.github.dmitriyiliyov.oncebox.core.utils.ResultSetMapper;
import io.github.dmitriyiliyov.oncebox.core.utils.SqlUuidHelper;
import io.github.dmitriyiliyov.oncebox.dlq.api.AbstractOutboxDlqApiRepository;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

public class PostgreSqlOutboxDlqApiRepository extends AbstractOutboxDlqApiRepository {

    public PostgreSqlOutboxDlqApiRepository(JdbcTemplate jdbcTemplate, SqlUuidHelper uuidHelper, ResultSetMapper mapper) {
        super(jdbcTemplate, uuidHelper, mapper);
    }

    @Override
    protected Object convertIdParameter(UUID id) {
        return id;
    }

    @Override
    protected String currentTimestampSql() {
        return "clock_timestamp()";
    }
}
