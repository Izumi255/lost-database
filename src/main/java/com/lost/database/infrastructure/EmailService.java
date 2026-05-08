package com.lost.database.infrastructure;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.io.InputStream;
import java.util.Properties;
import java.util.Random;

/**
 * Інфраструктурний компонент для роботи з електронною поштою.
 *
 * <p>Використовує Gmail SMTP для надсилання листів. Для роботи потрібен App Password від Google.
 *
 * @see <a href="https://myaccount.google.com/apppasswords">Google App Passwords</a>
 */
public class EmailService {

    private final String smtpHost;
    private final int smtpPort;
    private final String senderEmail;
    private final String senderPassword;
    private final boolean enabled;

    /**
     * Створює EmailService з Gmail SMTP.
     *
     * @param senderEmail Gmail-адреса відправника
     * @param appPassword App Password від Google (16 символів)
     */
    public EmailService(String senderEmail, String appPassword) {
        this.smtpHost = "smtp.gmail.com";
        this.smtpPort = 587;
        this.senderEmail = senderEmail;
        this.senderPassword = appPassword;
        this.enabled =
                senderEmail != null
                        && !senderEmail.isEmpty()
                        && appPassword != null
                        && !appPassword.isEmpty();
    }

    /**
     * Фабричний метод — створює EmailService з файлу email.properties.
     *
     * @return налаштований EmailService
     */
    public static EmailService fromProperties() {
        try (InputStream is = EmailService.class.getResourceAsStream("/email.properties")) {
            if (is == null) {
                System.out.println("[EmailService] email.properties not found, using mock mode");
                return new EmailService(null, null);
            }
            Properties props = new Properties();
            props.load(is);
            String email = props.getProperty("mail.sender.email");
            String password = props.getProperty("mail.sender.password");
            System.out.println("[EmailService] Loaded config for: " + email);
            return new EmailService(email, password);
        } catch (Exception e) {
            System.err.println("[EmailService] Error loading properties: " + e.getMessage());
            return new EmailService(null, null);
        }
    }

    /**
     * Перевіряє формат email-адреси.
     *
     * @param email адреса для перевірки
     * @return true, якщо формат валідний
     */
    public boolean isValidEmail(String email) {
        if (email == null || email.isEmpty()) {
            return false;
        }
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }

    /**
     * Генерує випадковий 6-значний код підтвердження.
     *
     * @return код у вигляді рядка (наприклад, "482916")
     */
    public String generateVerificationCode() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }

    /**
     * Надсилає лист з кодом підтвердження на вказану email-адресу.
     *
     * @param recipientEmail адреса отримувача
     * @param verificationCode код підтвердження
     * @return true, якщо лист успішно надіслано
     */
    public boolean sendVerificationEmail(String recipientEmail, String verificationCode) {
        if (!enabled) {
            System.out.println(
                    "[EmailService] SMTP not configured. Code for "
                            + recipientEmail
                            + ": "
                            + verificationCode);
            return true; // mock — для демо без реального SMTP
        }

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", smtpHost);
        props.put("mail.smtp.port", String.valueOf(smtpPort));

        Session session =
                Session.getInstance(
                        props,
                        new Authenticator() {
                            @Override
                            protected PasswordAuthentication getPasswordAuthentication() {
                                return new PasswordAuthentication(senderEmail, senderPassword);
                            }
                        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(senderEmail, "LOST Game"));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail));
            message.setSubject("LOST — Код підтвердження");
            message.setContent(
                    "<div style='font-family: monospace; background: #0d1117; color: #00ffaa;"
                            + " padding: 30px; border-radius: 10px;'>"
                            + "<h2 style='color: white;'>🎮 LOST Game</h2>"
                            + "<p style='color: #ccc;'>Ваш код підтвердження:</p>"
                            + "<h1 style='color: #00ffaa; letter-spacing: 8px; font-size: 36px;'>"
                            + verificationCode
                            + "</h1>"
                            + "<p style='color: #666; font-size: 12px;'>"
                            + "Цей код дійсний протягом 10 хвилин.</p>"
                            + "</div>",
                    "text/html; charset=UTF-8");

            Transport.send(message);
            System.out.println("[EmailService] Verification email sent to: " + recipientEmail);
            return true;

        } catch (Exception e) {
            System.err.println("[EmailService] Failed to send email: " + e.getMessage());
            return false;
        }
    }

    /**
     * @return true, якщо SMTP налаштовано
     */
    public boolean isEnabled() {
        return enabled;
    }
}
