package io.github.dmitriyiliyov.oncebox.metrics.outbox;

import org.springframework.jdbc.core.JdbcTemplate;

import java.nio.ByteBuffer;
import java.util.UUID;

class MySqlOutboxMetricsRepositoryVerifier extends MultiSqlDialectOutboxMetricsRepositoryVerifier {

    MySqlOutboxMetricsRepositoryVerifier(MultiDialectOutboxMetricsRepository repository,
                                         JdbcTemplate jdbcTemplate) {
        super(repository, jdbcTemplate);
    }

    @Override
    protected Object idParam(UUID id) {
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(id.getMostSignificantBits());
        bb.putLong(id.getLeastSignificantBits());
        return bb.array();
    }
}