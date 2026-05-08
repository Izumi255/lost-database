package com.lost.database.infrastructure;

import java.io.*;
import java.util.Properties;

/**
 * Інфраструктурний компонент для збереження/завантаження налаштувань користувача.
 *
 * <p>Зберігає у файл settings.properties поруч з БД (./data/).
 */
public class SettingsManager {

    private static final String SETTINGS_FILE = "./data/settings.properties";
    private final Properties props;

    public SettingsManager() {
        props = new Properties();
        load();
    }

    /** Завантажити налаштування з файлу. */
    public void load() {
        File file = new File(SETTINGS_FILE);
        if (file.exists()) {
            try (InputStream is = new FileInputStream(file)) {
                props.load(is);
                System.out.println("[Settings] Loaded from: " + file.getAbsolutePath());
            } catch (IOException e) {
                System.err.println("[Settings] Error loading: " + e.getMessage());
            }
        } else {
            // Значення за замовчуванням
            props.setProperty("volume", "50");
            props.setProperty("resolution", "FULLSCREEN");
            props.setProperty("fullscreen", "true");
            save();
        }
    }

    /** Зберегти налаштування у файл. */
    public void save() {
        File file = new File(SETTINGS_FILE);
        file.getParentFile().mkdirs();
        try (OutputStream os = new FileOutputStream(file)) {
            props.store(os, "LOST Game — User Settings");
            System.out.println("[Settings] Saved to: " + file.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("[Settings] Error saving: " + e.getMessage());
        }
    }

    public int getVolume() {
        return Integer.parseInt(props.getProperty("volume", "50"));
    }

    public void setVolume(int volume) {
        props.setProperty("volume", String.valueOf(volume));
    }

    public String getResolution() {
        return props.getProperty("resolution", "FULLSCREEN");
    }

    public void setResolution(String resolution) {
        props.setProperty("resolution", resolution);
    }

    public boolean isFullscreen() {
        return Boolean.parseBoolean(props.getProperty("fullscreen", "true"));
    }

    public void setFullscreen(boolean fullscreen) {
        props.setProperty("fullscreen", String.valueOf(fullscreen));
    }
}
