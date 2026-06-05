package com.pipeline.db;

import com.pipeline.config.ConfigManager;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseManager {

    public Connection connectToBronze() throws SQLException {
        String dbHost = ConfigManager.get("BRONZE_DB_HOST");
        String dbUser = ConfigManager.get("BRONZE_DB_USER");
        String dbPassword = ConfigManager.get("BRONZE_DB_PASSWORD");

        String dbPort = ConfigManager.get("bronze.db.port");
        String dbName = ConfigManager.get("bronze.db.name");

        String bronzeUrl = String.format("jdbc:mysql://%s:%s/%s", dbHost, dbPort, dbName);

        return DriverManager.getConnection(bronzeUrl, dbUser, dbPassword);
    }

    public Connection connectToGold() throws SQLException {
        String dbHost = ConfigManager.get("GOLD_DB_HOST");
        String dbUser = ConfigManager.get("GOLD_DB_USER");
        String dbPassword = ConfigManager.get("GOLD_DB_PASSWORD");

        String dbPort = ConfigManager.get("gold.db.port");
        String dbName = ConfigManager.get("gold.db.name");

        String goldUrl = String.format("jdbc:postgresql://%s:%s/%s", dbHost, dbPort, dbName);

        return DriverManager.getConnection(goldUrl, dbUser, dbPassword);
    }
}
