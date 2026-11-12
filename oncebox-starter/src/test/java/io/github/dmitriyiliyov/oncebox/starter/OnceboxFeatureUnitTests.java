package io.github.dmitriyiliyov.oncebox.starter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class OnceboxFeatureUnitTests {

    @ParameterizedTest(name = "{0}")
    @EnumSource(DatabaseType.class)
    @DisplayName("UT getValue() should name exactly the migration directories of every vendor")
    void getValue_shouldNameExactlyMigrationDirectoriesOfEveryVendor(DatabaseType databaseType) throws IOException {
        // given
        Set<String> expected = Arrays.stream(OnceboxFeature.values())
                .map(OnceboxFeature::getValue)
                .collect(Collectors.toSet());

        // when
        Set<String> directories = migrationDirectories(databaseType.getVendorName());

        // then
        assertThat(directories).isEqualTo(expected);
    }

    private static Set<String> migrationDirectories(String vendor) throws IOException {
        Resource[] scripts = new PathMatchingResourcePatternResolver()
                .getResources("classpath*:db/oncebox/%s/*/V*__*.sql".formatted(vendor));
        Set<String> directories = new HashSet<>();
        for (Resource script : scripts) {
            String[] segments = script.getURL().getPath().split("/");
            directories.add(segments[segments.length - 2]);
        }
        return directories;
    }
}
