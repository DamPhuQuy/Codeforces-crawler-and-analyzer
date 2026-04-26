package com.cf.analysis.db;

import java.sql.Connection;
import java.sql.SQLException;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class DatabaseManager {

    private static final HikariDataSource dataSource;

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(requireEnv("DB_URL"));
        config.setUsername(requireEnv("DB_USERNAME"));
        config.setPassword(requireEnv("DB_PASSWORD"));

        config.setMaximumPoolSize(10); // tùy chỉnh
        config.setMinimumIdle(2);
        config.setIdleTimeout(30000);

        dataSource = new HikariDataSource(config);
    }

    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    private static String requireEnv(String key) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing env: " + key);
        }
        return value;
    }

    private DatabaseManager() {}
}
