package com.lost.database.controller;

import com.lost.database.app.LostDatabaseApp;
import com.lost.database.dao.PlayerDao;
import com.lost.database.entity.Player;
import com.lost.database.service.AuthService;
import java.util.Optional;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

/** Контролер JavaFX для сторінки входу та реєстрації. */
public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label statusLabel;
    @FXML private Button loginBtn;
    @FXML private VBox authBox;

    // Реєстрація
    @FXML private TextField regUsernameField;
    @FXML private PasswordField regPasswordField;
    @FXML private TextField regEmailField;
    @FXML private Label regStatusLabel;
    @FXML private VBox regBox;

    // Верифікація (inline)
    @FXML private TextField verificationCodeField;
    @FXML private Button regSubmitBtn;
    @FXML private Button confirmCodeBtn;

    private AuthService authService;
    private PlayerDao playerDao;

    // Дані для очікуючої верифікації
    private String pendingVerificationCode;
    private String pendingUsername;
    private String pendingPassword;
    private String pendingEmail;

    @FXML
    public void initialize() {
        authService = new AuthService(LostDatabaseApp.getConnectionPool());
        playerDao = new PlayerDao(LostDatabaseApp.getConnectionPool());
    }

    @FXML
    public void onLogin() {
        String user = usernameField.getText().trim();
        String pass = passwordField.getText();

        if (user.isEmpty() || pass.isEmpty()) {
            showStatus(statusLabel, "Введіть логін та пароль!", "#ff3333");
            return;
        }

        showStatus(statusLabel, "Перевірка...", "yellow");

        new Thread(
                        () -> {
                            Optional<Player> result = authService.login(user, pass);
                            Platform.runLater(
                                    () -> {
                                        if (result.isPresent()) {
                                            goToLobby(result.get());
                                        } else {
                                            showStatus(
                                                    statusLabel,
                                                    "❌ Невірний логін або пароль.",
                                                    "#ff3333");
                                        }
                                    });
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

        if (user.isEmpty() || pass.isEmpty()) {
            showStatus(regStatusLabel, "Логін і пароль обов'язкові!", "#ff3333");
            return;
        }

        if (pass.length() < 4) {
            showStatus(regStatusLabel, "Пароль має бути щонайменше 4 символи!", "#ff3333");
            return;
        }

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
                                Optional<Player> result = authService.register(user, pass, null);
                                Platform.runLater(
                                        () -> {
                                            if (result.isPresent()) {
                                                showStatus(
                                                        regStatusLabel,
                                                        "✅ Акаунт створено! Тепер увійдіть.",
                                                        "#00ff00");
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
            showStatus(regStatusLabel, "✅ Email підтверджено! Акаунт створено!", "#00ff00");
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
}
