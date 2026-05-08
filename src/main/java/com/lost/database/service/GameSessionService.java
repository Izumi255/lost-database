package com.lost.database.service;

import com.lost.database.dao.GameSessionDao;
import com.lost.database.dao.SessionPlayerDao;
import com.lost.database.entity.GameSession;
import com.lost.database.entity.SessionPlayer;
import com.lost.database.pool.ConnectionPool;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Сервіс бізнес-логіки для ігрових сесій.
 *
 * <p>Створення, керування, та приєднання до мультиплеєрних сесій.
 */
public class GameSessionService {

    private final GameSessionDao sessionDao;
    private final SessionPlayerDao sessionPlayerDao;

    public GameSessionService(ConnectionPool pool) {
        this.sessionDao = new GameSessionDao(pool);
        this.sessionPlayerDao = new SessionPlayerDao(pool);
    }

    /** Створити нову ігрову сесію з унікальним кодом. */
    public GameSession createSession(Long hostPlayerId, int maxPlayers) {
        String code = UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        GameSession session = new GameSession();
        session.setSessionCode(code);
        session.setHostPlayerId(hostPlayerId);
        session.setMaxPlayers(maxPlayers);
        session.setStatus("WAITING");
        sessionDao.save(session);

        // Автоматично додаємо хоста як гравця сесії
        SessionPlayer sp = new SessionPlayer();
        sp.setSessionId(session.getId());
        sp.setPlayerId(hostPlayerId);
        sessionPlayerDao.save(sp);

        return session;
    }

    /** Знайти сесію за кодом. */
    public Optional<GameSession> findByCode(String code) {
        return sessionDao.findAll().stream()
                .filter(s -> s.getSessionCode().equalsIgnoreCase(code.trim()))
                .findFirst();
    }

    /** Приєднати гравця до існуючої сесії. */
    public boolean joinSession(String sessionCode, Long playerId) {
        Optional<GameSession> sessionOpt = findByCode(sessionCode);
        if (sessionOpt.isEmpty()) {
            return false;
        }

        GameSession session = sessionOpt.get();
        if (!"WAITING".equals(session.getStatus())) {
            return false;
        }

        SessionPlayer sp = new SessionPlayer();
        sp.setSessionId(session.getId());
        sp.setPlayerId(playerId);
        sessionPlayerDao.save(sp);
        return true;
    }

    /** Отримати всі активні (WAITING) сесії. */
    public List<GameSession> findActiveSessions() {
        return sessionDao.findAll().stream()
                .filter(s -> "WAITING".equals(s.getStatus()))
                .collect(Collectors.toList());
    }

    /** Змінити статус сесії (WAITING → ACTIVE → FINISHED). */
    public void updateStatus(Long sessionId, String newStatus) {
        sessionDao
                .findById(sessionId)
                .ifPresent(
                        session -> {
                            session.setStatus(newStatus);
                            sessionDao.update(session);
                        });
    }

    /** Отримати всі сесії хоста. */
    public List<GameSession> findByHost(Long hostPlayerId) {
        return sessionDao.findByHostId(hostPlayerId);
    }

    /** Отримати всі сесії. */
    public List<GameSession> findAll() {
        return sessionDao.findAll();
    }
}
