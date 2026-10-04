package io.github.dmitriyiliyov.oncebox.metrics.outbox.dlq;

import org.springframework.jdbc.core.JdbcTemplate;

import java.nio.ByteBuffer;
import java.util.UUID;

public class OracleOutboxMetricsDlqRepositoryVerifier extends MultiSqlDialectOutboxDlqMetricsRepositoryVerifier {

    OracleOutboxMetricsDlqRepositoryVerifier(MultiDialectOutboxDlqMetricsRepository repository,
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