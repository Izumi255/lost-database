package com.lost.database.dao;

import com.lost.database.entity.LeaderboardEntry;
import com.lost.database.pool.ConnectionPool;
import java.util.List;

public class LeaderboardEntryDao extends GenericDao<LeaderboardEntry, Long> {
    public LeaderboardEntryDao(ConnectionPool pool) {
        super(pool, LeaderboardEntry.class, "leaderboard_entries");
    }

    public List<LeaderboardEntry> findTop10() {
        String sql = "SELECT * FROM leaderboard_entries ORDER BY score DESC LIMIT 10";
        return executeQuery(sql, stmt -> {});
    }
}
