package com.lost.database.service;

import com.lost.database.dao.PlayerAchievementDao;
import com.lost.database.entity.PlayerAchievement;
import com.lost.database.pool.ConnectionPool;
import java.util.List;

/**
 * Сервіс бізнес-логіки для досягнень гравця.
 *
 * <p>Видача, перевірка та перегляд ачівментів.
 */
public class AchievementService {

    private final PlayerAchievementDao achievementDao;

    public AchievementService(ConnectionPool pool) {
        this.achievementDao = new PlayerAchievementDao(pool);
    }

    /** Отримати всі досягнення гравця. */
    public List<PlayerAchievement> getPlayerAchievements(Long playerId) {
        return achievementDao.findByPlayerId(playerId);
    }

    /** Видати досягнення гравцю (якщо ще не має). */
    public boolean grantAchievement(Long playerId, String achievementName) {
        // Перевіряємо, чи вже є таке досягнення
        boolean alreadyHas =
                achievementDao.findByPlayerId(playerId).stream()
                        .anyMatch(a -> a.getAchievementName().equals(achievementName));

        if (alreadyHas) {
            return false;
        }

        PlayerAchievement achievement = new PlayerAchievement();
        achievement.setPlayerId(playerId);
        achievement.setAchievementName(achievementName);
        achievementDao.save(achievement);
        return true;
    }

    /** Перевірити, чи гравець має певне досягнення. */
    public boolean hasAchievement(Long playerId, String achievementName) {
        return achievementDao.findByPlayerId(playerId).stream()
                .anyMatch(a -> a.getAchievementName().equals(achievementName));
    }

    /** Порахувати кількість досягнень гравця. */
    public int countAchievements(Long playerId) {
        return achievementDao.findByPlayerId(playerId).size();
    }
}
