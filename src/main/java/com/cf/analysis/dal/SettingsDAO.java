package com.cf.analysis.dal;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import com.cf.analysis.db.Database;
import com.cf.analysis.db.DatabaseConnection;

/**
 * DAO cho bảng key-value settings.
 */
public class SettingsDAO {

    private final Database db;

    public SettingsDAO() {
        this(new DatabaseConnection());
    }

    public SettingsDAO(Database db) {
        this.db = db;
    }

    public String get(String key, String defaultValue) throws SQLException {
        ensureTable();

        String sql = "SELECT value FROM settings WHERE key = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, key);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String value = rs.getString("value");
                return value != null ? value : defaultValue;
            }
        }
        return defaultValue;
    }

    public void set(String key, String value) throws SQLException {
        ensureTable();

        String sql = """
                INSERT INTO settings (key, value)
                VALUES (?, ?)
                ON CONFLICT (key) DO UPDATE SET value = EXCLUDED.value
                """;
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, key);
            ps.setString(2, value != null ? value : "");
            ps.executeUpdate();
        }
    }

    private void ensureTable() throws SQLException {
        String sql = """
                CREATE TABLE IF NOT EXISTS settings (
                    key VARCHAR(100) PRIMARY KEY,
                    value TEXT NOT NULL DEFAULT ''
                )
                """;
        try (Connection conn = db.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }
}
