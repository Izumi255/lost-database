package com.lost.database.service;

import com.lost.database.dao.InventoryItemDao;
import com.lost.database.entity.InventoryItem;
import com.lost.database.pool.ConnectionPool;
import java.util.List;
import java.util.Optional;

/**
 * Сервіс бізнес-логіки для інвентарю гравця.
 *
 * <p>Управління предметами: додавання, видалення, перегляд.
 */
public class InventoryService {

    private final InventoryItemDao inventoryDao;

    public InventoryService(ConnectionPool pool) {
        this.inventoryDao = new InventoryItemDao(pool);
    }

    /** Отримати всі предмети гравця. */
    public List<InventoryItem> getPlayerInventory(Long playerId) {
        return inventoryDao.findByPlayerId(playerId);
    }

    /** Додати предмет до інвентарю гравця. */
    public InventoryItem addItem(Long playerId, String itemName, int quantity) {
        InventoryItem item = new InventoryItem();
        item.setPlayerId(playerId);
        item.setItemName(itemName);
        item.setQuantity(quantity);
        inventoryDao.save(item);
        return item;
    }

    /** Оновити кількість предмета. */
    public void updateQuantity(Long itemId, int newQuantity) {
        inventoryDao
                .findById(itemId)
                .ifPresent(
                        item -> {
                            item.setQuantity(newQuantity);
                            inventoryDao.update(item);
                        });
    }

    /** Видалити предмет з інвентарю. */
    public void removeItem(Long itemId) {
        inventoryDao.deleteById(itemId);
    }

    /** Знайти предмет за ID. */
    public Optional<InventoryItem> findById(Long id) {
        return inventoryDao.findById(id);
    }
}
