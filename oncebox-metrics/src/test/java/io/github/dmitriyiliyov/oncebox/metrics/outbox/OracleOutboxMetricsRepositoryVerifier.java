package io.github.dmitriyiliyov.oncebox.metrics.outbox;

import org.springframework.jdbc.core.JdbcTemplate;

import java.nio.ByteBuffer;
import java.util.UUID;

class OracleOutboxMetricsRepositoryVerifier extends MultiSqlDialectOutboxMetricsRepositoryVerifier {

    OracleOutboxMetricsRepositoryVerifier(MultiDialectOutboxMetricsRepository repository,
                                          JdbcTemplate jdbcTemplate) {
        super(repository, jdbcTemplate);
    }

    @Override
    protected Object idParam(UUID id) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(new byte[16]);
        byteBuffer.putLong(id.getMostSignificantBits());
        byteBuffer.putLong(id.getLeastSignificantBits());
        return byteBuffer.array();
    }
}
