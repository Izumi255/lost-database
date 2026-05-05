package com.lost.database.dao;

import static org.junit.jupiter.api.Assertions.*;

import com.lost.database.entity.InventoryItem;
import com.lost.database.entity.Player;
import java.util.List;
import org.junit.jupiter.api.*;

/** Інтеграційні тести для InventoryItemDao (таблиця inventory_items). */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class InventoryItemDaoTest extends BaseDaoTest {

    private static InventoryItemDao itemDao;
    private static PlayerDao playerDao;
    private static Long playerId;
    private static Long itemId;

    @BeforeAll
    static void setUp() {
        playerDao = new PlayerDao(pool);
        itemDao = new InventoryItemDao(pool);

        Player player = createTestPlayer("inventory_test_player");
        playerDao.save(player);
        playerId = player.getId();
    }

    @Test
    @Order(1)
    @DisplayName("save() — додавання предмета в інвентар")
    void testSaveItem() {
        InventoryItem item = new InventoryItem();
        item.setPlayerId(playerId);
        item.setItemType("WEAPON");
        item.setItemName("Кам'яна Сокира");
        item.setQuantity(1);
        item.setItemValue(50);

        itemDao.save(item);

        assertNotNull(item.getId());
        itemId = item.getId();
    }

    @Test
    @Order(2)
    @DisplayName("findByPlayerId() — пошук предметів гравця")
    void testFindByPlayerId() {
        // Додамо ще один предмет
        InventoryItem item2 = new InventoryItem();
        item2.setPlayerId(playerId);
        item2.setItemType("CONSUMABLE");
        item2.setItemName("Зілля здоров'я");
        item2.setQuantity(3);
        item2.setItemValue(25);
        itemDao.save(item2);

        List<InventoryItem> items = itemDao.findByPlayerId(playerId);

        assertTrue(items.size() >= 2);
    }

    @Test
    @Order(3)
    @DisplayName("findAll() — отримання всіх предметів")
    void testFindAll() {
        List<InventoryItem> all = itemDao.findAll();

        assertFalse(all.isEmpty());
    }

    @Test
    @Order(4)
    @DisplayName("deleteById() — видалення предмета")
    void testDeleteItem() {
        itemDao.deleteById(itemId);

        assertFalse(itemDao.findById(itemId).isPresent());
    }
}
