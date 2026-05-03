package com.lost.database.dao;

import com.lost.database.entity.Npc;
import com.lost.database.pool.ConnectionPool;
import java.util.List;

public class NpcDao extends GenericDao<Npc, Long> {
    public NpcDao(ConnectionPool pool) {
        super(pool, Npc.class, "npcs");
    }

    public List<Npc> findByLevelNumber(int levelNumber) {
        return findByField("level_number", levelNumber);
    }
}
