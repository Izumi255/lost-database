package com.lost.database.service;

import com.lost.database.dao.PlayerDao;
import com.lost.database.entity.Player;
import com.lost.database.infrastructure.EmailService;
import com.lost.database.infrastructure.PasswordHasher;
import com.lost.database.pool.ConnectionPool;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Сервіс авторизації та реєстрації.
 *
 * <p>Делегує хешування паролів до {@link PasswordHasher} (інфраструктура) та перевірку email до
 * {@link EmailService} (інфраструктура).
 */
public class AuthService {

    private final PlayerDao playerDao;
    private final PasswordHasher passwordHasher;
    private final EmailService emailService;

    public AuthService(ConnectionPool pool) {
        this.playerDao = new PlayerDao(pool);
        this.passwordHasher = new PasswordHasher();
        this.emailService = EmailService.fromProperties();
    }

    public AuthService(ConnectionPool pool, EmailService emailService) {
        this.playerDao = new PlayerDao(pool);
        this.passwordHasher = new PasswordHasher();
        this.emailService = emailService;
    }

    /**
     * Реєстрація нового гравця (без email-верифікації).
     *
     * @return збережений гравець, або empty якщо username або email вже зайняті
     */
    public Optional<Player> register(String username, String password, String email) {
        if (playerDao.findByUsername(username).isPresent()) {
            return Optional.empty();
        }

        // Перевірка на дублікат email
        if (email != null && !email.isEmpty() && playerDao.findByEmail(email).isPresent()) {
            return Optional.empty();
        }

        String hashedPassword = passwordHasher.hash(password);
        Player player = new Player(username, hashedPassword, email);
        playerDao.save(player);

        return Optional.of(player);
    }

    /**
     * Реєстрація з підтвердженням email.
     *
     * <p>Генерує 6-значний код та надсилає його на email. Повертає код, який треба перевірити у
     * {@link #confirmRegistration}.
     *
     * @return код підтвердження, або empty якщо email невалідний або username зайнятий
     */
    public Optional<String> registerWithEmailVerification(
            String username, String password, String email) {
        if (playerDao.findByUsername(username).isPresent()) {
            return Optional.empty();
        }

        // Перевірка на дублікат email
        if (playerDao.findByEmail(email).isPresent()) {
            return Optional.empty();
        }

        if (!emailService.isValidEmail(email)) {
            return Optional.empty();
        }

        String code = emailService.generateVerificationCode();
        boolean sent = emailService.sendVerificationEmail(email, code);

        if (!sent) {
            return Optional.empty();
        }

        return Optional.of(code);
    }

    /**
     * Підтверджує реєстрацію після введення коду.
     *
     * @param expectedCode код, що був надісланий
     * @param actualCode код, введений користувачем
     * @return збережений гравець, або empty якщо коди не збігаються
     */
    public Optional<Player> confirmRegistration(
            String username,
            String password,
            String email,
            String expectedCode,
            String actualCode) {
        if (!expectedCode.equals(actualCode)) {
            return Optional.empty();
        }
        return register(username, password, email);
    }

    /**
     * Авторизація гравця.
     *
     * @return гравець, якщо логін та пароль вірні
     */
    public Optional<Player> login(String username, String password) {
        Optional<Player> playerOpt = playerDao.findByUsername(username);

        if (playerOpt.isEmpty()) {
            return Optional.empty();
        }

        Player player = playerOpt.get();

        if (passwordHasher.verify(password, player.getPasswordHash())) {
            // Оновлюємо час останнього входу
            player.setLastLogin(LocalDateTime.now());
            playerDao.update(player);
            return Optional.of(player);
        }

        return Optional.empty();
    }
}
