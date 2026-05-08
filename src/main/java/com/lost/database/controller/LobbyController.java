package com.lost.database.controller;

import com.lost.database.app.LostDatabaseApp;
import com.lost.database.dao.*;
import com.lost.database.entity.*;
import com.lost.database.infrastructure.SettingsManager;
import com.lost.database.pool.ConnectionPool;
import java.net.URL;
import java.util.List;
import java.util.UUID;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

/**
 * Контролер лобі — головне меню після входу. Всі панелі (лідерборд, опції) відображаються як
 * in-game оверлеї без окремих вікон.
 */
public class LobbyController {

    private StackPane rootStack;
    private MediaPlayer musicPlayer;
    private Player currentPlayer;
    private ConnectionPool pool;
    private GameSessionDao sessionDao;
    private SessionPlayerDao sessionPlayerDao;
    private SettingsManager settings;

    private HBox menuBox;
    private VBox leaderboardPanel;
    private VBox optionsPanel;
    private VBox joinPanel;
    private VBox createPanel;

    public StackPane buildView(Player player) {
        this.currentPlayer = player;
        this.pool = LostDatabaseApp.getConnectionPool();
        this.sessionDao = new GameSessionDao(pool);
        this.sessionPlayerDao = new SessionPlayerDao(pool);
        this.settings = new SettingsManager();

        rootStack = new StackPane();

        // 1. Відео-фон
        setupVideoBackground();

        // 2. Темний оверлей
        StackPane overlay = new StackPane();
        overlay.setStyle("-fx-background-color: rgba(10, 15, 12, 0.2);");
        rootStack.getChildren().add(overlay);

        // 3. Кнопки внизу
        menuBox = new HBox(40);
        menuBox.setAlignment(Pos.BOTTOM_CENTER);
        menuBox.setPadding(new Insets(0, 0, 50, 0));

        Button btnNewGame = createTextButton("NEW GAME");
        btnNewGame.setOnAction(e -> showCreatePanel());

        Button btnContinue = createTextButton("CONTINUE");
        btnContinue.setOnAction(e -> showJoinPanel());

        Button btnLeaderboard = createTextButton("LEADERBOARD");
        btnLeaderboard.setOnAction(e -> showLeaderboard());

        Button btnOptions = createTextButton("OPTIONS");
        btnOptions.setOnAction(e -> showOptions());

        Button btnQuit = createTextButton("QUIT");
        btnQuit.setOnAction(e -> onLogout());

        menuBox.getChildren().addAll(btnNewGame, btnContinue, btnLeaderboard, btnOptions, btnQuit);

        // Анімація входу
        menuBox.setOpacity(0);
        menuBox.setTranslateY(20);
        FadeTransition ftMenu = new FadeTransition(Duration.millis(1200), menuBox);
        ftMenu.setFromValue(0);
        ftMenu.setToValue(1);
        ftMenu.setDelay(Duration.millis(600));
        ftMenu.play();
        TranslateTransition ttMenu = new TranslateTransition(Duration.millis(1200), menuBox);
        ttMenu.setFromY(20);
        ttMenu.setToY(0);
        ttMenu.setDelay(Duration.millis(600));
        ttMenu.play();

        rootStack.getChildren().add(menuBox);

        // 4. Створюємо оверлей-панелі (приховані)
        buildLeaderboardPanel();
        buildOptionsPanel();
        buildJoinPanel();
        buildCreatePanel();

        // 5. Музика
        setupMusic();

        return rootStack;
    }

    // ═══════════════════════════════════════════════════════
    // ПОБУДОВА ОВЕРЛЕЙ-ПАНЕЛЕЙ
    // ═══════════════════════════════════════════════════════

    private void buildLeaderboardPanel() {
        leaderboardPanel = createOverlayPanel("LEADERBOARD");

        VBox listBox = new VBox(8);
        listBox.setAlignment(Pos.CENTER);

        var leaderboardDao = new LeaderboardEntryDao(pool);
        List<LeaderboardEntry> top = leaderboardDao.findTop10();

        if (top.isEmpty()) {
            Label empty = new Label("NO DATA YET");
            empty.setFont(loadRetroFont(12));
            empty.setTextFill(Color.gray(0.5));
            listBox.getChildren().add(empty);
        } else {
            int rank = 1;
            for (LeaderboardEntry entry : top) {
                HBox row = new HBox(20);
                row.setAlignment(Pos.CENTER);

                Label rankLabel = new Label("#" + rank);
                rankLabel.setFont(loadRetroFont(14));
                rankLabel.setTextFill(rank <= 3 ? Color.rgb(255, 204, 0) : Color.WHITE);
                rankLabel.setMinWidth(60);

                Label scoreLabel = new Label("SCORE: " + entry.getScore());
                scoreLabel.setFont(loadRetroFont(11));
                scoreLabel.setTextFill(Color.rgb(0, 255, 170));
                scoreLabel.setMinWidth(180);

                Label levelLabel = new Label("LVL " + entry.getLevelCompleted());
                levelLabel.setFont(loadRetroFont(11));
                levelLabel.setTextFill(Color.rgb(68, 204, 255));

                row.getChildren().addAll(rankLabel, scoreLabel, levelLabel);
                listBox.getChildren().add(row);
                rank++;
            }
        }

        ScrollPane scroll = new ScrollPane(listBox);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        scroll.setFitToWidth(true);
        scroll.setMaxHeight(350);

        Button btnBack = createTextButton("BACK");
        btnBack.setOnAction(e -> hidePanel(leaderboardPanel));

        leaderboardPanel.getChildren().addAll(scroll, btnBack);
        leaderboardPanel.setVisible(false);
        leaderboardPanel.setOpacity(0);
        rootStack.getChildren().add(leaderboardPanel);
    }

    private void buildOptionsPanel() {
        optionsPanel = createOverlayPanel("OPTIONS");

        // ── Гучність музики ──
        VBox volumeBox = new VBox(10);
        volumeBox.setAlignment(Pos.CENTER);

        Label volLabel = new Label("MUSIC VOLUME");
        volLabel.setFont(loadRetroFont(12));
        volLabel.setTextFill(Color.LIGHTGRAY);

        Slider volSlider = new Slider(0, 100, settings.getVolume());
        volSlider.setStyle("-fx-control-inner-background: #444; -fx-cursor: hand;");
        volSlider.setMaxWidth(300);
        volSlider
                .valueProperty()
                .addListener(
                        (obs, old, val) -> {
                            if (musicPlayer != null) {
                                musicPlayer.setVolume(val.doubleValue() / 100.0);
                            }
                            settings.setVolume(val.intValue());
                        });

        // Лейбл з поточним значенням
        Label volValue = new Label(settings.getVolume() + "%");
        volValue.setFont(loadRetroFont(10));
        volValue.setTextFill(Color.web("#00ffaa"));
        volSlider
                .valueProperty()
                .addListener((obs, old, val) -> volValue.setText(val.intValue() + "%"));

        volumeBox.getChildren().addAll(volLabel, volSlider, volValue);

        // ── Роздільна здатність ──
        VBox resBox = new VBox(10);
        resBox.setAlignment(Pos.CENTER);

        Label resLabel = new Label("RESOLUTION");
        resLabel.setFont(loadRetroFont(12));
        resLabel.setTextFill(Color.LIGHTGRAY);

        ComboBox<String> resCombo = new ComboBox<>();
        resCombo.getItems()
                .addAll(
                        "800 x 600",
                        "1024 x 768",
                        "1280 x 720",
                        "1366 x 768",
                        "1600 x 900",
                        "1920 x 1080");
        // Завантажуємо збережену роздільну здатність
        String savedRes = settings.getResolution();
        if (!"FULLSCREEN".equals(savedRes) && resCombo.getItems().contains(savedRes)) {
            resCombo.setValue(savedRes);
        } else {
            resCombo.setValue("1920 x 1080");
        }
        resCombo.setStyle(
                "-fx-font-size: 14px; -fx-background-color: rgba(0,0,0,0.6);"
                        + "-fx-text-fill: white; -fx-border-color: rgba(100,150,200,0.4);"
                        + "-fx-border-radius: 6; -fx-background-radius: 6;");

        // ── Fullscreen галочка ──
        CheckBox fullscreenCheck = new CheckBox("FULLSCREEN");
        fullscreenCheck.setSelected(settings.isFullscreen());
        fullscreenCheck.setFont(loadRetroFont(11));
        fullscreenCheck.setTextFill(Color.LIGHTGRAY);
        fullscreenCheck.setStyle("-fx-cursor: hand;");

        // Якщо fullscreen включено — дізейблимо combo
        resCombo.setDisable(fullscreenCheck.isSelected());

        fullscreenCheck.setOnAction(
                e -> {
                    boolean fs = fullscreenCheck.isSelected();
                    resCombo.setDisable(fs);
                    settings.setFullscreen(fs);

                    javafx.stage.Stage stage =
                            (javafx.stage.Stage) rootStack.getScene().getWindow();
                    if (fs) {
                        stage.setMaximized(true);
                        settings.setResolution("FULLSCREEN");
                    } else {
                        stage.setMaximized(false);
                        String val = resCombo.getValue();
                        if (val != null) {
                            String[] parts = val.split(" x ");
                            stage.setWidth(Double.parseDouble(parts[0]));
                            stage.setHeight(Double.parseDouble(parts[1]));
                            stage.centerOnScreen();
                            settings.setResolution(val);
                        }
                    }
                    settings.save();
                });

        resCombo.setOnAction(
                e -> {
                    if (!fullscreenCheck.isSelected()) {
                        String val = resCombo.getValue();
                        if (val != null) {
                            javafx.stage.Stage stage =
                                    (javafx.stage.Stage) rootStack.getScene().getWindow();
                            stage.setMaximized(false);
                            String[] parts = val.split(" x ");
                            stage.setWidth(Double.parseDouble(parts[0]));
                            stage.setHeight(Double.parseDouble(parts[1]));
                            stage.centerOnScreen();
                            settings.setResolution(val);
                            settings.save();
                        }
                    }
                });

        resBox.getChildren().addAll(resLabel, resCombo, fullscreenCheck);

        Button btnBack = createTextButton("BACK");
        btnBack.setOnAction(
                e -> {
                    settings.save(); // Зберігаємо налаштування при виході
                    hidePanel(optionsPanel);
                });

        optionsPanel.getChildren().addAll(volumeBox, resBox, btnBack);
        optionsPanel.setVisible(false);
        optionsPanel.setOpacity(0);
        rootStack.getChildren().add(optionsPanel);
    }

    private void buildJoinPanel() {
        joinPanel = createOverlayPanel("JOIN GAME");

        Label promptLabel = new Label("ENTER SESSION CODE:");
        promptLabel.setFont(loadRetroFont(12));
        promptLabel.setTextFill(Color.LIGHTGRAY);

        TextField codeField = new TextField();
        codeField.setPromptText("CODE");
        codeField.setMaxWidth(250);
        codeField.setStyle(
                "-fx-font-size: 18px; -fx-background-color: rgba(0,0,0,0.6);"
                        + "-fx-text-fill: white; -fx-prompt-text-fill: #666;"
                        + "-fx-border-color: rgba(100,150,200,0.4); -fx-border-radius: 8;"
                        + "-fx-background-radius: 8; -fx-padding: 10; -fx-alignment: center;");

        Label statusLabel = new Label("");
        statusLabel.setFont(loadRetroFont(10));

        Button btnJoin = createTextButton("JOIN");
        btnJoin.setTextFill(Color.rgb(0, 255, 170));
        btnJoin.setOnAction(
                e -> {
                    String code = codeField.getText().trim();
                    if (code.isEmpty()) {
                        statusLabel.setTextFill(Color.rgb(255, 80, 80));
                        statusLabel.setText("ENTER A CODE!");
                        return;
                    }
                    statusLabel.setTextFill(Color.YELLOW);
                    statusLabel.setText("SEARCHING...");

                    new Thread(
                                    () -> {
                                        var sessions = sessionDao.findAll();
                                        var found =
                                                sessions.stream()
                                                        .filter(
                                                                s ->
                                                                        s.getSessionCode()
                                                                                .equalsIgnoreCase(
                                                                                        code))
                                                        .findFirst();

                                        Platform.runLater(
                                                () -> {
                                                    if (found.isPresent()) {
                                                        SessionPlayer sp = new SessionPlayer();
                                                        sp.setSessionId(found.get().getId());
                                                        sp.setPlayerId(currentPlayer.getId());
                                                        sessionPlayerDao.save(sp);
                                                        statusLabel.setTextFill(
                                                                Color.rgb(0, 255, 170));
                                                        statusLabel.setText(
                                                                "JOINED: " + code + "!");
                                                    } else {
                                                        statusLabel.setTextFill(
                                                                Color.rgb(255, 80, 80));
                                                        statusLabel.setText("SESSION NOT FOUND!");
                                                    }
                                                });
                                    })
                            .start();
                });

        Button btnBack = createTextButton("BACK");
        btnBack.setOnAction(e -> hidePanel(joinPanel));

        joinPanel.getChildren().addAll(promptLabel, codeField, btnJoin, statusLabel, btnBack);
        joinPanel.setVisible(false);
        joinPanel.setOpacity(0);
        rootStack.getChildren().add(joinPanel);
    }

    private void buildCreatePanel() {
        createPanel = createOverlayPanel("NEW GAME");

        Label infoLabel = new Label("CREATE A NEW SESSION");
        infoLabel.setFont(loadRetroFont(12));
        infoLabel.setTextFill(Color.LIGHTGRAY);

        Label codeLabel = new Label("");
        codeLabel.setFont(loadRetroFont(18));
        codeLabel.setTextFill(Color.rgb(0, 255, 170));

        Label statusLabel = new Label("");
        statusLabel.setFont(loadRetroFont(10));

        Button btnCreate = createTextButton("CREATE");
        btnCreate.setTextFill(Color.rgb(0, 255, 170));
        btnCreate.setOnAction(
                e -> {
                    String code = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
                    statusLabel.setTextFill(Color.YELLOW);
                    statusLabel.setText("CREATING...");

                    new Thread(
                                    () -> {
                                        GameSession session = new GameSession();
                                        session.setSessionCode(code);
                                        session.setHostPlayerId(currentPlayer.getId());
                                        session.setMaxPlayers(4);
                                        session.setStatus("WAITING");
                                        sessionDao.save(session);

                                        SessionPlayer sp = new SessionPlayer();
                                        sp.setSessionId(session.getId());
                                        sp.setPlayerId(currentPlayer.getId());
                                        sessionPlayerDao.save(sp);

                                        Platform.runLater(
                                                () -> {
                                                    codeLabel.setText("CODE: " + code);
                                                    statusLabel.setTextFill(Color.rgb(0, 255, 170));
                                                    statusLabel.setText(
                                                            "SHARE THIS CODE WITH FRIENDS!");
                                                });
                                    })
                            .start();
                });

        Button btnBack = createTextButton("BACK");
        btnBack.setOnAction(e -> hidePanel(createPanel));

        createPanel.getChildren().addAll(infoLabel, btnCreate, codeLabel, statusLabel, btnBack);
        createPanel.setVisible(false);
        createPanel.setOpacity(0);
        rootStack.getChildren().add(createPanel);
    }

    // ═══════════════════════════════════════════════════════
    // ПОКАЗ/ПРИХОВАННЯ ПАНЕЛЕЙ
    // ═══════════════════════════════════════════════════════

    private void showPanel(VBox panel) {
        // Ховаємо меню
        FadeTransition ftMenu = new FadeTransition(Duration.millis(300), menuBox);
        ftMenu.setToValue(0);
        ftMenu.setOnFinished(e -> menuBox.setVisible(false));
        ftMenu.play();

        // Показуємо панель
        panel.setVisible(true);
        panel.setTranslateY(30);

        FadeTransition ftPanel = new FadeTransition(Duration.millis(300), panel);
        ftPanel.setFromValue(0);
        ftPanel.setToValue(1);
        ftPanel.play();

        TranslateTransition ttPanel = new TranslateTransition(Duration.millis(300), panel);
        ttPanel.setFromY(30);
        ttPanel.setToY(0);
        ttPanel.setInterpolator(Interpolator.EASE_OUT);
        ttPanel.play();
    }

    private void hidePanel(VBox panel) {
        FadeTransition ftPanel = new FadeTransition(Duration.millis(300), panel);
        ftPanel.setToValue(0);
        ftPanel.setOnFinished(e -> panel.setVisible(false));
        ftPanel.play();

        TranslateTransition ttPanel = new TranslateTransition(Duration.millis(200), panel);
        ttPanel.setToY(30);
        ttPanel.play();

        // Показуємо меню
        menuBox.setVisible(true);
        FadeTransition ftMenu = new FadeTransition(Duration.millis(300), menuBox);
        ftMenu.setFromValue(0);
        ftMenu.setToValue(1);
        ftMenu.play();
    }

    private void showLeaderboard() {
        // Оновити дані
        leaderboardPanel.getChildren().clear();

        Label title = new Label("LEADERBOARD");
        title.setFont(loadRetroFont(28));
        title.setTextFill(Color.WHITE);
        title.setEffect(new DropShadow(8, Color.BLACK));
        leaderboardPanel.getChildren().add(title);

        var leaderboardDao = new LeaderboardEntryDao(pool);
        List<LeaderboardEntry> top = leaderboardDao.findTop10();

        VBox listBox = new VBox(6);
        listBox.setAlignment(Pos.CENTER);

        if (top.isEmpty()) {
            Label empty = new Label("NO DATA YET");
            empty.setFont(loadRetroFont(12));
            empty.setTextFill(Color.gray(0.5));
            listBox.getChildren().add(empty);
        } else {
            int rank = 1;
            for (LeaderboardEntry entry : top) {
                HBox row = new HBox(30);
                row.setAlignment(Pos.CENTER);

                Label rankL = new Label("#" + rank);
                rankL.setFont(loadRetroFont(14));
                rankL.setTextFill(rank <= 3 ? Color.rgb(255, 204, 0) : Color.WHITE);
                rankL.setMinWidth(50);

                Label scoreL = new Label("SCORE: " + entry.getScore());
                scoreL.setFont(loadRetroFont(11));
                scoreL.setTextFill(Color.rgb(0, 255, 170));
                scoreL.setMinWidth(180);

                Label lvlL = new Label("LVL " + entry.getLevelCompleted());
                lvlL.setFont(loadRetroFont(11));
                lvlL.setTextFill(Color.rgb(68, 204, 255));

                row.getChildren().addAll(rankL, scoreL, lvlL);
                listBox.getChildren().add(row);
                rank++;
            }
        }

        Button btnBack = createTextButton("BACK");
        btnBack.setOnAction(e -> hidePanel(leaderboardPanel));

        leaderboardPanel.getChildren().addAll(listBox, btnBack);
        showPanel(leaderboardPanel);
    }

    private void showOptions() {
        showPanel(optionsPanel);
    }

    private void showJoinPanel() {
        showPanel(joinPanel);
    }

    private void showCreatePanel() {
        showPanel(createPanel);
    }

    // ═══════════════════════════════════════════════════════
    // ДОПОМІЖНІ МЕТОДИ
    // ═══════════════════════════════════════════════════════

    private VBox createOverlayPanel(String titleText) {
        VBox panel = new VBox(25);
        panel.setAlignment(Pos.CENTER);
        panel.setMaxSize(500, 450);
        panel.setStyle(
                "-fx-background-color: rgba(0, 0, 0, 0.8);"
                        + "-fx-background-radius: 15;"
                        + "-fx-border-color: rgba(255,255,255,0.15);"
                        + "-fx-border-radius: 15;"
                        + "-fx-border-width: 1;"
                        + "-fx-padding: 40;");
        panel.setEffect(new DropShadow(25, Color.BLACK));

        Label title = new Label(titleText);
        title.setFont(loadRetroFont(28));
        title.setTextFill(Color.WHITE);
        title.setEffect(new DropShadow(8, Color.BLACK));

        panel.getChildren().add(title);
        return panel;
    }

    private void setupVideoBackground() {
        URL videoUrl = getClass().getResource("/assets/background.mp4");
        if (videoUrl != null) {
            Media media = new Media(videoUrl.toString());
            MediaPlayer videoPlayer = new MediaPlayer(media);
            videoPlayer.setCycleCount(MediaPlayer.INDEFINITE);
            videoPlayer.setMute(true);
            MediaView mediaView = new MediaView(videoPlayer);
            mediaView.fitWidthProperty().bind(rootStack.widthProperty());
            mediaView.fitHeightProperty().bind(rootStack.heightProperty());
            mediaView.setPreserveRatio(false);
            rootStack.getChildren().add(mediaView);
            videoPlayer.play();
        } else {
            rootStack.setStyle(
                    "-fx-background-color: linear-gradient(to bottom, #1a1a2e, #0d1117);");
        }
    }

    private void setupMusic() {
        URL musicUrl = getClass().getResource("/assets/music/lofi hip.mp3");
        if (musicUrl != null) {
            Media musicMedia = new Media(musicUrl.toString());
            musicPlayer = new MediaPlayer(musicMedia);
            musicPlayer.setCycleCount(MediaPlayer.INDEFINITE);
            musicPlayer.setVolume(settings.getVolume() / 100.0);
            musicPlayer.play();
        }
    }

    private Button createTextButton(String text) {
        Button btn = new Button(text);
        btn.setFont(loadRetroFont(24));
        btn.setTextFill(Color.WHITE);
        btn.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");

        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.BLACK);
        shadow.setRadius(5);
        shadow.setSpread(0.7);
        shadow.setOffsetX(2);
        shadow.setOffsetY(2);
        btn.setEffect(shadow);

        btn.setOnMouseEntered(
                e -> {
                    btn.setTextFill(Color.rgb(200, 200, 200));
                    ScaleTransition st = new ScaleTransition(Duration.millis(150), btn);
                    st.setToX(1.1);
                    st.setToY(1.1);
                    st.play();
                });
        btn.setOnMouseExited(
                e -> {
                    btn.setTextFill(Color.WHITE);
                    ScaleTransition st = new ScaleTransition(Duration.millis(150), btn);
                    st.setToX(1.0);
                    st.setToY(1.0);
                    st.play();
                });

        return btn;
    }

    private Font loadRetroFont(double size) {
        Font f =
                Font.loadFont(
                        getClass().getResourceAsStream("/assets/fonts/PressStart2P-Regular.ttf"),
                        size);
        return f != null ? f : Font.font("Courier New", FontWeight.BOLD, size);
    }

    private void onLogout() {
        if (musicPlayer != null) {
            musicPlayer.stop();
        }
        FadeTransition ft = new FadeTransition(Duration.millis(400), rootStack);
        ft.setFromValue(1);
        ft.setToValue(0);
        ft.setOnFinished(
                e -> {
                    try {
                        FXMLLoader loader =
                                new FXMLLoader(getClass().getResource("/fxml/login_view.fxml"));
                        Parent root = loader.load();
                        rootStack.getScene().setRoot(root);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                });
        ft.play();
    }
}
