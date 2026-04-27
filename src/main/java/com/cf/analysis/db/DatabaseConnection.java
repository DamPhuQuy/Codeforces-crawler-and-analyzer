package com.cf.analysis.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection implements Database {

    private final String dbUrl = System.getenv("DB_URL");
    private final String dbUsername = System.getenv("DB_USERNAME");
    private final String dbPassword = System.getenv("DB_PASSWORD");

    @Override
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(dbUrl, dbUsername, dbPassword);
    }
}

