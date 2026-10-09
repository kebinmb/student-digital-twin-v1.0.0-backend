package com.sdt.web_app;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.flywaydb.core.api.resolver.ResolvedMigration;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.util.Collection;

public class FlywayMigrationChecksumTest {

    @Test
    void testFlywayResolvedChecksums() {
        DataSource ds = new DriverManagerDataSource("jdbc:h2:mem:flyway_test;DB_CLOSE_DELAY=-1", "sa", "");
        Flyway flyway = Flyway.configure()
                .dataSource(ds)
                .locations("classpath:db/migration")
                .load();

        for (var migration : flyway.info().all()) {
            if ("109".equals(migration.getVersion() != null ? migration.getVersion().getVersion() : "")) {
                System.out.println("Flyway Resolved Checksum for V109: " + migration.getChecksum());
            }
        }
    }
}
