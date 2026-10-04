package io.github.dmitriyiliyov.oncebox.core.outbox;

import com.github.f4b6a3.uuid.UuidCreator;

import java.util.UUID;

public class UuidV7Generator implements UuidGenerator {
    @Override
    public UUID generate() {
        return UuidCreator.getTimeOrderedEpoch();
    }
}
