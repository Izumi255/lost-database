package com.lost.database.pool;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Власна реалізація пулу з'єднань (Connection Pool).
 *
 * <p>Використовує Proxy для перевизначення close, повертаючи з'єднання в пул автоматично.
 */
public class ConnectionPool {

    private final BlockingQueue<Connection> availableConnections;
    private final String url;
    private final String user;
    private final String password;
    private final int maxConnections;
    private final boolean autoCommit;
    private final AtomicBoolean isInitialized = new AtomicBoolean(false);

    public ConnectionPool(PoolConfig config) {
        this.url = config.url;
        this.user = config.user;
        this.password = config.password;
        this.maxConnections = config.maxConnections;
        this.autoCommit = config.autoCommit;
        this.availableConnections = new ArrayBlockingQueue<>(maxConnections);
        initializePool();
    }

    public static ConnectionPool createDefault() {
        return new ConnectionPool(new PoolConfig.Builder().build());
    }

    private void initializePool() {
        if (isInitialized.compareAndSet(false, true)) {
            for (int i = 0; i < maxConnections; i++) {
                try {
                    availableConnections.add(createProxyConnection());
                } catch (SQLException e) {
                    throw new RuntimeException("Помилка ініціалізації пулу з'єднань", e);
                }
            }
            System.out.println(
                    "[ConnectionPool] Initialized with "
                            + maxConnections
                            + " connections to: "
                            + url);
        }
    }

    private Connection createProxyConnection() throws SQLException {
        Connection connection = DriverManager.getConnection(url, user, password);
        connection.setAutoCommit(autoCommit);
        return (Connection)
                Proxy.newProxyInstance(
                        ConnectionPool.class.getClassLoader(),
                        new Class[] {Connection.class},
                        (proxy, method, args) -> {
                            if ("close".equals(method.getName())) {
                                try {
                                    connection.setAutoCommit(autoCommit);
                                } catch (SQLException e) {
                                    System.err.println(
                                            "Не вдалось встановити autoCommit: " + e.getMessage());
                                }
                                availableConnections.offer((Connection) proxy);
                                return null; // Don't actually close
                            }
                            return method.invoke(connection, args);
                        });
    }

    public Connection getConnection() throws SQLException {
        try {
            Connection connection = availableConnections.take();
            if (connection.isClosed()) {
                Connection newConnection = createProxyConnection();
                availableConnections.offer(newConnection);
                return newConnection;
            }
            return connection;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SQLException("Перервано очікування з'єднання", e);
        }
    }

    public void shutdown() {
        try {
            for (Connection connection : availableConnections) {
                if (!connection.isClosed()) {
                    connection.unwrap(Connection.class).close(); // physically close
                }
            }
            availableConnections.clear();
            isInitialized.set(false);
            System.out.println("[ConnectionPool] Shut down. All connections closed.");
        } catch (SQLException e) {
            throw new RuntimeException("Помилка закриття пулу з'єднань", e);
        }
    }

    public static class PoolConfig {
        private static final String DEFAULT_URL = "jdbc:h2:file:~/.lost-database/data/lostdb";
        private static final String DEFAULT_USER = "sa";
        private static final String DEFAULT_PASSWORD = "";
        private static final int DEFAULT_MAX_CONNECTIONS = 5;
        private static final boolean DEFAULT_AUTO_COMMIT = true;

        private final String url;
        private final String user;
        private final String password;
        private final int maxConnections;
        private final boolean autoCommit;

        private PoolConfig(Builder builder) {
            this.url = builder.url;
            this.user = builder.user;
            this.password = builder.password;
            this.maxConnections = builder.maxConnections;
            this.autoCommit = builder.autoCommit;
        }

        public static class Builder {
            private String url = DEFAULT_URL;
            private String user = DEFAULT_USER;
            private String password = DEFAULT_PASSWORD;
            private int maxConnections = DEFAULT_MAX_CONNECTIONS;
            private boolean autoCommit = DEFAULT_AUTO_COMMIT;

            public Builder withUrl(String url) {
                this.url = url;
                return this;
            }

            public Builder withUser(String user) {
                this.user = user;
                return this;
            }

            public Builder withPassword(String password) {
                this.password = password;
                return this;
            }

            public Builder withMaxConnections(int maxConnections) {
                this.maxConnections = Math.max(1, maxConnections);
                return this;
            }

            public Builder withAutoCommit(boolean autoCommit) {
                this.autoCommit = autoCommit;
                return this;
            }

            public PoolConfig build() {
                return new PoolConfig(this);
            }
        }
    }
}
