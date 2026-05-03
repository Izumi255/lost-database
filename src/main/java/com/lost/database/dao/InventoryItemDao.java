package com.lost.database.dao;

import com.lost.database.entity.InventoryItem;
import com.lost.database.pool.ConnectionPool;
import java.util.List;

public class InventoryItemDao extends GenericDao<InventoryItem, Long> {
    public InventoryItemDao(ConnectionPool pool) {
        super(pool, InventoryItem.class, "inventory_items");
    }

    public List<InventoryItem> findByPlayerId(Long playerId) {
        return findByField("player_id", playerId);
    }
}
