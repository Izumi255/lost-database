package com.lost.database.dao;

import com.lost.database.entity.GameSession;
import com.lost.database.pool.ConnectionPool;
import java.util.List;

public class GameSessionDao extends GenericDao<GameSession, Long> {
    public GameSessionDao(ConnectionPool pool) {
        super(pool, GameSession.class, "game_sessions");
    }

    public List<GameSession> findByHostId(Long hostId) {
        return findByField("host_id", hostId);
    }
}
