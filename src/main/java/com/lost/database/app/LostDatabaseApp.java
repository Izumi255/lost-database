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

    @Override
    public void start(Stage stage) throws Exception {
        // 1. Ініціалізація БД (Flyway міграції)
        Flyway flyway =
                Flyway.configure()
                        .dataSource("jdbc:h2:file:./data/lostdb", "sa", "")
                        .locations("classpath:db/migration")
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
    }

    public static ConnectionPool getConnectionPool() {
        return connectionPool;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
