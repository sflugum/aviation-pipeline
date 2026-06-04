package com.pipeline.db;

import com.pipeline.config.ConfigManager;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseManager {

    private static final String BRONZE_URL = "jdbc:mysql://localhost:3306/bronze_db";
    private static final String BRONZE_USER = "root";
    private static final String BRONZE_PASSWORD = "new_root_bronze_pw";

    public Connection getBronzeConnection() throws SQLException {
        return DriverManager.getConnection(BRONZE_URL, BRONZE_USER, BRONZE_PASSWORD);
    }
}
