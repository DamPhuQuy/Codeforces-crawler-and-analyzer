package com.cf.analysis.dal;

import com.cf.analysis.db.DatabaseConnection;
import com.cf.analysis.model.analysis.Analysis;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DAL - Xử lý SQL với bảng "analyses".
 * Lưu và đọc JSON strings từ PostgreSQL TEXT columns.
 */
public class AnalysisDAO {

    private final DatabaseConnection db   = DatabaseConnection.getInstance();
    private final Gson               gson = new Gson();

    /**
     * Lưu kết quả phân tích AI.
     * submission_id có UNIQUE constraint → bỏ qua nếu đã tồn tại.
     */
    public void insert(Analysis a) throws SQLException {
        String sql = """
            INSERT INTO analyses
                (submission_id, data_structures, algorithms, ai_detected, ai_confidence,
                 ai_indicators, highlighted_lines, time_complexity, space_complexity,
                 difficulty_score, explanation, raw_json, analyzed_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
            ON CONFLICT (submission_id) DO UPDATE SET
                data_structures   = EXCLUDED.data_structures,
                algorithms        = EXCLUDED.algorithms,
                ai_detected       = EXCLUDED.ai_detected,
                ai_confidence     = EXCLUDED.ai_confidence,
                ai_indicators     = EXCLUDED.ai_indicators,
                highlighted_lines = EXCLUDED.highlighted_lines,
                time_complexity   = EXCLUDED.time_complexity,
                space_complexity  = EXCLUDED.space_complexity,
                difficulty_score  = EXCLUDED.difficulty_score,
                explanation       = EXCLUDED.explanation,
                raw_json          = EXCLUDED.raw_json,
                analyzed_at       = CURRENT_TIMESTAMP
            """;

        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setLong(1, a.getSubmissionId());
            ps.setString(2, gson.toJson(a.getDataStructures()));
            ps.setString(3, gson.toJson(a.getAlgorithms()));
            ps.setBoolean(4, a.isAiDetected());
            ps.setDouble(5, a.getAiConfidence());
            ps.setString(6, gson.toJson(a.getAiIndicators()));
            ps.setString(7, gson.toJson(a.getHighlightedLines()));
            ps.setString(8, a.getTimeComplexity());
            ps.setString(9, a.getSpaceComplexity());
            ps.setInt(10, a.getDifficultyScore());
            ps.setString(11, a.getExplanation());
            ps.setString(12, a.getRawJson());
            ps.executeUpdate();
        }
    }

    /**
     * Lấy kết quả phân tích của một submission (theo DB id của submission).
     */
    public Analysis findBySubmissionId(long submissionId) throws SQLException {
        String sql = "SELECT * FROM analyses WHERE submission_id = ? LIMIT 1";

        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setLong(1, submissionId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        }
        return null;
    }

    /**
     * Lấy tất cả analyses của một user (qua JOIN với submissions).
     */
    public List<Analysis> findByHandle(String handle) throws SQLException {
        String sql = """
            SELECT a.* FROM analyses a
            JOIN submissions s ON s.id = a.submission_id
            WHERE s.user_handle = ?
            ORDER BY a.analyzed_at DESC
            """;

        List<Analysis> list = new ArrayList<>();
        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, handle);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    /**
     * Convert ResultSet → Analysis object.
     * Parse lại các JSON string thành List/Object.
     */
    private Analysis mapRow(ResultSet rs) throws SQLException {
        Analysis a = new Analysis();
        a.setId(rs.getLong("id"));
        a.setSubmissionId(rs.getLong("submission_id"));

        // Parse JSON arrays
        Type listType = new TypeToken<List<String>>(){}.getType();
        a.setDataStructures(gson.fromJson(rs.getString("data_structures"), listType));
        a.setAlgorithms(gson.fromJson(rs.getString("algorithms"), listType));

        a.setAiDetected(rs.getBoolean("ai_detected"));
        a.setAiConfidence(rs.getDouble("ai_confidence"));

        // Parse JSON objects
        a.setAiIndicators(gson.fromJson(rs.getString("ai_indicators"), Analysis.AiIndicators.class));

        Type hlType = new TypeToken<List<Analysis.HighlightedLine>>(){}.getType();
        a.setHighlightedLines(gson.fromJson(rs.getString("highlighted_lines"), hlType));

        a.setTimeComplexity(rs.getString("time_complexity"));
        a.setSpaceComplexity(rs.getString("space_complexity"));
        a.setDifficultyScore(rs.getInt("difficulty_score"));
        a.setExplanation(rs.getString("explanation"));
        a.setRawJson(rs.getString("raw_json"));

        Timestamp analyzedTs = rs.getTimestamp("analyzed_at");
        if (analyzedTs != null) a.setAnalyzedAt(analyzedTs.toLocalDateTime());

        return a;
    }
}
