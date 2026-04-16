package com.cf.analysis.dal;

import com.cf.analysis.db.DatabaseConnection;

import java.sql.*;

/**
 * DAL - Đọc/ghi cài đặt từ bảng "settings".
 * Bảng settings lưu key-value đơn giản.
 */
public class SettingsDAO {

    private final DatabaseConnection db = DatabaseConnection.getInstance();

    /**
     * Lấy giá trị setting theo key.
     * @param key          Tên setting cần lấy
     * @param defaultValue Giá trị mặc định nếu key không tồn tại hoặc rỗng
     */
    public String get(String key, String defaultValue) throws SQLException {
        String sql = "SELECT value FROM settings WHERE key = ?";

        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, key);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String val = rs.getString("value");
                // Trả về defaultValue nếu value null hoặc rỗng
                return (val != null && !val.isEmpty()) ? val : defaultValue;
            }
        }
        return defaultValue;
    }

    /**
     * Lưu hoặc cập nhật giá trị setting.
     * Dùng upsert (ON CONFLICT) để an toàn.
     */
    public void set(String key, String value) throws SQLException {
        String sql = """
            INSERT INTO settings (key, value) VALUES (?, ?)
            ON CONFLICT (key) DO UPDATE SET value = EXCLUDED.value
            """;

        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, key);
            ps.setString(2, value);
            ps.executeUpdate();
        }
    }
}
