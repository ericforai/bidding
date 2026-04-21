package com.xiyu.bid.support;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class DualDatabaseMigrationParityTest {

    private static final int FIRST_DUAL_DATABASE_VERSION = 74;
    private static final Path POSTGRES_SQL_MIGRATIONS = Path.of("src/main/resources/db/migration");
    private static final Path POSTGRES_JAVA_MIGRATIONS = Path.of("src/main/java/db/migration");
    private static final Path MYSQL_MIGRATIONS = Path.of("src/main/resources/db/migration-mysql");
    private static final Pattern VERSIONED_MIGRATION = Pattern.compile("^V(\\d+)__.+\\.(sql|java)$");

    @Test
    void migrationsFromV74MustExistInBothDatabasePaths() throws IOException {
        SortedSet<Integer> postgresVersions = versionsAtOrAfterDualDatabaseStart(
                POSTGRES_SQL_MIGRATIONS,
                POSTGRES_JAVA_MIGRATIONS
        );
        SortedSet<Integer> mysqlVersions = versionsAtOrAfterDualDatabaseStart(MYSQL_MIGRATIONS);

        assertThat(difference(postgresVersions, mysqlVersions))
                .as("PostgreSQL V74+ migrations missing MySQL counterparts in %s", MYSQL_MIGRATIONS)
                .isEmpty();
        assertThat(difference(mysqlVersions, postgresVersions))
                .as("MySQL V74+ migrations missing PostgreSQL counterparts in %s or %s",
                        POSTGRES_SQL_MIGRATIONS,
                        POSTGRES_JAVA_MIGRATIONS)
                .isEmpty();
    }

    private static SortedSet<Integer> versionsAtOrAfterDualDatabaseStart(Path... directories) throws IOException {
        SortedSet<Integer> versions = new TreeSet<>();
        for (Path directory : directories) {
            assertThat(Files.isDirectory(directory))
                    .as("Migration directory must exist: %s", directory)
                    .isTrue();

            try (Stream<Path> paths = Files.list(directory)) {
                paths.map(path -> path.getFileName().toString())
                        .map(VERSIONED_MIGRATION::matcher)
                        .filter(matcher -> matcher.matches()
                                && Integer.parseInt(matcher.group(1)) >= FIRST_DUAL_DATABASE_VERSION)
                        .map(matcher -> Integer.parseInt(matcher.group(1)))
                        .forEach(versions::add);
            }
        }
        return versions;
    }

    private static SortedSet<Integer> difference(SortedSet<Integer> left, SortedSet<Integer> right) {
        SortedSet<Integer> diff = new TreeSet<>(left);
        diff.removeAll(right);
        return diff;
    }
}
