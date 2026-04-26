package com.cf.analysis.dal;

import com.cf.analysis.db.DatabaseConnection;
import com.cf.analysis.model.user.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAL (Data Access Layer) - Chỉ xử lý SQL với bảng "users".
 * KHÔNG có bất kỳ logic nghiệp vụ nào ở đây.
 *
 * Mỗi method = một thao tác SQL cụ thể (CRUD).
 */
public class UserDAO {

    // Lấy instance DatabaseConnection dùng chung trong toàn app
    private final DatabaseConnection db = DatabaseConnection.getInstance();

    /**
     * Thêm user mới vào DB.
     * Nếu handle đã tồn tại → cập nhật thông tin (upsert).
     */
    public void insert(User user) throws SQLException {
        String sql = """
            INSERT INTO users (handle, display_name, rating, max_rating, rank, country, avatar_url, added_date)
            VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
            ON CONFLICT (handle) DO UPDATE SET
                display_name = EXCLUDED.display_name,
                rating       = EXCLUDED.rating,
                max_rating   = EXCLUDED.max_rating,
                rank         = EXCLUDED.rank,
                country      = EXCLUDED.country,
                avatar_url   = EXCLUDED.avatar_url
            """;

        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, user.getHandle());
            ps.setString(2, user.getDisplayName());
            ps.setInt(3, user.getRating());
            ps.setInt(4, user.getMaxRating());
            ps.setString(5, user.getRank());
            ps.setString(6, user.getCountry());
            ps.setString(7, user.getAvatarUrl());
            ps.executeUpdate();
        }
    }

    /**
     * Lấy tất cả users, sắp xếp theo rating giảm dần.
     */
    public List<User> findAll() throws SQLException {
        String sql = "SELECT * FROM users ORDER BY rating DESC";
        List<User> users = new ArrayList<>();

        try (Statement stmt = db.getConnection().createStatement();
             ResultSet rs   = stmt.executeQuery(sql)) {
            while (rs.next()) {
                users.add(mapRow(rs));
            }
        }
        return users;
    }

    /**
     * Tìm user theo handle. Trả về null nếu không tồn tại.
     */
    public User findByHandle(String handle) throws SQLException {
        String sql = "SELECT * FROM users WHERE handle = ?";

        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, handle);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        }
        return null;
    }

    /** Cập nhật thời gian crawl gần nhất. */
    public void updateLastCrawl(String handle, Timestamp time) throws SQLException {
        String sql = "UPDATE users SET last_crawl_at = ? WHERE handle = ?";

        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setTimestamp(1, time);
            ps.setString(2, handle);
            ps.executeUpdate();
        }
    }

    /** Cập nhật rating mới nhất từ Codeforces. */
    public void updateRating(String handle, int rating, int maxRating, String rank) throws SQLException {
        String sql = "UPDATE users SET rating = ?, max_rating = ?, rank = ? WHERE handle = ?";

        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setInt(1, rating);
            ps.setInt(2, maxRating);
            ps.setString(3, rank);
            ps.setString(4, handle);
            ps.executeUpdate();
        }
    }

    /**
     * Xóa user và tất cả submissions/analyses liên quan (CASCADE).
     */
    public void delete(String handle) throws SQLException {
        String sql = "DELETE FROM users WHERE handle = ?";

        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, handle);
            ps.executeUpdate();
        }
    }

    /**
     * Convert một dòng ResultSet thành User object.
     * Gọi private để chỉ dùng trong class này.
     */
    private User mapRow(ResultSet rs) throws SQLException {
        User user = new User();
        user.setHandle(rs.getString("handle"));
        user.setDisplayName(rs.getString("display_name"));
        user.setRating(rs.getInt("rating"));
        user.setMaxRating(rs.getInt("max_rating"));
        user.setRank(rs.getString("rank"));
        user.setCountry(rs.getString("country"));
        user.setAvatarUrl(rs.getString("avatar_url"));
        user.setAddedDate(rs.getTimestamp("added_date"));
        user.setLastCrawlAt(rs.getTimestamp("last_crawl_at"));
        return user;
    }
}
