package com.lost.database.dao;

import static org.junit.jupiter.api.Assertions.*;

import com.lost.database.entity.GameSave;
import com.lost.database.entity.Player;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.*;

/** Інтеграційні тести для GameSaveDao (таблиця game_saves). */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class GameSaveDaoTest extends BaseDaoTest {

    private static GameSaveDao gameSaveDao;
    private static PlayerDao playerDao;
    private static Long playerId;
    private static Long saveId;

    @BeforeAll
    static void setUp() {
        playerDao = new PlayerDao(pool);
        gameSaveDao = new GameSaveDao(pool);

        // Створюємо гравця, від якого залежать збереження
        Player player = createTestPlayer("save_test_player");
        playerDao.save(player);
        playerId = player.getId();
    }

    @Test
    @Order(1)
    @DisplayName("save() — створення нового збереження гри")
    void testSaveNewGameSave() {
        GameSave save = new GameSave();
        save.setPlayerId(playerId);
        save.setCurrentLevel(2);
        save.setHealth(85);
        save.setPositionX(120.5);
        save.setPositionY(340.0);
        save.setSaveName("Checkpoint Alpha");

        gameSaveDao.save(save);

        assertNotNull(save.getId());
        saveId = save.getId();
    }

    @Test
    @Order(2)
    @DisplayName("findById() — пошук збереження за ID")
    void testFindById() {
        Optional<GameSave> found = gameSaveDao.findById(saveId);

        assertTrue(found.isPresent());
        assertEquals("Checkpoint Alpha", found.get().getSaveName());
        assertEquals(85, found.get().getHealth());
    }

    @Test
    @Order(3)
    @DisplayName("findByPlayerId() — пошук всіх збережень одного гравця")
    void testFindByPlayerId() {
        // Додамо ще одне збереження
        GameSave save2 = new GameSave();
        save2.setPlayerId(playerId);
        save2.setCurrentLevel(3);
        save2.setHealth(50);
        save2.setPositionX(200.0);
        save2.setPositionY(100.0);
        save2.setSaveName("Boss Room");
        gameSaveDao.save(save2);

        List<GameSave> saves = gameSaveDao.findByPlayerId(playerId);

        assertTrue(saves.size() >= 2, "Має бути щонайменше 2 збереження");
    }

    @Test
    @Order(4)
    @DisplayName("save() — оновлення існуючого збереження (UPDATE)")
    void testUpdateGameSave() {
        GameSave save = gameSaveDao.findById(saveId).orElseThrow();
        save.setHealth(100);
        save.setSaveName("Updated Checkpoint");
        gameSaveDao.save(save);

        GameSave updated = gameSaveDao.findById(saveId).orElseThrow();
        assertEquals(100, updated.getHealth());
        assertEquals("Updated Checkpoint", updated.getSaveName());
    }

    @Test
    @Order(5)
    @DisplayName("deleteById() — видалення збереження")
    void testDeleteGameSave() {
        gameSaveDao.deleteById(saveId);

        assertFalse(gameSaveDao.findById(saveId).isPresent());
    }
}
