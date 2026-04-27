package com.cf.analysis.dal;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.cf.analysis.db.Database;
import com.cf.analysis.model.user.User;

public class UserDAO {

    private final Database database;

    public UserDAO(Database database) {
        this.database = database;
    }

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

        try (PreparedStatement ps = database.getConnection().prepareStatement(sql)) {
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

        try (Statement stmt = database.getConnection().createStatement();
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

        try (PreparedStatement ps = database.getConnection().prepareStatement(sql)) {
            ps.setString(1, handle);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        }
        return null;
    }

    /** Cập nhật thời gian crawl gần nhất. */
    public void updateLastCrawl(String handle, LocalDateTime time) throws SQLException {
        String sql = "UPDATE users SET last_crawl_at = ? WHERE handle = ?";

        try (PreparedStatement ps = database.getConnection().prepareStatement(sql)) {
            ps.setObject(1, time);
            ps.setString(2, handle);
            ps.executeUpdate();
        }
    }

    /** Cập nhật rating mới nhất từ Codeforces. */
    public void updateRating(String handle, int rating, int maxRating, String rank) throws SQLException {
        String sql = "UPDATE users SET rating = ?, max_rating = ?, rank = ? WHERE handle = ?";

        try (PreparedStatement ps = database.getConnection().prepareStatement(sql)) {
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

        try (PreparedStatement ps = database.getConnection().prepareStatement(sql)) {
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
        user.setRating(rs.getInt("rating"));
        user.setMaxRating(rs.getInt("max_rating"));
        user.setRank(rs.getString("rank"));
        user.setCountry(rs.getString("country"));
        user.setAvatarUrl(rs.getString("avatar_url"));

        Timestamp addedTs = rs.getTimestamp("added_date");
        if (addedTs != null) user.setAddedDate(addedTs.toLocalDateTime());

        Timestamp crawlTs = rs.getTimestamp("last_crawl_at");
        if (crawlTs != null) user.setLastCrawlAt(crawlTs.toLocalDateTime());

        return user;
    }
}
