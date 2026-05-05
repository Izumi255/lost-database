package com.lost.database.dao;

import static org.junit.jupiter.api.Assertions.*;

import com.lost.database.entity.Player;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.*;

/** Інтеграційні тести для PlayerDao (таблиця players). */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PlayerDaoTest extends BaseDaoTest {

    private static PlayerDao playerDao;
    private static Long savedPlayerId;

    @BeforeAll
    static void setUp() {
        playerDao = new PlayerDao(pool);
    }

    @Test
    @Order(1)
    @DisplayName("save() — збереження нового гравця з автогенерацією ID")
    void testSaveNewPlayer() {
        Player player = createTestPlayer("test_player_1");

        Player saved = playerDao.save(player);

        assertNotNull(saved.getId(), "ID має бути згенерований автоматично");
        savedPlayerId = saved.getId();
    }

    @Test
    @Order(2)
    @DisplayName("findById() — пошук гравця за ID")
    void testFindById() {
        Optional<Player> found = playerDao.findById(savedPlayerId);

        assertTrue(found.isPresent(), "Гравець має бути знайдений");
        assertEquals("test_player_1", found.get().getUsername());
    }

    @Test
    @Order(3)
    @DisplayName("findByUsername() — пошук за унікальним ім'ям")
    void testFindByUsername() {
        Optional<Player> found = playerDao.findByUsername("test_player_1");

        assertTrue(found.isPresent());
        assertEquals(savedPlayerId, found.get().getId());
    }

    @Test
    @Order(4)
    @DisplayName("findAll() — отримання списку всіх гравців")
    void testFindAll() {
        playerDao.save(createTestPlayer("test_player_2"));

        List<Player> all = playerDao.findAll();

        assertTrue(all.size() >= 2, "Має бути щонайменше 2 гравці");
    }

    @Test
    @Order(5)
    @DisplayName("save() — оновлення існуючого гравця (UPDATE)")
    void testUpdatePlayer() {
        Optional<Player> found = playerDao.findById(savedPlayerId);
        assertTrue(found.isPresent());

        Player player = found.get();
        player.setEmail("updated@test.com");
        player.setTotalScore(500);
        playerDao.save(player);

        Player updated = playerDao.findById(savedPlayerId).orElseThrow();
        assertEquals("updated@test.com", updated.getEmail());
        assertEquals(500, updated.getTotalScore());
    }

    @Test
    @Order(6)
    @DisplayName("deleteById() — видалення гравця")
    void testDeleteById() {
        Player toDelete = createTestPlayer("to_delete");
        playerDao.save(toDelete);
        assertNotNull(toDelete.getId());

        playerDao.deleteById(toDelete.getId());

        assertFalse(playerDao.findById(toDelete.getId()).isPresent());
    }
}
