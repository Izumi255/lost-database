package com.lost.database.dao;

import static org.junit.jupiter.api.Assertions.*;

import com.lost.database.entity.LeaderboardEntry;
import com.lost.database.entity.Player;
import java.util.List;
import org.junit.jupiter.api.*;

/** Інтеграційні тести для LeaderboardEntryDao (таблиця leaderboard_entries). */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class LeaderboardEntryDaoTest extends BaseDaoTest {

    private static LeaderboardEntryDao lbDao;
    private static PlayerDao playerDao;
    private static Long playerId;

    @BeforeAll
    static void setUp() {
        playerDao = new PlayerDao(pool);
        lbDao = new LeaderboardEntryDao(pool);

        Player player = createTestPlayer("leaderboard_test_player");
        playerDao.save(player);
        playerId = player.getId();
    }

    @Test
    @Order(1)
    @DisplayName("save() — додавання записів у таблицю лідерів")
    void testSaveEntries() {
        for (int i = 1; i <= 5; i++) {
            LeaderboardEntry entry = new LeaderboardEntry();
            entry.setPlayerId(playerId);
            entry.setScore(i * 100);
            entry.setLevelCompleted(i);
            entry.setCompletionTimeSec(60.0 * i);
            lbDao.save(entry);
            assertNotNull(entry.getId());
        }
    }

    @Test
    @Order(2)
    @DisplayName("findTop10() — повертає до 10 записів відсортованих за score DESC")
    void testFindTop10() {
        List<LeaderboardEntry> top = lbDao.findTop10();

        assertFalse(top.isEmpty());
        assertTrue(top.size() <= 10);
        // Перевіряємо, що перший запис має найбільший score
        assertTrue(top.get(0).getScore() >= top.get(top.size() - 1).getScore());
    }

    @Test
    @Order(3)
    @DisplayName("findAll() — отримання всіх записів")
    void testFindAll() {
        List<LeaderboardEntry> all = lbDao.findAll();

        assertTrue(all.size() >= 5, "Має бути щонайменше 5 записів");
    }

    @Test
    @Order(4)
    @DisplayName("deleteById() — видалення запису таблиці лідерів")
    void testDeleteEntry() {
        LeaderboardEntry entry = new LeaderboardEntry();
        entry.setPlayerId(playerId);
        entry.setScore(9999);
        entry.setLevelCompleted(10);
        lbDao.save(entry);

        Long id = entry.getId();
        lbDao.deleteById(id);

        assertFalse(lbDao.findById(id).isPresent());
    }
}
