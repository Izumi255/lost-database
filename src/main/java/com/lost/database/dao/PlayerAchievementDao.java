package com.lost.database.dao;

import com.lost.database.entity.PlayerAchievement;
import com.lost.database.pool.ConnectionPool;
import java.util.List;

public class PlayerAchievementDao extends GenericDao<PlayerAchievement, Long> {
    public PlayerAchievementDao(ConnectionPool pool) {
        super(pool, PlayerAchievement.class, "player_achievements");
    }

    public List<PlayerAchievement> findByPlayerId(Long playerId) {
        return findByField("player_id", playerId);
    }
}
