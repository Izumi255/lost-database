package com.lost.database.dao;

import com.lost.database.entity.Player;
import com.lost.database.pool.ConnectionPool;
import java.util.Optional;

public class PlayerDao extends GenericDao<Player, Long> {
    public PlayerDao(ConnectionPool pool) {
        super(pool, Player.class, "players");
    }

    public Optional<Player> findByUsername(String username) {
        return findByField("username", username).stream().findFirst();
    }

    public Optional<Player> findByEmail(String email) {
        return findByField("email", email).stream().findFirst();
    }
}
