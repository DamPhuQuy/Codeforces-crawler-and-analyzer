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

public class UserDAO implements DataAccessInterface<User, String> {

    private final Database database;

    public UserDAO(Database database) {
        this.database = database;
    }

    @Override
    public void insert(User user) throws SQLException {
        String sql = """
            INSERT INTO users (handle, email, vk_id, open_id, first_name, last_name, country, city,
                organization, contribution, rank, rating, max_rank, max_rating, last_online_time_seconds,
                registration_time_seconds, friend_of_count, avatar_url, title_photo_url, added_date)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
            ON CONFLICT (handle) DO UPDATE SET
                email = EXCLUDED.email,
                vk_id = EXCLUDED.vk_id,
                open_id = EXCLUDED.open_id,
                first_name = EXCLUDED.first_name,
                last_name = EXCLUDED.last_name,
                country = EXCLUDED.country,
                city = EXCLUDED.city,
                organization = EXCLUDED.organization,
                contribution = EXCLUDED.contribution,
                rank = EXCLUDED.rank,
                rating = EXCLUDED.rating,
                max_rank = EXCLUDED.max_rank,
                max_rating = EXCLUDED.max_rating,
                last_online_time_seconds = EXCLUDED.last_online_time_seconds,
                registration_time_seconds = EXCLUDED.registration_time_seconds,
                friend_of_count = EXCLUDED.friend_of_count,
                avatar_url = EXCLUDED.avatar_url,
                title_photo_url = EXCLUDED.title_photo_url
            """;

        try (PreparedStatement ps = database.getConnection().prepareStatement(sql)) {
            ps.setString(1, user.getHandle());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getVkId());
            ps.setString(4, user.getOpenId());
            ps.setString(5, user.getFirstName());
            ps.setString(6, user.getLastName());
            ps.setString(7, user.getCountry());
            ps.setString(8, user.getCity());
            ps.setString(9, user.getOrganization());
            ps.setInt(10, user.getContribution());
            ps.setString(11, user.getRank());
            ps.setInt(12, user.getRating());
            ps.setString(13, user.getMaxRank());
            ps.setInt(14, user.getMaxRating());
            ps.setInt(15, user.getLastOnlineTimeSeconds());
            ps.setInt(16, user.getRegistrationTimeSeconds());
            ps.setInt(17, user.getFriendOfCount());
            ps.setString(18, user.getAvatarUrl());
            ps.setString(19, user.getTitlePhotoUrl());
            ps.executeUpdate();
        }
    }

    /**
     * Lấy tất cả users, sắp xếp theo rating giảm dần.
     */
    public List<User> findAll() throws SQLException {
        String sql = """
            SELECT handle, email, vk_id, open_id, first_name, last_name, country, city,
                   organization, contribution, rank, rating, max_rank, max_rating,
                   last_online_time_seconds, registration_time_seconds, friend_of_count,
                   avatar_url, title_photo_url, added_date, last_crawl_at
            FROM users ORDER BY rating DESC
            """;
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
    @Override
    public User findById(String handle) throws SQLException {
        return findByHandle(handle);
    }

    public User findByHandle(String handle) throws SQLException {
        String sql = """
            SELECT handle, email, vk_id, open_id, first_name, last_name, country, city,
                   organization, contribution, rank, rating, max_rank, max_rating,
                   last_online_time_seconds, registration_time_seconds, friend_of_count,
                   avatar_url, title_photo_url, added_date, last_crawl_at
            FROM users WHERE handle = ?
            """;

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
    public void updateRating(String handle, int rating, int maxRating, String rank, String maxRank) throws SQLException {
        String sql = "UPDATE users SET rating = ?, max_rating = ?, rank = ?, max_rank = ? WHERE handle = ?";

        try (PreparedStatement ps = database.getConnection().prepareStatement(sql)) {
            ps.setInt(1, rating);
            ps.setInt(2, maxRating);
            ps.setString(3, rank);
            ps.setString(4, maxRank);
            ps.setString(5, handle);
            ps.executeUpdate();
        }
    }

    /**
     * Xóa user và tất cả submissions/analyses liên quan (CASCADE).
     */
    @Override
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
        User user = new User(rs.getString("handle"));
        user.setEmail(rs.getString("email"));
        user.setVkId(rs.getString("vk_id"));
        user.setOpenId(rs.getString("open_id"));
        user.setFirstName(rs.getString("first_name"));
        user.setLastName(rs.getString("last_name"));
        user.setCountry(rs.getString("country"));
        user.setCity(rs.getString("city"));
        user.setOrganization(rs.getString("organization"));
        user.setContribution(rs.getInt("contribution"));
        user.setRank(rs.getString("rank"));
        user.setRating(rs.getInt("rating"));
        user.setMaxRank(rs.getString("max_rank"));
        user.setMaxRating(rs.getInt("max_rating"));
        user.setLastOnlineTimeSeconds(rs.getInt("last_online_time_seconds"));
        user.setRegistrationTimeSeconds(rs.getInt("registration_time_seconds"));
        user.setFriendOfCount(rs.getInt("friend_of_count"));
        user.setAvatarUrl(rs.getString("avatar_url"));
        user.setTitlePhotoUrl(rs.getString("title_photo_url"));

        Timestamp addedTs = rs.getTimestamp("added_date");
        if (addedTs != null) user.setAddedDate(addedTs.toLocalDateTime());

        Timestamp crawlTs = rs.getTimestamp("last_crawl_at");
        if (crawlTs != null) user.setLastCrawlAt(crawlTs.toLocalDateTime());

        return user;
    }
}
