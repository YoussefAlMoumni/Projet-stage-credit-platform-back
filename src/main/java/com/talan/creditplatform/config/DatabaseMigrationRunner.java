package com.talan.creditplatform.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseMigrationRunner implements CommandLineRunner {

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
            System.out.println("✅ Successfully verified/dropped NOT NULL constraints on dossier table.");
        } catch (Exception e) {
            System.out.println("⚠️ Could not drop NOT NULL constraints: " + e.getMessage());
        }
    }
}
