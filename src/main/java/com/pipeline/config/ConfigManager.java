package com.pipeline.config;

import com.pipeline.Main;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigManager {

    private static final Logger logger = LoggerFactory.getLogger(ConfigManager.class);

    private static final Properties appProps = new Properties();
    private static final Properties envProps = new Properties();

    static {
        try (InputStream input = ConfigManager.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (input != null) {
                appProps.load(input);
            }
        } catch (Exception e) {
            logger.error("Warning: Could not load application.properties.");
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(".env"))) {
            envProps.load(reader);
        } catch (Exception e) {
              //TODO
        }
    }

    public static String get(String key) {
        String value = System.getenv(key);
        if (value != null && !value.trim().isEmpty()) {
            return value;
        }

        value = envProps.getProperty(key);
        if (value != null && !value.trim().isEmpty()) {
            return value;
        }

        value = appProps.getProperty(key);
        if (value != null && !value.trim().isEmpty()) {
            return value;
        }

        throw new RuntimeException("Configuration key '" + key + "' is missing. Check your .env or application.properties.");
    }
}
