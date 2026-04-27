package com.cf.analysis.dal;

import java.lang.reflect.Type;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.cf.analysis.db.Database;
import com.cf.analysis.model.analysis.AiIndicators;
import com.cf.analysis.model.analysis.AiResult;
import com.cf.analysis.model.analysis.Analysis;
import com.cf.analysis.model.analysis.AnalysisOutput;
import com.cf.analysis.model.analysis.ComplexityAnalysis;
import com.cf.analysis.model.analysis.Indicator;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

/**
 * DAL - Xử lý SQL với bảng "analyses".
 * Lưu và đọc JSON strings từ PostgreSQL TEXT columns.
 */
public class AnalysisDAO {

    private static final String JSON_FIELD_DETECTED = "detected";
    private static final String JSON_FIELD_EVIDENCE = "evidence";

    private final Database db;
    private final Gson gson;

    public AnalysisDAO(Database db, Gson gson) {
        this.db = db;
        this.gson = gson;
    }

    /**
     * Lưu kết quả phân tích AI.
     * submission_id có UNIQUE constraint → bỏ qua nếu đã tồn tại.
     */
    public void insert(Analysis a) throws SQLException {
        AiResult aiResult = a.getAiResult() != null ? a.getAiResult() : new AiResult();
        AiIndicators indicators = aiResult.getAiIndicators();
        ComplexityAnalysis complexity = a.getComplexityAnalysis() != null
                ? a.getComplexityAnalysis()
                : new ComplexityAnalysis();
        AnalysisOutput output = a.getAnalysisOutput() != null ? a.getAnalysisOutput() : new AnalysisOutput();

        String sql = """
            INSERT INTO analyses
                (submission_id, ai_confidence,
                 too_clean, too_clean_evidence,
                 textbook_comments, textbook_evidence,
                 perfect_naming, naming_evidence,
                 ai_pattern, pattern_evidence,
                 too_perfect, perfect_evidence,
                 wrong_style, style_evidence,
                 data_structures, algorithms, time_complexity, space_complexity,
                 difficulty_score, explanation, raw_json, analyzed_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
            ON CONFLICT (submission_id) DO UPDATE SET
                ai_confidence     = EXCLUDED.ai_confidence,
                too_clean         = EXCLUDED.too_clean,
                too_clean_evidence= EXCLUDED.too_clean_evidence,
                textbook_comments = EXCLUDED.textbook_comments,
                textbook_evidence = EXCLUDED.textbook_evidence,
                perfect_naming    = EXCLUDED.perfect_naming,
                naming_evidence   = EXCLUDED.naming_evidence,
                ai_pattern        = EXCLUDED.ai_pattern,
                pattern_evidence  = EXCLUDED.pattern_evidence,
                too_perfect       = EXCLUDED.too_perfect,
                perfect_evidence  = EXCLUDED.perfect_evidence,
                wrong_style       = EXCLUDED.wrong_style,
                style_evidence    = EXCLUDED.style_evidence,
                data_structures   = EXCLUDED.data_structures,
                algorithms        = EXCLUDED.algorithms,
                time_complexity   = EXCLUDED.time_complexity,
                space_complexity  = EXCLUDED.space_complexity,
                difficulty_score  = EXCLUDED.difficulty_score,
                explanation       = EXCLUDED.explanation,
                raw_json          = EXCLUDED.raw_json,
                analyzed_at       = CURRENT_TIMESTAMP
            """;

        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            int idx = 1;
            ps.setLong(1, a.getSubmissionId());
            ps.setDouble(++idx, safeFloat(aiResult.getAiConfidence()));

            ps.setBoolean(++idx, indicatorDetected(indicators, "tooClean"));
            ps.setString(++idx, indicatorEvidence(indicators, "tooClean"));
            ps.setBoolean(++idx, indicatorDetected(indicators, "textbookComments"));
            ps.setString(++idx, indicatorEvidence(indicators, "textbookComments"));
            ps.setBoolean(++idx, indicatorDetected(indicators, "perfectNaming"));
            ps.setString(++idx, indicatorEvidence(indicators, "perfectNaming"));
            ps.setBoolean(++idx, indicatorDetected(indicators, "aiPattern"));
            ps.setString(++idx, indicatorEvidence(indicators, "aiPattern"));
            ps.setBoolean(++idx, indicatorDetected(indicators, "tooPerfect"));
            ps.setString(++idx, indicatorEvidence(indicators, "tooPerfect"));
            ps.setBoolean(++idx, indicatorDetected(indicators, "wrongStyle"));
            ps.setString(++idx, indicatorEvidence(indicators, "wrongStyle"));

            ps.setString(++idx, gson.toJson(nullToEmptyList(complexity.getDataStructures())));
            ps.setString(++idx, gson.toJson(nullToEmptyList(complexity.getAlgorithms())));
            ps.setString(++idx, defaultString(complexity.getTimeComplexity()));
            ps.setString(++idx, defaultString(complexity.getSpaceComplexity()));
            ps.setInt(++idx, complexity.getDifficultyScore());
            ps.setString(++idx, defaultString(output.getExplanation()));
            ps.setString(++idx, defaultString(output.getRawJson()));
            ps.executeUpdate();
        }
    }

    /**
     * Lấy kết quả phân tích của một submission (theo DB id của submission).
     */
    public Analysis findBySubmissionId(long submissionId) throws SQLException {
        String sql = """
            SELECT id, submission_id, ai_confidence,
                   too_clean, too_clean_evidence,
                   textbook_comments, textbook_evidence,
                   perfect_naming, naming_evidence,
                   ai_pattern, pattern_evidence,
                   too_perfect, perfect_evidence,
                   wrong_style, style_evidence,
                   data_structures, algorithms, time_complexity, space_complexity,
                   difficulty_score, explanation, raw_json, analyzed_at
            FROM analyses
            WHERE submission_id = ?
            LIMIT 1
            """;

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
            SELECT a.id, a.submission_id, a.ai_confidence,
                   a.too_clean, a.too_clean_evidence,
                   a.textbook_comments, a.textbook_evidence,
                   a.perfect_naming, a.naming_evidence,
                   a.ai_pattern, a.pattern_evidence,
                   a.too_perfect, a.perfect_evidence,
                   a.wrong_style, a.style_evidence,
                   a.data_structures, a.algorithms, a.time_complexity, a.space_complexity,
                   a.difficulty_score, a.explanation, a.raw_json, a.analyzed_at
            FROM analyses a
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

        AiResult aiResult = new AiResult();
        aiResult.setAiConfidence((float) rs.getDouble("ai_confidence"));
        aiResult.setAiIndicators(new AiIndicators(
                new Indicator(rs.getBoolean("too_clean"), defaultString(rs.getString("too_clean_evidence"))),
                new Indicator(rs.getBoolean("textbook_comments"), defaultString(rs.getString("textbook_evidence"))),
                new Indicator(rs.getBoolean("perfect_naming"), defaultString(rs.getString("naming_evidence"))),
                new Indicator(rs.getBoolean("ai_pattern"), defaultString(rs.getString("pattern_evidence"))),
                new Indicator(rs.getBoolean("too_perfect"), defaultString(rs.getString("perfect_evidence"))),
                new Indicator(rs.getBoolean("wrong_style"), defaultString(rs.getString("style_evidence")))
        ));
        a.setAiResult(aiResult);

        ComplexityAnalysis complexity = new ComplexityAnalysis();
        complexity.setDataStructures(parseStringList(rs.getString("data_structures")));
        complexity.setAlgorithms(parseStringList(rs.getString("algorithms")));
        complexity.setTimeComplexity(defaultString(rs.getString("time_complexity")));
        complexity.setSpaceComplexity(defaultString(rs.getString("space_complexity")));
        complexity.setDifficultyScore(rs.getInt("difficulty_score"));
        a.setComplexityAnalysis(complexity);

        AnalysisOutput output = new AnalysisOutput();
        output.setExplanation(defaultString(rs.getString("explanation")));
        output.setRawJson(defaultString(rs.getString("raw_json")));
        a.setAnalysisOutput(output);

        Timestamp analyzedTs = rs.getTimestamp("analyzed_at");
        if (analyzedTs != null) a.setAnalyzedAt(analyzedTs.toLocalDateTime());

        return a;
    }

    private float safeFloat(Float value) {
        return value != null ? value : 0.0f;
    }

    private List<String> parseStringList(String jsonText) {
        if (jsonText == null || jsonText.isBlank()) return new ArrayList<>();
        Type listType = new TypeToken<List<String>>(){}.getType();
        List<String> values = gson.fromJson(jsonText, listType);
        return values != null ? values : new ArrayList<>();
    }

    private List<String> nullToEmptyList(List<String> values) {
        return values != null ? values : Collections.emptyList();
    }

    private String defaultString(String value) {
        return value != null ? value : "";
    }

    private boolean indicatorDetected(AiIndicators indicators, String indicatorKey) {
        JsonObject indicator = getIndicatorJson(indicators, indicatorKey);
        if (indicator == null
                || !indicator.has(JSON_FIELD_DETECTED)
                || indicator.get(JSON_FIELD_DETECTED).isJsonNull()) {
            return false;
        }
        return indicator.get(JSON_FIELD_DETECTED).getAsBoolean();
    }

    private String indicatorEvidence(AiIndicators indicators, String indicatorKey) {
        JsonObject indicator = getIndicatorJson(indicators, indicatorKey);
        if (indicator == null
                || !indicator.has(JSON_FIELD_EVIDENCE)
                || indicator.get(JSON_FIELD_EVIDENCE).isJsonNull()) {
            return "";
        }
        return indicator.get(JSON_FIELD_EVIDENCE).getAsString();
    }

    private JsonObject getIndicatorJson(AiIndicators indicators, String indicatorKey) {
        if (indicators == null) return null;
        JsonElement aiIndicatorsJson = gson.toJsonTree(indicators);
        if (!aiIndicatorsJson.isJsonObject()) return null;

        JsonObject aiIndicatorsObject = aiIndicatorsJson.getAsJsonObject();
        JsonElement indicatorElement = aiIndicatorsObject.get(indicatorKey);
        if (indicatorElement == null || indicatorElement.isJsonNull() || !indicatorElement.isJsonObject()) {
            return null;
        }
        return indicatorElement.getAsJsonObject();
    }
}
