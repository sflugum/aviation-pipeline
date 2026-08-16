package com.pipeline.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Central place for reading config values, checked in this order: OS environment
 * variables, then a local .env file, then application.properties. This lets the
 * same code run locally (via .env) and in a deployed container (via real env vars)
 * without changing how config is looked up.
 */
public class ConfigManager {

    private static final Logger logger = LoggerFactory.getLogger(ConfigManager.class);

    private static final Properties appProps = new Properties();
    private static final Properties envProps = new Properties();

    // Runs once when the class is first loaded, so both files are read a single time
    // and cached in these Properties objects rather than re-reading from disk on
    // every get() call
    static {
        try (InputStream input = ConfigManager.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (input != null) {
                appProps.load(input);
            } else {
                logger.debug("application.properties not found on classpath.");
            }
        } catch (IOException e) {
            logger.error("Error reading application.properties: {}", e.getMessage());
        }

        Path envPath = Paths.get(".env");
        if (Files.exists(envPath)) {
            try (BufferedReader reader = Files.newBufferedReader(envPath)) {
                envProps.load(reader);
                logger.info("Successfully loaded configuration from .env file.");
            } catch (IOException e) {
                logger.warn("A .env file was found but could not be read. Check permissions. Reason: {}", e.getMessage());
            }
        } else {
            logger.debug("No .env file found. Falling back to system environment variables");
        }
    }

    /**
     * Looks up a config value, checking OS env vars first, then .env, then
     * application.properties, in that order. Throws instead of returning null so a
     * missing key fails loudly at startup rather than causing a harder-to-trace
     * error later when the value is actually used.
     */
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

        throw new RuntimeException("Configuration key '" + key + "' is missing. Check your OS env variables, .env file or application.properties.");
    }

    // TODO: add validatedRequiredKeys method here, see GitHub issue #[12]
}
