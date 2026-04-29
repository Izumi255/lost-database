package com.lost.database;

/**
 * Конфігурація з'єднання з базою даних.
 */
public class DatabaseConfig {

    private String url;
    private String username;
    private String password;
    private String driver;

    public DatabaseConfig() {}

    public DatabaseConfig(String url, String username, String password, String driver) {
        this.url = url;
        this.username = username;
        this.password = password;
        this.driver = driver;
    }

    /**
     * Створює конфігурацію за замовчуванням для H2.
     */
    public static DatabaseConfig defaultH2() {
        return new DatabaseConfig(
                "jdbc:h2:file:./data/lostdb",
                "sa",
                "",
                "org.h2.Driver"
        );
    }

    // Getters & Setters
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getDriver() { return driver; }
    public void setDriver(String driver) { this.driver = driver; }

    @Override
    public String toString() {
        return "DatabaseConfig{url='" + url + "', username='" + username + "', driver='" + driver + "'}";
    }
}
