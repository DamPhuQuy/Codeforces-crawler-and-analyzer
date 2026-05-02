package com.cf.analysis.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import io.github.cdimascio.dotenv.Dotenv;

public class DatabaseConnection implements Database {

    private final Dotenv dotenv = Dotenv.load();
    private final String dbUrl = dotenv.get("DB_URL");
    private final String dbUsername = dotenv.get("DB_USERNAME");
    private final String dbPassword = dotenv.get("DB_PASSWORD");

    static {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("PostgreSQL JDBC Driver not found", e);
        }
    }

    @Override
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(dbUrl, dbUsername, dbPassword);
    }
}

