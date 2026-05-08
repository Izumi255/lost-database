package com.lost.database.service;

import com.lost.database.dao.PlayerDao;
import com.lost.database.entity.Player;
import com.lost.database.pool.ConnectionPool;
import java.util.List;
import java.util.Optional;

/**
 * Сервіс бізнес-логіки для гравців.
 *
 * <p>Управляє профілями гравців: CRUD, оновлення рахунку та рівня.
 */
public class PlayerService {

    private final PlayerDao playerDao;

    public PlayerService(ConnectionPool pool) {
        this.playerDao = new PlayerDao(pool);
    }

    /** Отримати гравця за ID. */
    public Optional<Player> findById(Long id) {
        return playerDao.findById(id);
    }

    /** Отримати гравця за username. */
    public Optional<Player> findByUsername(String username) {
        return playerDao.findByUsername(username);
    }

    /** Отримати список усіх гравців. */
    public List<Player> findAll() {
        return playerDao.findAll();
    }

    /** Оновити загальний рахунок гравця. */
    public void updateScore(Long playerId, int newScore) {
        playerDao
                .findById(playerId)
                .ifPresent(
                        player -> {
                            player.setTotalScore(newScore);
                            playerDao.update(player);
                        });
    }

    /** Додати очки до загального рахунку. */
    public void addScore(Long playerId, int points) {
        playerDao
                .findById(playerId)
                .ifPresent(
                        player -> {
                            player.setTotalScore(player.getTotalScore() + points);
                            playerDao.update(player);
                        });
    }

    /** Оновити максимальний досягнутий рівень. */
    public void updateMaxLevel(Long playerId, int level) {
        playerDao
                .findById(playerId)
                .ifPresent(
                        player -> {
                            if (level > player.getMaxLevelReached()) {
                                player.setMaxLevelReached(level);
                                playerDao.update(player);
                            }
                        });
    }

    /** Видалити гравця. */
    public void delete(Long playerId) {
        playerDao.deleteById(playerId);
    }
}
