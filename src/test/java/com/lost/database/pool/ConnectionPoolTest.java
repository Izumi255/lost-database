package com.lost.database.pool;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.Connection;
import java.sql.SQLException;
import org.junit.jupiter.api.*;

/** Інтеграційні тести для ConnectionPool (Proxy-based). */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ConnectionPoolTest {

    private ConnectionPool pool;

    @BeforeEach
    void setUp() {
        ConnectionPool.PoolConfig config =
                new ConnectionPool.PoolConfig.Builder()
                        .withUrl("jdbc:h2:mem:pooltest;DB_CLOSE_DELAY=-1")
                        .withUser("sa")
                        .withPassword("")
                        .withMaxConnections(3)
                        .build();
        pool = new ConnectionPool(config);
    }

    @AfterEach
    void tearDown() {
        if (pool != null) {
            pool.shutdown();
        }
    }

    @Test
    @DisplayName("getConnection() — повертає валідне з'єднання")
    void testGetConnection() throws SQLException {
        Connection conn = pool.getConnection();

        assertNotNull(conn, "З'єднання не повинно бути null");
        assertFalse(conn.isClosed(), "З'єднання повинно бути відкритим");

        conn.close(); // Повертає назад у пул через Proxy
    }

    @Test
    @DisplayName("close() через Proxy — з'єднання повертається в пул, а не закривається")
    void testProxyCloseReturnsToPool() throws SQLException {
        Connection conn1 = pool.getConnection();
        Connection conn2 = pool.getConnection();
        Connection conn3 = pool.getConnection();
        // Пул порожній (3 з 3 зайнято)

        // Повертаємо одне з'єднання
        conn1.close();

        // Тепер ми знову можемо отримати з'єднання (якщо б close фізично закрив,
        // пул б залишився порожнім і заблокувався)
        Connection conn4 = pool.getConnection();
        assertNotNull(conn4, "Має повернути перероблене з'єднання з пулу");

        // Прибираємо
        conn2.close();
        conn3.close();
        conn4.close();
    }

    @Test
    @DisplayName("PoolConfig Builder — коректні значення за замовчуванням")
    void testPoolConfigDefaults() {
        ConnectionPool.PoolConfig config = new ConnectionPool.PoolConfig.Builder().build();
        ConnectionPool defaultPool = new ConnectionPool(config);

        assertDoesNotThrow(
                () -> {
                    Connection conn = defaultPool.getConnection();
                    conn.close();
                });

        defaultPool.shutdown();
    }

    @Test
    @DisplayName("shutdown() — коректне закриття пулу")
    void testShutdown() {
        assertDoesNotThrow(() -> pool.shutdown());
    }

    @Test
    @DisplayName("Множинні запити з try-with-resources працюють коректно")
    void testTryWithResources() {
        assertDoesNotThrow(
                () -> {
                    for (int i = 0; i < 10; i++) {
                        try (Connection conn = pool.getConnection()) {
                            assertFalse(conn.isClosed());
                            conn.createStatement().execute("SELECT 1");
                        }
                    }
                });
    }
}
