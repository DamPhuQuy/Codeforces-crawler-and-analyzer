package com.cf.analysis.dal;

import com.cf.analysis.db.DatabaseConnection;
import com.cf.analysis.model.submission.Submission;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DAL - Xử lý SQL với bảng "submissions".
 */
public class SubmissionDAO {

    private final DatabaseConnection db = DatabaseConnection.getInstance();

    /**
     * Thêm submission mới. Bỏ qua nếu submission_id đã tồn tại.
     * "ON CONFLICT DO NOTHING" đảm bảo không bao giờ bị trùng.
     */
    public void insert(Submission sub) throws SQLException {
        String sql = """
            INSERT INTO submissions
                (user_handle, submission_id, contest_id, problem_index, problem_name,
                 language, verdict, time_ms, memory_kb, source_code, submitted_at, crawled_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
            ON CONFLICT (submission_id) DO NOTHING
            """;

        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, sub.getUserHandle());
            ps.setLong(2, sub.getSubmissionId());
            ps.setInt(3, sub.getContestId());
            ps.setString(4, sub.getProblemIndex());
            ps.setString(5, sub.getProblemName());
            ps.setString(6, sub.getLanguage());
            ps.setString(7, sub.getVerdict() != null ? sub.getVerdict().name() : "");
            ps.setInt(8, sub.getTimeMs());
            ps.setInt(9, sub.getMemoryKb());
            ps.setString(10, sub.getSourceCode());
            ps.setObject(11, sub.getSubmittedAt());
            ps.executeUpdate();
        }
    }

    /**
     * Lấy tất cả submissions của user, mới nhất trước.
     * JOIN với analyses để biết cái nào đã được phân tích.
     */
    public List<Submission> findByHandle(String handle) throws SQLException {
        String sql = """
            SELECT s.*, (a.id IS NOT NULL) AS analyzed
            FROM submissions s
            LEFT JOIN analyses a ON a.submission_id = s.id
            WHERE s.user_handle = ?
            ORDER BY s.submitted_at DESC
            """;

        List<Submission> list = new ArrayList<>();
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, handle);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Submission sub = mapRow(rs);
                sub.setAnalyzed(rs.getBoolean("analyzed"));
                list.add(sub);
            }
        }
        return list;
    }

    /**
     * Tìm submission theo DB id (not CF submission_id).
     */
    public Submission findById(long id) throws SQLException {
        String sql = "SELECT * FROM submissions WHERE id = ?";

        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        }
        return null;
    }

    /**
     * Lấy submission_id lớn nhất đã có trong DB của user.
     * Dùng để chỉ crawl những submission MỚI HƠN (tránh crawl lại).
     */
    public long getMaxSubmissionId(String handle) throws SQLException {
        String sql = "SELECT COALESCE(MAX(submission_id), 0) FROM submissions WHERE user_handle = ?";

        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, handle);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getLong(1);
        }
        return 0L;
    }

    /**
     * Lấy tất cả submissions chưa được phân tích AI.
     * Cần có source_code mới phân tích được.
     */
    public List<Submission> findUnanalyzed() throws SQLException {
        String sql = """
            SELECT s.* FROM submissions s
            LEFT JOIN analyses a ON a.submission_id = s.id
            WHERE a.id IS NULL
              AND s.source_code IS NOT NULL
              AND s.source_code <> ''
            ORDER BY s.submitted_at DESC
            """;

        List<Submission> list = new ArrayList<>();
        try (Statement stmt = db.getConnection().createStatement();
             ResultSet rs   = stmt.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    /** Đếm tổng số submissions của user. */
    public int countByHandle(String handle) throws SQLException {
        String sql = "SELECT COUNT(*) FROM submissions WHERE user_handle = ?";

        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, handle);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        }
        return 0;
    }

    /** Cập nhật source code sau khi scrape được. */
    public void updateSourceCode(long id, String sourceCode) throws SQLException {
        String sql = "UPDATE submissions SET source_code = ? WHERE id = ?";

        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, sourceCode);
            ps.setLong(2, id);
            ps.executeUpdate();
        }
    }

    private Submission mapRow(ResultSet rs) throws SQLException {
        Submission sub = new Submission();
        sub.setId(rs.getLong("id"));
        sub.setUserHandle(rs.getString("user_handle"));
        sub.setSubmissionId(rs.getLong("submission_id"));
        sub.setContestId(rs.getInt("contest_id"));
        sub.setProblemIndex(rs.getString("problem_index"));
        sub.setProblemName(rs.getString("problem_name"));
        sub.setLanguage(rs.getString("language"));
        sub.setTimeMs(rs.getInt("time_ms"));
        sub.setMemoryKb(rs.getInt("memory_kb"));
        sub.setSourceCode(rs.getString("source_code"));

        Timestamp submittedTs = rs.getTimestamp("submitted_at");
        if (submittedTs != null) sub.setSubmittedAt(submittedTs.toLocalDateTime());

        Timestamp crawledTs = rs.getTimestamp("crawled_at");
        if (crawledTs != null) sub.setCrawledAt(crawledTs.toLocalDateTime());

        return sub;
    }
}
