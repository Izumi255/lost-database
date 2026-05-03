package com.lost.database;

import com.lost.database.dao.*;
import com.lost.database.entity.*;
import com.lost.database.pool.ConnectionPool;
import java.sql.SQLException;
import java.util.List;
import org.flywaydb.core.Flyway;

/** Головний клас — запуск міграцій та демонстрація роботи DAO. */
public class DatabaseMigrator {

    private static final String DB_URL = "jdbc:h2:file:./data/lostdb";
    private static final String DB_USER = "sa";
    private static final String DB_PASSWORD = "";

    public static void main(String[] args) throws SQLException {
        System.out.println("=== Lost Game Database ===\n");

        // 1. Flyway міграції
        Flyway flyway =
                Flyway.configure()
                        .dataSource(DB_URL, DB_USER, DB_PASSWORD)
                        .locations("classpath:db/migration")
                        .load();
        var result = flyway.migrate();
        System.out.println("Migrations applied: " + result.migrationsExecuted);
        System.out.println("DB version: " + result.targetSchemaVersion + "\n");

        // 2. Власний Connection Pool
        ConnectionPool pool = ConnectionPool.createDefault();

        // 3. Демонстрація DAO
        PlayerDao playerDao = new PlayerDao(pool);
        List<Player> players = playerDao.findAll();
        System.out.println("=== Players (" + players.size() + ") ===");
        players.forEach(p -> System.out.println("  " + p));

        NpcDao npcDao = new NpcDao(pool);
        List<Npc> npcs = npcDao.findAll();
        System.out.println("\n=== NPCs (" + npcs.size() + ") ===");
        npcs.forEach(n -> System.out.println("  " + n));

        LeaderboardEntryDao lbDao = new LeaderboardEntryDao(pool);
        List<LeaderboardEntry> top = lbDao.findTop10();
        System.out.println("\n=== Leaderboard Top ===");
        top.forEach(e -> System.out.println("  " + e));

        // 4. Закриття пулу
        pool.shutdown();
        System.out.println("\n=== Done ===");
    }
}
