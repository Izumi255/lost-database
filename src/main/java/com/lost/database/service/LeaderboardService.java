package com.lost.database.service;

import com.lost.database.dao.LeaderboardEntryDao;
import com.lost.database.entity.LeaderboardEntry;
import com.lost.database.pool.ConnectionPool;
import java.util.List;
import java.util.Optional;

/**
 * Сервіс бізнес-логіки для таблиці лідерів.
 *
 * <p>Рейтингова система: TOP-10, оновлення рекордів, позиція гравця.
 */
public class LeaderboardService {

    private final LeaderboardEntryDao leaderboardDao;

    public LeaderboardService(ConnectionPool pool) {
        this.leaderboardDao = new LeaderboardEntryDao(pool);
    }

    /** Отримати TOP-10 гравців за рахунком. */
    public List<LeaderboardEntry> getTop10() {
        return leaderboardDao.findTop10();
    }

    /** Оновити або створити запис гравця в лідерборді. */
    public void updateEntry(Long playerId, int score, int levelCompleted) {
        // Шукаємо існуючий запис
        Optional<LeaderboardEntry> existing =
                leaderboardDao.findAll().stream()
                        .filter(e -> e.getPlayerId().equals(playerId))
                        .findFirst();

        if (existing.isPresent()) {
            LeaderboardEntry entry = existing.get();
            if (score > entry.getScore()) {
                entry.setScore(score);
                entry.setLevelCompleted(levelCompleted);
                leaderboardDao.update(entry);
            }
        } else {
            LeaderboardEntry entry = new LeaderboardEntry();
            entry.setPlayerId(playerId);
            entry.setScore(score);
            entry.setLevelCompleted(levelCompleted);
            leaderboardDao.save(entry);
        }
    }

    /** Отримати позицію гравця в рейтингу (1-based). */
    public int getPlayerRank(Long playerId) {
        List<LeaderboardEntry> all = leaderboardDao.findTop10();
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).getPlayerId().equals(playerId)) {
                return i + 1;
            }
        }
        return -1; // Не в TOP-10
    }
}
