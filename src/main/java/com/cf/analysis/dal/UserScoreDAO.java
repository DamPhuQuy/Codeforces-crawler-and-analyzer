package com.cf.analysis.dal;

import com.cf.analysis.db.DatabaseConnection;
import com.cf.analysis.model.user.Level;
import com.cf.analysis.model.user.UserScore;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserScoreDAO {

    private final DatabaseConnection db = DatabaseConnection.getInstance();

    public void upsert(UserScore score) throws SQLException {
        String sql = """
            INSERT INTO user_scores (
                handle, display_name, rating,
                ds_score, algorithm_score, ai_score, overall_score,
                total_submissions, analyzed_submissions, ai_detected_count, ai_usage_rate,
                level, top_data_structure, top_algorithm,
                last_evaluated_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::VARCHAR, ?, ?, NOW())
            ON CONFLICT (handle) DO UPDATE SET
                display_name = EXCLUDED.display_name,
                rating = EXCLUDED.rating,
                ds_score = EXCLUDED.ds_score,
                algorithm_score = EXCLUDED.algorithm_score,
                ai_score = EXCLUDED.ai_score,
                overall_score = EXCLUDED.overall_score,
                total_submissions = EXCLUDED.total_submissions,
                analyzed_submissions = EXCLUDED.analyzed_submissions,
                ai_detected_count = EXCLUDED.ai_detected_count,
                ai_usage_rate = EXCLUDED.ai_usage_rate,
                level = EXCLUDED.level,
                top_data_structure = EXCLUDED.top_data_structure,
                top_algorithm = EXCLUDED.top_algorithm,
                last_evaluated_at = NOW()
            """;

        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, score.getHandle());
            ps.setString(2, score.getDisplayName());
            ps.setInt(3, score.getRating());
            ps.setDouble(4, score.getDsScore());
            ps.setDouble(5, score.getAlgorithmScore());
            ps.setDouble(6, score.getAiScore());
            ps.setDouble(7, score.getOverallScore());
            ps.setInt(8, score.getTotalSubmissions());
            ps.setInt(9, score.getAnalyzedSubmissions());
            ps.setInt(10, score.getAiDetectedCount());
            ps.setDouble(11, score.getAiUsageRate());
            ps.setString(12, score.getLevel().name());
            ps.setString(13, score.getTopDataStructure());
            ps.setString(14, score.getTopAlgorithm());
            ps.executeUpdate();
        }
    }

    public UserScore findByHandle(String handle) throws SQLException {
        String sql = "SELECT * FROM user_scores WHERE handle = ?";

        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, handle);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        }
        return null;
    }

    public List<UserScore> findAll() throws SQLException {
        String sql = "SELECT * FROM user_scores ORDER BY overall_score DESC";
        List<UserScore> scores = new ArrayList<>();

        try (Statement stmt = db.getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                scores.add(mapRow(rs));
            }
        }
        return scores;
    }

    public void delete(String handle) throws SQLException {
        String sql = "DELETE FROM user_scores WHERE handle = ?";

        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, handle);
            ps.executeUpdate();
        }
    }

    private UserScore mapRow(ResultSet rs) throws SQLException {
        UserScore score = new UserScore();
        score.setHandle(rs.getString("handle"));
        score.setDisplayName(rs.getString("display_name"));
        score.setRating(rs.getInt("rating"));
        score.setDsScore(rs.getDouble("ds_score"));
        score.setAlgorithmScore(rs.getDouble("algorithm_score"));
        score.setAiScore(rs.getDouble("ai_score"));
        score.setOverallScore(rs.getDouble("overall_score"));
        score.setTotalSubmissions(rs.getInt("total_submissions"));
        score.setAnalyzedSubmissions(rs.getInt("analyzed_submissions"));
        score.setAiDetectedCount(rs.getInt("ai_detected_count"));
        score.setAiUsageRate(rs.getDouble("ai_usage_rate"));
        score.setLevel(Level.valueOf(rs.getString("level")));
        score.setTopDataStructure(rs.getString("top_data_structure"));
        score.setTopAlgorithm(rs.getString("top_algorithm"));
        return score;
    }
}
