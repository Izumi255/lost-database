package com.lost.database.controller;

import com.lost.database.app.LostDatabaseApp;
import com.lost.database.dao.PlayerDao;
import com.lost.database.entity.Player;
import com.lost.database.infrastructure.SettingsManager;
import com.lost.database.service.AuthService;
import com.lost.database.infrastructure.OnlineService;
import java.util.Optional;
import java.util.Map;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import javafx.scene.control.Hyperlink;

/** Контролер JavaFX для сторінки входу та реєстрації. */
public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private TextField serverIpField;
    @FXML private Label statusLabel;
    @FXML private Button loginBtn;
    @FXML private CheckBox onlineLoginCheck;
    @FXML private VBox authBox;
    @FXML private VBox ipSettingsBox;
    @FXML private Hyperlink toggleIpBtn;

    // Реєстрація
    @FXML private TextField regUsernameField;
    @FXML private PasswordField regPasswordField;
    @FXML private TextField regEmailField;
    @FXML private TextField regServerIpField;
    @FXML private Label regStatusLabel;
    @FXML private VBox regBox;
    @FXML private VBox regIpSettingsBox;
    @FXML private Hyperlink regToggleIpBtn;

    // Верифікація (inline)
    @FXML private TextField verificationCodeField;
    @FXML private Button regSubmitBtn;
    @FXML private Button confirmCodeBtn;

    private AuthService authService;
    private PlayerDao playerDao;
    private SettingsManager settings;

    // Дані для очікуючої верифікації
    private String pendingVerificationCode;
    private String pendingUsername;
    private String pendingPassword;
    private String pendingEmail;

    @FXML
    public void initialize() {
        authService = new AuthService(LostDatabaseApp.getConnectionPool());
        playerDao = new PlayerDao(LostDatabaseApp.getConnectionPool());
        settings = new SettingsManager();

        // Load saved server IP and synchronize
        String savedIp = OnlineService.getInstance().getServerAddress();
        if (serverIpField != null) {
            serverIpField.setText(savedIp);
        }
        if (regServerIpField != null) {
            regServerIpField.setText(savedIp);
        }

        if (serverIpField != null && regServerIpField != null) {
            serverIpField.textProperty().addListener((obs, oldVal, newVal) -> {
                if (!regServerIpField.getText().equals(newVal)) {
                    regServerIpField.setText(newVal);
                }
            });
            regServerIpField.textProperty().addListener((obs, oldVal, newVal) -> {
                if (!serverIpField.getText().equals(newVal)) {
                    serverIpField.setText(newVal);
                }
            });
        }

        // Auto-login if saved (Тимчасово вимкнено для тестування онлайн логіну)
        if (settings.hasAutoLogin()) {
            String user = settings.getSavedUsername();
            String pass = settings.getSavedPassword();
            usernameField.setText(user);
            passwordField.setText(pass);
            // Не робимо автоматичний перехід, щоб гравець міг вибрати онлайн чи офлайн
        }
    }

    @FXML
    public void onLogin() {
        String user = usernameField.getText().trim();
        String pass = passwordField.getText();
        boolean isOnline = onlineLoginCheck != null && onlineLoginCheck.isSelected();
        String ip = serverIpField != null ? serverIpField.getText().trim() : "26.4.16.99";

        if (user.isEmpty() || pass.isEmpty()) {
            showStatus(statusLabel, "Введіть логін та пароль!", "#ff3333");
            return;
        }

        // Set custom server address
        OnlineService.getInstance().setServerAddress(ip);
        com.lost.database.infrastructure.MultiplayerService.getInstance().setServerAddress(ip);

        showStatus(statusLabel, isOnline ? "З'єднання з сервером..." : "Перевірка...", "yellow");

        new Thread(
                        () -> {
                            if (isOnline) {
                                Optional<Map<String, String>> onlineResult = OnlineService.getInstance().loginOnline(user, pass);
                                Platform.runLater(() -> {
                                    if (onlineResult.isPresent()) {
                                        // Якщо онлайн логін успішний, знаходимо або створюємо локального гравця для сесії
                                        Optional<Player> localPlayerOpt = playerDao.findByUsername(user);
                                        Player localPlayer;
                                        if (localPlayerOpt.isPresent()) {
                                            localPlayer = localPlayerOpt.get();
                                        } else {
                                            localPlayer = new Player();
                                            localPlayer.setUsername(user);
                                            localPlayer.setPasswordHash("online_temp");
                                            localPlayer.setRole(onlineResult.get().get("role"));
                                            playerDao.save(localPlayer); // Створюємо локальну копію
                                        }
                                        settings.saveAccount(user, pass);
                                        showStatus(statusLabel, "✅ Онлайн вхід успішний!", "#00ff00");
                                        goToLobby(localPlayer);
                                    } else {
                                        showStatus(statusLabel, "❌ Помилка з'єднання або невірні дані.", "#ff3333");
                                    }
                                });
                            } else {
                                // Локальний логін
                                Optional<Player> result = authService.login(user, pass);
                                Platform.runLater(
                                        () -> {
                                            if (result.isPresent()) {
                                                settings.saveAccount(user, pass);
                                                goToLobby(result.get());
                                            } else {
                                                showStatus(
                                                        statusLabel,
                                                        "❌ Невірний логін або пароль.",
                                                        "#ff3333");
                                            }
                                        });
                            }
                        })
                .start();
    }

    private void goToLobby(Player player) {
        try {
            LobbyController lobbyController = new LobbyController();
            javafx.scene.layout.StackPane lobbyRoot = lobbyController.buildView(player);
            authBox.getScene().setRoot(lobbyRoot);
        } catch (Exception e) {
            showStatus(statusLabel, "Помилка завантаження лобі!", "#ff3333");
            e.printStackTrace();
        }
    }

    @FXML
    public void onShowRegister() {
        authBox.setVisible(false);
        authBox.setManaged(false);
        regBox.setVisible(true);
        regBox.setManaged(true);
        regStatusLabel.setText("");
        resetVerificationUI();
    }

    @FXML
    public void onCancelRegister() {
        regBox.setVisible(false);
        regBox.setManaged(false);
        authBox.setVisible(true);
        authBox.setManaged(true);
        regStatusLabel.setText("");
        resetVerificationUI();
    }

    @FXML
    public void onRegister() {
        String user = regUsernameField.getText().trim();
        String pass = regPasswordField.getText();
        String mail = regEmailField.getText().trim();
        String ip = regServerIpField != null ? regServerIpField.getText().trim() : "26.4.16.99";

        if (user.isEmpty() || pass.isEmpty()) {
            showStatus(regStatusLabel, "Логін і пароль обов'язкові!", "#ff3333");
            return;
        }

        if (pass.length() < 4) {
            showStatus(regStatusLabel, "Пароль має бути щонайменше 4 символи!", "#ff3333");
            return;
        }

        // Set custom server address
        OnlineService.getInstance().setServerAddress(ip);
        com.lost.database.infrastructure.MultiplayerService.getInstance().setServerAddress(ip);

        // Якщо email вказано → верифікація через пошту
        if (!mail.isEmpty()) {
            // Перевірка дублікатів перед надсиланням
            if (playerDao.findByUsername(user).isPresent()) {
                showStatus(regStatusLabel, "❌ Логін \"" + user + "\" вже зайнятий!", "#ff3333");
                return;
            }
            if (playerDao.findByEmail(mail).isPresent()) {
                showStatus(
                        regStatusLabel,
                        "❌ Email \"" + mail + "\" вже використовується!",
                        "#ff3333");
                return;
            }

            showStatus(regStatusLabel, "Надсилаємо код на " + mail + "...", "yellow");

            // Блокуємо поля під час надсилання
            regUsernameField.setDisable(true);
            regPasswordField.setDisable(true);
            regEmailField.setDisable(true);
            regSubmitBtn.setDisable(true);

            new Thread(
                            () -> {
                                Optional<String> codeOpt =
                                        authService.registerWithEmailVerification(user, pass, mail);
                                Platform.runLater(
                                        () -> {
                                            if (codeOpt.isPresent()) {
                                                pendingVerificationCode = codeOpt.get();
                                                pendingUsername = user;
                                                pendingPassword = pass;
                                                pendingEmail = mail;

                                                // Показуємо поле для коду
                                                showVerificationStep();
                                            } else {
                                                showStatus(
                                                        regStatusLabel,
                                                        "❌ Не вдалося надіслати або"
                                                                + " користувач існує!",
                                                        "#ff3333");
                                                regUsernameField.setDisable(false);
                                                regPasswordField.setDisable(false);
                                                regEmailField.setDisable(false);
                                                regSubmitBtn.setDisable(false);
                                            }
                                        });
                            })
                    .start();
        } else {
            // Без email → пряма реєстрація
            showStatus(regStatusLabel, "Реєстрація...", "yellow");
            new Thread(
                            () -> {
                                // Спочатку реєструємо онлайн (якщо сервер працює)
                                boolean onlineSuccess = false;
                                try {
                                    onlineSuccess = OnlineService.getInstance().registerOnline(user, pass, "").isPresent();
                                } catch (Exception e) {
                                    // Ігноруємо, якщо сервер недоступний
                                }
                                
                                final boolean finalOnlineSuccess = onlineSuccess;
                                Optional<Player> result = authService.register(user, pass, null);
                                Platform.runLater(
                                        () -> {
                                            if (result.isPresent()) {
                                                if (finalOnlineSuccess) {
                                                    showStatus(
                                                            regStatusLabel,
                                                            "✅ Акаунт створено онлайн та локально! Тепер увійдіть.",
                                                            "#00ff00");
                                                } else {
                                                    showStatus(
                                                            regStatusLabel,
                                                            "⚠️ Створено тільки ЛОКАЛЬНО (сервер офлайн або невірний IP)!",
                                                            "orange");
                                                }
                                                onCancelRegister();
                                                usernameField.setText(user);
                                                passwordField.setText("");
                                            } else {
                                                showStatus(
                                                        regStatusLabel,
                                                        "❌ Користувач з таким ім'ям вже існує!",
                                                        "#ff3333");
                                            }
                                        });
                            })
                    .start();
        }
    }

    @FXML
    public void onConfirmCode() {
        String code = verificationCodeField.getText().trim();

        if (code.isEmpty()) {
            showStatus(regStatusLabel, "Введіть код!", "#ff3333");
            return;
        }

        showStatus(regStatusLabel, "Перевіряємо код...", "yellow");

        Optional<Player> result =
                authService.confirmRegistration(
                        pendingUsername,
                        pendingPassword,
                        pendingEmail,
                        pendingVerificationCode,
                        code);

        if (result.isPresent()) {
            // Спочатку реєструємо онлайн (якщо сервер працює)
            boolean onlineSuccess = false;
            try {
                onlineSuccess = OnlineService.getInstance().registerOnline(pendingUsername, pendingPassword, pendingEmail).isPresent();
            } catch (Exception e) {
                // Ігноруємо, якщо сервер недоступний
            }

            if (onlineSuccess) {
                showStatus(regStatusLabel, "✅ Email підтверджено! Акаунт створено онлайн та локально!", "#00ff00");
            } else {
                showStatus(regStatusLabel, "⚠️ Створено тільки ЛОКАЛЬНО (сервер офлайн або невірний IP)!", "orange");
            }
            onCancelRegister();
            usernameField.setText(pendingUsername);
            passwordField.setText("");
        } else {
            showStatus(regStatusLabel, "❌ Невірний код! Спробуйте ще раз.", "#ff3333");
            verificationCodeField.setText("");
            verificationCodeField.requestFocus();
        }
    }

    /** Показує крок верифікації: поле коду + кнопка підтвердження. */
    private void showVerificationStep() {
        // Ховаємо кнопку "Створити акаунт"
        regSubmitBtn.setVisible(false);
        regSubmitBtn.setManaged(false);

        // Показуємо поле коду
        verificationCodeField.setVisible(true);
        verificationCodeField.setManaged(true);
        verificationCodeField.setText("");
        verificationCodeField.requestFocus();

        // Показуємо кнопку "Підтвердити код"
        confirmCodeBtn.setVisible(true);
        confirmCodeBtn.setManaged(true);

        showStatus(regStatusLabel, "📧 Код надіслано на пошту! Введіть код:", "#00ccff");
    }

    /** Скидає UI верифікації до початкового стану. */
    private void resetVerificationUI() {
        regUsernameField.setDisable(false);
        regPasswordField.setDisable(false);
        regEmailField.setDisable(false);

        if (regSubmitBtn != null) {
            regSubmitBtn.setDisable(false);
            regSubmitBtn.setVisible(true);
            regSubmitBtn.setManaged(true);
        }

        if (verificationCodeField != null) {
            verificationCodeField.setVisible(false);
            verificationCodeField.setManaged(false);
            verificationCodeField.setText("");
        }

        if (confirmCodeBtn != null) {
            confirmCodeBtn.setVisible(false);
            confirmCodeBtn.setManaged(false);
        }

        pendingVerificationCode = null;
    }

    private void showStatus(Label label, String text, String color) {
        label.setText(text);
        label.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 12px;");
    }

    @FXML
    public void onToggleIpSettings() {
        if (ipSettingsBox != null && toggleIpBtn != null) {
            boolean isVisible = ipSettingsBox.isVisible();
            ipSettingsBox.setVisible(!isVisible);
            ipSettingsBox.setManaged(!isVisible);
            toggleIpBtn.setText(!isVisible ? "▼ Сховати мережу" : "⚙ Налаштування мережі");
        }
    }

    @FXML
    public void onToggleRegIpSettings() {
        if (regIpSettingsBox != null && regToggleIpBtn != null) {
            boolean isVisible = regIpSettingsBox.isVisible();
            regIpSettingsBox.setVisible(!isVisible);
            regIpSettingsBox.setManaged(!isVisible);
            regToggleIpBtn.setText(!isVisible ? "▼ Сховати мережу" : "⚙ Налаштування мережі");
        }
    }
}
