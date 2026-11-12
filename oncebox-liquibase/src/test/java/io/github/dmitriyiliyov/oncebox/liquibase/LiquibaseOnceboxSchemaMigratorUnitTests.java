package io.github.dmitriyiliyov.oncebox.liquibase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class LiquibaseOnceboxSchemaMigratorUnitTests {

    private static final ResourceLoader RESOURCE_LOADER = new DefaultResourceLoader();

    @Test
    @DisplayName("UT constructor when dataSource is null should throw NullPointerException")
    void constructor_whenDataSourceIsNull_shouldThrowNullPointerException() {
        // when / then
        assertThatThrownBy(() -> new LiquibaseOnceboxSchemaMigrator(null, RESOURCE_LOADER, "postgresql"))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("dataSource cannot be null");
    }

    @Test
    @DisplayName("UT constructor when resourceLoader is null should throw NullPointerException")
    void constructor_whenResourceLoaderIsNull_shouldThrowNullPointerException() {
        // when / then
        assertThatThrownBy(() -> new LiquibaseOnceboxSchemaMigrator(mock(DataSource.class), null, "postgresql"))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("resourceLoader cannot be null");
    }

    @Test
    @DisplayName("UT constructor when vendor is null should throw NullPointerException")
    void constructor_whenVendorIsNull_shouldThrowNullPointerException() {
        // when / then
        assertThatThrownBy(() -> new LiquibaseOnceboxSchemaMigrator(mock(DataSource.class), RESOURCE_LOADER, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("vendor cannot be null");
    }
}
