package com.lost.database.dao;

import static org.junit.jupiter.api.Assertions.*;

import com.lost.database.entity.Player;
import com.lost.database.entity.PlayerAchievement;
import java.util.List;
import org.junit.jupiter.api.*;

/** Інтеграційні тести для PlayerAchievementDao (таблиця player_achievements). */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PlayerAchievementDaoTest extends BaseDaoTest {

    private static PlayerAchievementDao achievementDao;
    private static PlayerDao playerDao;
    private static Long playerId;
    private static Long achievementId;

    @BeforeAll
    static void setUp() {
        playerDao = new PlayerDao(pool);
        achievementDao = new PlayerAchievementDao(pool);

        Player player = createTestPlayer("achievement_test_player");
        playerDao.save(player);
        playerId = player.getId();
    }

    @Test
    @Order(1)
    @DisplayName("save() — додавання досягнення гравцю")
    void testSaveAchievement() {
        PlayerAchievement achievement = new PlayerAchievement();
        achievement.setPlayerId(playerId);
        achievement.setAchievementCode("FIRST_KILL");
        achievement.setAchievementName("Перше вбивство");
        achievement.setDescription("Вбити першого ворога");

        achievementDao.save(achievement);

        assertNotNull(achievement.getId());
        achievementId = achievement.getId();
    }

    @Test
    @Order(2)
    @DisplayName("findByPlayerId() — пошук досягнень за ID гравця")
    void testFindByPlayerId() {
        // Додамо ще одне
        PlayerAchievement a2 = new PlayerAchievement();
        a2.setPlayerId(playerId);
        a2.setAchievementCode("EXPLORER");
        a2.setAchievementName("Дослідник");
        a2.setDescription("Відвідати всі локації рівня 1");
        achievementDao.save(a2);

        List<PlayerAchievement> achievements = achievementDao.findByPlayerId(playerId);

        assertTrue(achievements.size() >= 2);
    }

    @Test
    @Order(3)
    @DisplayName("findById() — пошук конкретного досягнення")
    void testFindById() {
        var found = achievementDao.findById(achievementId);

        assertTrue(found.isPresent());
        assertEquals("FIRST_KILL", found.get().getAchievementCode());
    }

    @Test
    @Order(4)
    @DisplayName("deleteById() — видалення досягнення")
    void testDeleteAchievement() {
        achievementDao.deleteById(achievementId);

        assertFalse(achievementDao.findById(achievementId).isPresent());
    }
}
