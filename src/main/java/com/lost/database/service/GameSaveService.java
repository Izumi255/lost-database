package com.lost.database.service;

import com.lost.database.dao.GameSaveDao;
import com.lost.database.entity.GameSave;
import com.lost.database.pool.ConnectionPool;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Сервіс бізнес-логіки для збережень гри.
 *
 * <p>Управління ігровим прогресом: збереження, завантаження, видалення.
 */
public class GameSaveService {

    private final GameSaveDao gameSaveDao;

    public GameSaveService(ConnectionPool pool) {
        this.gameSaveDao = new GameSaveDao(pool);
    }

    /** Зберегти ігровий прогрес. */
    public GameSave save(GameSave gameSave) {
        gameSaveDao.save(gameSave);
        return gameSave;
    }

    /** Отримати всі збереження гравця. */
    public List<GameSave> findByPlayer(Long playerId) {
        return gameSaveDao.findByPlayerId(playerId);
    }

    /** Отримати останнє збереження гравця. */
    public Optional<GameSave> findLatestByPlayer(Long playerId) {
        return gameSaveDao.findByPlayerId(playerId).stream()
                .max(Comparator.comparing(GameSave::getSavedAt));
    }

    /** Завантажити збереження за ID. */
    public Optional<GameSave> findById(Long id) {
        return gameSaveDao.findById(id);
    }

    /** Видалити збереження. */
    public void delete(Long saveId) {
        gameSaveDao.deleteById(saveId);
    }
}
