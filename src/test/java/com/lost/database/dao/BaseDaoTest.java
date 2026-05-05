package com.lost.database.dao;

import com.lost.database.pool.ConnectionPool;
import java.sql.Connection;
import java.sql.Statement;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

/**
 * Базовий клас для інтеграційних тестів DAO.
 *
 * <p>Використовує H2 in-memory базу даних для повної ізоляції тестів. Перед кожним тест-класом
 * створює схему та заповнює початковими даними.
 */
public abstract class BaseDaoTest {

    protected static ConnectionPool pool;

    @BeforeAll
    static void initDatabase() throws Exception {
        // Створюємо in-memory H2 пул (не файловий, а тільки в пам'яті)
        ConnectionPool.PoolConfig config =
                new ConnectionPool.PoolConfig.Builder()
                        .withUrl("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1")
                        .withUser("sa")
                        .withPassword("")
                        .withMaxConnections(3)
                        .build();
        pool = new ConnectionPool(config);

        // Створюємо всі таблиці вручну (без Flyway, щоб тести були незалежними)
        try (Connection conn = pool.getConnection();
                Statement stmt = conn.createStatement()) {

            // -- players
            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS players ("
                            + "id BIGINT AUTO_INCREMENT PRIMARY KEY,"
                            + "username VARCHAR(50) NOT NULL UNIQUE,"
                            + "password_hash VARCHAR(255) NOT NULL,"
                            + "email VARCHAR(100),"
                            + "role VARCHAR(20) NOT NULL DEFAULT 'ROLE_USER',"
                            + "total_score INT DEFAULT 0,"
                            + "max_level_reached INT DEFAULT 1,"
                            + "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                            + "last_login TIMESTAMP)");

            // -- game_saves
            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS game_saves ("
                            + "id BIGINT AUTO_INCREMENT PRIMARY KEY,"
                            + "player_id BIGINT NOT NULL,"
                            + "current_level INT NOT NULL,"
                            + "health INT NOT NULL,"
                            + "max_health INT DEFAULT 100,"
                            + "sanity DOUBLE DEFAULT 100.0,"
                            + "position_x DOUBLE NOT NULL,"
                            + "position_y DOUBLE NOT NULL,"
                            + "save_name VARCHAR(100),"
                            + "saved_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                            + "CONSTRAINT fk_save_player FOREIGN KEY (player_id) REFERENCES players(id) ON DELETE CASCADE)");

            // -- inventory_items
            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS inventory_items ("
                            + "id BIGINT AUTO_INCREMENT PRIMARY KEY,"
                            + "player_id BIGINT NOT NULL,"
                            + "item_type VARCHAR(50) NOT NULL,"
                            + "item_name VARCHAR(100) NOT NULL,"
                            + "quantity INT DEFAULT 1,"
                            + "item_value INT DEFAULT 0,"
                            + "CONSTRAINT fk_item_player FOREIGN KEY (player_id) REFERENCES players(id) ON DELETE CASCADE)");

            // -- leaderboard_entries
            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS leaderboard_entries ("
                            + "id BIGINT AUTO_INCREMENT PRIMARY KEY,"
                            + "player_id BIGINT NOT NULL,"
                            + "score INT NOT NULL,"
                            + "level_completed INT NOT NULL,"
                            + "completion_time_sec DOUBLE,"
                            + "achieved_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                            + "CONSTRAINT fk_leaderboard_player FOREIGN KEY (player_id) REFERENCES players(id) ON DELETE CASCADE)");

            // -- player_achievements
            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS player_achievements ("
                            + "id BIGINT AUTO_INCREMENT PRIMARY KEY,"
                            + "player_id BIGINT NOT NULL,"
                            + "achievement_code VARCHAR(50) NOT NULL,"
                            + "achievement_name VARCHAR(100) NOT NULL,"
                            + "description VARCHAR(255),"
                            + "unlocked_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                            + "CONSTRAINT fk_achievement_player FOREIGN KEY (player_id) REFERENCES players(id) ON DELETE CASCADE)");

            // -- npcs
            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS npcs ("
                            + "id BIGINT AUTO_INCREMENT PRIMARY KEY,"
                            + "npc_name VARCHAR(50) NOT NULL,"
                            + "portrait_path VARCHAR(255),"
                            + "sprite_path VARCHAR(255),"
                            + "level_number INT NOT NULL,"
                            + "spawn_x DOUBLE NOT NULL,"
                            + "spawn_y DOUBLE NOT NULL,"
                            + "npc_type VARCHAR(30) NOT NULL)");

            // -- dialogue_lines
            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS dialogue_lines ("
                            + "id BIGINT AUTO_INCREMENT PRIMARY KEY,"
                            + "npc_id BIGINT NOT NULL,"
                            + "line_order INT NOT NULL,"
                            + "speaker_name VARCHAR(50) NOT NULL,"
                            + "portrait_key VARCHAR(50),"
                            + "dialogue_text TEXT NOT NULL,"
                            + "trigger_condition VARCHAR(100),"
                            + "CONSTRAINT fk_dialogue_npc FOREIGN KEY (npc_id) REFERENCES npcs(id) ON DELETE CASCADE)");

            // -- game_sessions
            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS game_sessions ("
                            + "id BIGINT AUTO_INCREMENT PRIMARY KEY,"
                            + "session_code VARCHAR(10) NOT NULL UNIQUE,"
                            + "host_player_id BIGINT NOT NULL,"
                            + "max_players INT DEFAULT 4,"
                            + "status VARCHAR(20) NOT NULL DEFAULT 'WAITING',"
                            + "current_level INT DEFAULT 1,"
                            + "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                            + "CONSTRAINT fk_session_host FOREIGN KEY (host_player_id) REFERENCES players(id) ON DELETE CASCADE)");

            // -- session_players
            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS session_players ("
                            + "id BIGINT AUTO_INCREMENT PRIMARY KEY,"
                            + "session_id BIGINT NOT NULL,"
                            + "player_id BIGINT NOT NULL,"
                            + "position_x DOUBLE DEFAULT 0,"
                            + "position_y DOUBLE DEFAULT 0,"
                            + "health INT DEFAULT 100,"
                            + "is_alive BOOLEAN DEFAULT TRUE,"
                            + "joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                            + "CONSTRAINT fk_sp_session FOREIGN KEY (session_id) REFERENCES game_sessions(id) ON DELETE CASCADE,"
                            + "CONSTRAINT fk_sp_player FOREIGN KEY (player_id) REFERENCES players(id) ON DELETE CASCADE)");
        }
    }

    @AfterAll
    static void tearDown() {
        if (pool != null) {
            pool.shutdown();
        }
    }

    /** Утілітний метод для створення тестового гравця. */
    protected static com.lost.database.entity.Player createTestPlayer(String username) {
        com.lost.database.entity.Player player = new com.lost.database.entity.Player();
        player.setUsername(username);
        player.setPasswordHash("hash_" + username);
        player.setEmail(username + "@test.com");
        return player;
    }
}
