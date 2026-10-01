package com.faisal.selenium.utils;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Properties;

public final class ConfigReader {
    private static final Properties PROPERTIES = new Properties();

    static {
        try (InputStream input = ConfigReader.class.getClassLoader()
                .getResourceAsStream("config.properties")) {
            if (input == null) {
                throw new IllegalStateException("config.properties file was not found");
            }
            PROPERTIES.load(input);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load config.properties", e);
        }
    }

    private ConfigReader() {
    }

    public static String get(String key) {
        String value = System.getProperty(key, PROPERTIES.getProperty(key));
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Configuration value missing or blank: " + key);
        }
        return value;
    }

    public static Duration getTimeout(String key) {
        try {
            long seconds = Long.parseLong(get(key));
            if (seconds <= 0) {
                throw new IllegalArgumentException("Timeout must be positive: " + key);
            }
            return Duration.ofSeconds(seconds);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Timeout must be an integer in seconds: " + key, e);
        }
    }
}
