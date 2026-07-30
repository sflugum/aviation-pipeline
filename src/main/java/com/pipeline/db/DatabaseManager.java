package com.pipeline.db;

import com.pipeline.config.ConfigManager;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatabaseManager {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseManager.class);
    private static final int MAX_RETRIES = 10;
    private static final int RETRY_DELAY_MS = 2000;
    private static final long MAX_WAIT_TIME_MS = 60000; // 60 seconds max wait

    static {
        try {
            Class.forName("org.postgresql.Driver");
            logger.info("PostgreSQL driver loaded");
        } catch (ClassNotFoundException e) {
            logger.error("Failed to load PostgreSQL driver", e);
        }
    }

    public Connection connectToBronze() throws SQLException {
        return connectWithRetry(() -> {
            String dbHost = ConfigManager.get("BRONZE_DB_HOST");
            String dbUser = ConfigManager.get("BRONZE_DB_USER");
            String dbPassword = ConfigManager.get("BRONZE_DB_PASSWORD");
            String dbPort = ConfigManager.get("bronze.db.port");
            String dbName = ConfigManager.get("bronze.db.name");
            String bronzeUrl = String.format("jdbc:mysql://%s:%s/%s", dbHost, dbPort, dbName);
            return DriverManager.getConnection(bronzeUrl, dbUser, dbPassword);
        }, "Bronze (MySQL)");
    }

    public Connection connectToGold() throws SQLException {
        return connectWithRetry(() -> {
            String dbHost = ConfigManager.get("GOLD_DB_HOST");
            String dbUser = ConfigManager.get("GOLD_DB_USER");
            String dbPassword = ConfigManager.get("GOLD_DB_PASSWORD");
            String dbPort = ConfigManager.get("gold.db.port");
            String dbName = ConfigManager.get("gold.db.name");
            String goldUrl = String.format("jdbc:postgresql://%s:%s/%s", dbHost, dbPort, dbName);
            return DriverManager.getConnection(goldUrl, dbUser, dbPassword);
        }, "Gold (PostgreSQL)");
    }

    private Connection connectWithRetry(ConnectionSupplier supplier, String dbName) throws SQLException {
        SQLException lastException = null;
        long startTime = System.currentTimeMillis();
        
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            long elapsedTime = System.currentTimeMillis() - startTime;
            if (elapsedTime > MAX_WAIT_TIME_MS) {
                logger.error("Max wait time ({} ms) exceeded for {} database connection", MAX_WAIT_TIME_MS, dbName);
                throw new SQLException("Connection timeout: exceeded max wait time of " + MAX_WAIT_TIME_MS + " ms", lastException);
            }
            
            try {
                logger.info("Connecting to {} database (attempt {}/{})", dbName, attempt, MAX_RETRIES);
                return supplier.getConnection();
            } catch (SQLException e) {
                lastException = e;
                logger.warn("Failed to connect to {} database (attempt {}/{}): {}", dbName, attempt, MAX_RETRIES, e.getMessage());
                
                if (attempt < MAX_RETRIES) {
                    try {
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new SQLException("Connection retry interrupted", ie);
                    }
                }
            }
        }
        
        logger.error("Failed to connect to {} database after {} attempts", dbName, MAX_RETRIES);
        throw lastException;
    }

    @FunctionalInterface
    interface ConnectionSupplier {
        Connection getConnection() throws SQLException;
    }
}
