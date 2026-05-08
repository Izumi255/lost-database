package com.lost.database.infrastructure;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Інфраструктурний компонент для хешування паролів.
 *
 * <p>Обгортка навколо BCrypt — бізнес-логіка (сервіси) не повинна залежати від конкретної
 * бібліотеки хешування напряму.
 */
public class PasswordHasher {

    private static final int BCRYPT_ROUNDS = 12;

    /**
     * Хешує пароль за допомогою BCrypt.
     *
     * @param rawPassword пароль у відкритому вигляді
     * @return BCrypt-хеш
     */
    public String hash(String rawPassword) {
        return BCrypt.hashpw(rawPassword, BCrypt.gensalt(BCRYPT_ROUNDS));
    }

    /**
     * Перевіряє чи збігається пароль з хешем.
     *
     * @param rawPassword пароль у відкритому вигляді
     * @param hashedPassword BCrypt-хеш з бази даних
     * @return true, якщо пароль вірний
     */
    public boolean verify(String rawPassword, String hashedPassword) {
        return BCrypt.checkpw(rawPassword, hashedPassword);
    }
}
