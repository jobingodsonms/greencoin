package com.greencoin.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Migration helper to ensure database columns are the correct type
 * for Base64 image storage.
 */
@Slf4j
@Configuration
public class DatabaseMigrationConfig {

    @Bean
    public CommandLineRunner migrateDatabase(JdbcTemplate jdbcTemplate) {
        return args -> {
            log.info("Running database migrations for image storage...");
            try {
                // Alter waste_reports.image_url to TEXT to support Base64
                jdbcTemplate.execute("ALTER TABLE waste_reports ALTER COLUMN image_url TYPE TEXT;");
                log.info("Successfully altered image_url column to TEXT.");
            } catch (Exception e) {
                log.info(
                        "Column image_url might already be TEXT or table doesn't exist yet. Skipping alteration. Error: {}",
                        e.getMessage());
            }

            try {
                // Also alter description to TEXT just in case
                jdbcTemplate.execute("ALTER TABLE waste_reports ALTER COLUMN description TYPE TEXT;");
                log.info("Successfully altered description column to TEXT.");
            } catch (Exception e) {
                log.info("Column description might already be TEXT. Skipping. Error: {}", e.getMessage());
            }
        };
    }
}
