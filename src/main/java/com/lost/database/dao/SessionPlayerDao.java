package com.lost.database.dao;

import com.lost.database.entity.SessionPlayer;
import com.lost.database.pool.ConnectionPool;
import java.util.List;

public class SessionPlayerDao extends GenericDao<SessionPlayer, Long> {
    public SessionPlayerDao(ConnectionPool pool) {
        super(pool, SessionPlayer.class, "session_players");
    }

    public List<SessionPlayer> findBySessionId(Long sessionId) {
        return findByField("session_id", sessionId);
    }
}
