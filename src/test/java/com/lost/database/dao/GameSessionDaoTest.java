package com.lost.database.dao;

import static org.junit.jupiter.api.Assertions.*;

import com.lost.database.entity.GameSession;
import com.lost.database.entity.Player;
import com.lost.database.entity.SessionPlayer;
import java.util.List;
import org.junit.jupiter.api.*;

/** Інтеграційні тести для GameSessionDao та SessionPlayerDao. */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class GameSessionDaoTest extends BaseDaoTest {

    private static GameSessionDao sessionDao;
    private static SessionPlayerDao sessionPlayerDao;
    private static PlayerDao playerDao;
    private static Long hostPlayerId;
    private static Long sessionId;
    private static Long guestPlayerId;

    @BeforeAll
    static void setUp() {
        playerDao = new PlayerDao(pool);
        sessionDao = new GameSessionDao(pool);
        sessionPlayerDao = new SessionPlayerDao(pool);

        // Створюємо хост-гравця та гостя
        Player host = createTestPlayer("session_host");
        playerDao.save(host);
        hostPlayerId = host.getId();

        Player guest = createTestPlayer("session_guest");
        playerDao.save(guest);
        guestPlayerId = guest.getId();
    }

    @Test
    @Order(1)
    @DisplayName("save() — створення нової ігрової сесії")
    void testCreateSession() {
        GameSession session = new GameSession();
        session.setSessionCode("ABCD12");
        session.setHostPlayerId(hostPlayerId);
        session.setMaxPlayers(4);
        session.setStatus("WAITING");
        session.setCurrentLevel(1);

        sessionDao.save(session);

        assertNotNull(session.getId());
        sessionId = session.getId();
    }

    @Test
    @Order(2)
    @DisplayName("findById() — пошук сесії за ID")
    void testFindSessionById() {
        var found = sessionDao.findById(sessionId);

        assertTrue(found.isPresent());
        assertEquals("ABCD12", found.get().getSessionCode());
        assertEquals("WAITING", found.get().getStatus());
    }

    @Test
    @Order(3)
    @DisplayName("findByHostId() — пошук сесій конкретного хоста")
    void testFindByHostId() {
        List<GameSession> sessions = sessionDao.findByHostId(hostPlayerId);

        assertFalse(sessions.isEmpty());
        assertEquals(hostPlayerId, sessions.get(0).getHostPlayerId());
    }

    @Test
    @Order(4)
    @DisplayName("SessionPlayerDao.save() — додавання гравця до сесії")
    void testAddPlayerToSession() {
        SessionPlayer sp = new SessionPlayer();
        sp.setSessionId(sessionId);
        sp.setPlayerId(guestPlayerId);
        sp.setPositionX(50.0);
        sp.setPositionY(50.0);
        sp.setHealth(100);
        sp.setAlive(true);

        sessionPlayerDao.save(sp);

        assertNotNull(sp.getId());
    }

    @Test
    @Order(5)
    @DisplayName("SessionPlayerDao.findBySessionId() — пошук гравців у сесії")
    void testFindPlayersBySession() {
        // Додамо хоста також
        SessionPlayer hostSp = new SessionPlayer();
        hostSp.setSessionId(sessionId);
        hostSp.setPlayerId(hostPlayerId);
        sessionPlayerDao.save(hostSp);

        List<SessionPlayer> players = sessionPlayerDao.findBySessionId(sessionId);

        assertTrue(players.size() >= 2, "Має бути щонайменше 2 гравці в сесії");
    }

    @Test
    @Order(6)
    @DisplayName("save() — оновлення статусу сесії (UPDATE)")
    void testUpdateSessionStatus() {
        GameSession session = sessionDao.findById(sessionId).orElseThrow();
        session.setStatus("IN_PROGRESS");
        sessionDao.save(session);

        GameSession updated = sessionDao.findById(sessionId).orElseThrow();
        assertEquals("IN_PROGRESS", updated.getStatus());
    }

    @Test
    @Order(7)
    @DisplayName("deleteById() — видалення сесії (каскадне видалення session_players)")
    void testDeleteSession() {
        sessionDao.deleteById(sessionId);

        assertFalse(sessionDao.findById(sessionId).isPresent());
        assertTrue(sessionPlayerDao.findBySessionId(sessionId).isEmpty());
    }
}
