package com.pipeline.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseManager {

    // TODO: Docker Secrets/file mounting with .env/application.properties, passwords, etc
    //  see OpenSkyClient for application.properties info
    private static final String BRONZE_URL = "jdbc:mysql://localhost:3306/bronze_db";
    private static final String BRONZE_USER = "root";
    private static final String BRONZE_PASSWORD = "${MYSQL_ROOT_PASSWORD}";

    public Connection getBronzeConnection() throws SQLException {
        return DriverManager.getConnection(BRONZE_URL, BRONZE_USER, BRONZE_PASSWORD);
    }
}
