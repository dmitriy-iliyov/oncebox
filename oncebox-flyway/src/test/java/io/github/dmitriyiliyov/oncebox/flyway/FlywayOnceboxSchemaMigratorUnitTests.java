package io.github.dmitriyiliyov.oncebox.flyway;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class FlywayOnceboxSchemaMigratorUnitTests {

    @Test
    @DisplayName("UT constructor when dataSource is null should throw NullPointerException")
    void constructor_whenDataSourceIsNull_shouldThrowNullPointerException() {
        // when / then
        assertThatThrownBy(() -> new FlywayOnceboxSchemaMigrator(null, "postgresql"))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("dataSource cannot be null");
    }

    @Test
    @DisplayName("UT constructor when vendor is null should throw NullPointerException")
    void constructor_whenVendorIsNull_shouldThrowNullPointerException() {
        // when / then
        assertThatThrownBy(() -> new FlywayOnceboxSchemaMigrator(mock(DataSource.class), null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("vendor cannot be null");
    }
}
