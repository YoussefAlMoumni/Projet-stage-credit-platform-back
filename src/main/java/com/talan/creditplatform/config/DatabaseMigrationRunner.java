package com.talan.creditplatform.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseMigrationRunner implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseMigrationRunner.class);

    private final JdbcTemplate jdbcTemplate;

    public DatabaseMigrationRunner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        try {
            // Hibernate ddl-auto=update does not drop NOT NULL constraints,
            // so we do it manually to allow employee deletion.
            jdbcTemplate.execute("ALTER TABLE dossier ALTER COLUMN assigned_analyst_id DROP NOT NULL;");
            jdbcTemplate.execute("ALTER TABLE dossier ALTER COLUMN approved_by_id DROP NOT NULL;");
            logger.debug("NOT NULL constraints on dossier table verified/dropped.");
        } catch (Exception e) {
            logger.debug("Could not drop NOT NULL constraints: {}", e.getMessage());
        }
    }
}
