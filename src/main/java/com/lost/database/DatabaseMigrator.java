package com.lost.database;

import org.flywaydb.core.Flyway;

/**
 * Головний клас для запуску Flyway міграцій.
 * Підключається до H2 бази даних та виконує SQL-міграції.
 */
public class DatabaseMigrator {

    private static final String DB_URL = "jdbc:h2:file:./data/lostdb";
    private static final String DB_USER = "sa";
    private static final String DB_PASSWORD = "";

    public static void main(String[] args) {
        System.out.println("=== Lost Game Database Migrator ===");
        System.out.println("Connecting to: " + DB_URL);

        Flyway flyway = Flyway.configure()
                .dataSource(DB_URL, DB_USER, DB_PASSWORD)
                .locations("classpath:db/migration")
                .load();

        var result = flyway.migrate();

        System.out.println("Migrations applied: " + result.migrationsExecuted);
        System.out.println("Database version: " + result.targetSchemaVersion);
        System.out.println("=== Migration complete ===");
    }
}
