package io.github.dmitriyiliyov.oncebox.dlq.api;

import io.github.dmitriyiliyov.oncebox.core.outbox.dlq.DlqStatus;
import io.github.dmitriyiliyov.oncebox.dlq.api.exception.UnknownDlqStatusException;
import jakarta.annotation.Nullable;
import org.springframework.core.convert.converter.Converter;

public class DlqStatusQueryConverter implements Converter<String, DlqStatus> {
    @Override
    public DlqStatus convert(@Nullable String source) {
        try {
            return DlqStatus.fromString(source);
        } catch (Exception e) {
             throw new UnknownDlqStatusException(source);
        }
    }
}
