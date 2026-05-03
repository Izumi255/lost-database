package com.lost.database.dao;

import com.lost.database.entity.GameSave;
import com.lost.database.pool.ConnectionPool;
import java.util.List;

public class GameSaveDao extends GenericDao<GameSave, Long> {
    public GameSaveDao(ConnectionPool pool) {
        super(pool, GameSave.class, "game_saves");
    }

    public List<GameSave> findByPlayerId(Long playerId) {
        return findByField("player_id", playerId);
    }
}
