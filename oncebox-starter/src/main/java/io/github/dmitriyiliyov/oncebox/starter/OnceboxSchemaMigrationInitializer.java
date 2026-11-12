package io.github.dmitriyiliyov.oncebox.starter;

import org.springframework.beans.factory.InitializingBean;

import java.util.Objects;

public final class OnceboxSchemaMigrationInitializer implements InitializingBean {

    private final Runnable migration;

    public OnceboxSchemaMigrationInitializer(Runnable migration) {
        this.migration = Objects.requireNonNull(migration, "migration cannot be null");
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        migration.run();
    }
}
