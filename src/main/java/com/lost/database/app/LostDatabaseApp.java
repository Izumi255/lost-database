package com.lost.database.app;

import com.lost.database.pool.ConnectionPool;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.flywaydb.core.Flyway;

/** Головний JavaFX додаток для демонстрації входу та реєстрації. */
public class LostDatabaseApp extends Application {

    private static ConnectionPool connectionPool;
    private static org.h2.tools.Server h2WebServer;

    @Override
    public void start(Stage stage) throws Exception {
        // 0. Запуск H2 Web Console (для зручного візуального перегляду бази даних)
        try {
            h2WebServer = org.h2.tools.Server.createWebServer("-web", "-webAllowOthers", "-webPort", "8082").start();
            System.out.println("\n🚀 [DATABASE] H2 Web Console успішно запущено! Відкрийте у браузері: http://localhost:8082");
            System.out.println("   - JDBC URL: jdbc:h2:file:~/.lost-database/data/lostdb;AUTO_SERVER=TRUE");
            System.out.println("   - Користувач (User Name): sa");
            System.out.println("   - Пароль (Password): (залиште порожнім)\n");
        } catch (Exception e) {
            System.err.println("⚠️ Не вдалося запустити H2 Web Console: " + e.getMessage());
        }

        // 1. Ініціалізація БД (Flyway міграції)
        Flyway flyway =
                Flyway.configure()
                        .dataSource("jdbc:h2:file:~/.lost-database/data/lostdb;AUTO_SERVER=TRUE", "sa", "")
                        .locations("classpath:db/migration")
                        .baselineOnMigrate(true)
                        .load();
        flyway.migrate();

        // 2. Створення пулу з'єднань
        connectionPool = ConnectionPool.createDefault();

        // 3. Завантаження UI
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login_view.fxml"));
        Parent root = loader.load();

        Scene scene = new Scene(root, 800, 600);
        scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());

        stage.setTitle("LOST — Авторизація");
        stage.setScene(scene);
        com.lost.database.infrastructure.SettingsManager settings =
                new com.lost.database.infrastructure.SettingsManager();
        if (settings.isFullscreen()) {
            stage.setMaximized(true);
        } else {
            stage.setMaximized(false);
            String val = settings.getResolution();
            if (!"FULLSCREEN".equals(val)) {
                String[] parts = val.split(" x ");
                stage.setWidth(Double.parseDouble(parts[0]));
                stage.setHeight(Double.parseDouble(parts[1]));
            }
        }
        stage.show();
    }

    @Override
    public void stop() {
        if (connectionPool != null) {
            connectionPool.shutdown();
        }
        if (h2WebServer != null) {
            h2WebServer.stop();
            System.out.println("🛑 [DATABASE] H2 Web Console зупинено.");
        }
    }

    public static ConnectionPool getConnectionPool() {
        return connectionPool;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
