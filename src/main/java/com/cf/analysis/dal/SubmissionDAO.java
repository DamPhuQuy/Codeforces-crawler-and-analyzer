package com.cf.analysis.dal;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import com.cf.analysis.db.Database;
import com.cf.analysis.model.submission.Submission;
import com.cf.analysis.model.submission.TestSet;
import com.cf.analysis.model.submission.Verdict;

public class SubmissionDAO implements DataAccessInterface<Submission, Integer> {

    private static final String ANALYZED_COLUMN = "analyzed";

    private final Database db;

    public SubmissionDAO(Database db) {
        this.db = db;
    }

    @Override
    public void insert(Submission sub) throws SQLException {
        String sqlWithId = """
            INSERT INTO submissions
                (id, user_handle, language, contest_id, creation_time_seconds,
                 relative_time_seconds, problem_id, programming_language, verdict,
                 test_set, passed_test_count, time_consumed_millis, memory_consumed_bytes,
                 points, source_code, submitted_at, crawled_at, analyzed)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, ?)
            ON CONFLICT (id) DO NOTHING
            """;

        String sqlWithoutId = """
            INSERT INTO submissions
                (user_handle, language, contest_id, creation_time_seconds,
                 relative_time_seconds, problem_id, programming_language, verdict,
                 test_set, passed_test_count, time_consumed_millis, memory_consumed_bytes,
                 points, source_code, submitted_at, crawled_at, analyzed)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, ?)
            ON CONFLICT DO NOTHING
            """;

        Integer submissionId = sub.getId();
        String sql = submissionId != null ? sqlWithId : sqlWithoutId;

        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            int idx = 1;
            if (submissionId != null) {
                ps.setInt(idx++, submissionId);
            }

            String language = sub.getLanguage() != null ? sub.getLanguage() : "";
            String programmingLanguage = sub.getProgrammingLanguage() != null
                    ? sub.getProgrammingLanguage()
                    : language;

            ps.setString(idx++, sub.getUserHandle());
            ps.setString(idx++, language);
            ps.setInt(idx++, safeInt(sub.getContestId()));
            ps.setInt(idx++, safeInt(sub.getCreationTimeSeconds()));
            ps.setInt(idx++, safeInt(sub.getRelativeTimeSeconds()));

            // Handle problemId: use 0 (dummy problem) if null or 0
            Integer problemId = sub.getProblemId();
            ps.setInt(idx++, (problemId == null || problemId == 0) ? 0 : problemId);

            ps.setString(idx++, programmingLanguage);
            ps.setString(idx++, sub.getVerdict() != null ? sub.getVerdict().name() : Verdict.TESTING.name());
            ps.setString(idx++, sub.getTestSet() != null ? sub.getTestSet().name() : TestSet.SAMPLES.name());
            ps.setInt(idx++, safeInt(sub.getPassedTestCount()));
            ps.setInt(idx++, safeInt(sub.getTimeConsumedMillis()));
            ps.setInt(idx++, safeInt(sub.getMemoryConsumedBytes()));
            ps.setFloat(idx++, sub.getPoints() != null ? sub.getPoints() : 0.0f);
            ps.setString(idx++, sub.getSourceCode());
            ps.setObject(idx++, sub.getSubmittedAt());
            ps.setBoolean(idx, sub.isAnalyzed());
            ps.executeUpdate();
        }
    }

    public List<Submission> findByHandle(String handle) throws SQLException {
        String sql = """
            SELECT s.id, s.user_handle, s.language, s.contest_id,
                   s.creation_time_seconds, s.relative_time_seconds,
                   s.problem_id, s.programming_language, s.verdict,
                   s.test_set, s.passed_test_count, s.time_consumed_millis,
                   s.memory_consumed_bytes, s.points, s.source_code,
                   s.submitted_at, s.crawled_at,
                   (a.id IS NOT NULL OR s.analyzed) AS " + ANALYZED_COLUMN + "
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
                sub.setAnalyzed(rs.getBoolean(ANALYZED_COLUMN));
                list.add(sub);
            }
        }
        return list;
    }

    @Override
    public List<Submission> findAll() throws SQLException {
        String sql = """
            SELECT s.id, s.user_handle, s.language, s.contest_id,
                   s.creation_time_seconds, s.relative_time_seconds,
                   s.problem_id, s.programming_language, s.verdict,
                   s.test_set, s.passed_test_count, s.time_consumed_millis,
                   s.memory_consumed_bytes, s.points, s.source_code,
                   s.submitted_at, s.crawled_at,
                   (a.id IS NOT NULL OR s.analyzed) AS " + ANALYZED_COLUMN + "
            FROM submissions s
            LEFT JOIN analyses a ON a.submission_id = s.id
            ORDER BY s.submitted_at DESC
            """;
        List<Submission> list = new ArrayList<>();
        try (Statement stmt = db.getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Submission sub = mapRow(rs);
                sub.setAnalyzed(rs.getBoolean(ANALYZED_COLUMN));
                list.add(sub);
            }
        }
        return list;
    }

    @Override
    public Submission findById(Integer id) throws SQLException {
        String sql = """
            SELECT id, user_handle, language, contest_id, creation_time_seconds,
                   relative_time_seconds, problem_id, programming_language, verdict,
                   test_set, passed_test_count, time_consumed_millis, memory_consumed_bytes,
                   points, source_code, submitted_at, crawled_at, " + ANALYZED_COLUMN
            FROM submissions
            WHERE id = ?
            """;

        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        }
        return null;
    }

    @Override
    public void delete(Integer id) throws SQLException {
        String sql = "DELETE FROM submissions WHERE id = ?";

        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    public long getMaxSubmissionId(String handle) throws SQLException {
        String sql = "SELECT COALESCE(MAX(id), 0) FROM submissions WHERE user_handle = ?";

        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, handle);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getLong(1);
        }
        return 0L;
    }

    public List<Submission> findUnanalyzed() throws SQLException {
        String sql = """
                        SELECT s.id, s.user_handle, s.language, s.contest_id,
                                     s.creation_time_seconds, s.relative_time_seconds,
                                     s.problem_id, s.programming_language, s.verdict,
                                     s.test_set, s.passed_test_count, s.time_consumed_millis,
                                     s.memory_consumed_bytes, s.points, s.source_code,
                                     s.submitted_at, s.crawled_at, s." + ANALYZED_COLUMN
                        FROM submissions s
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

    public int countByHandle(String handle) throws SQLException {
        String sql = "SELECT COUNT(*) FROM submissions WHERE user_handle = ?";

        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, handle);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        }
        return 0;
    }

    public void updateSourceCode(long id, String sourceCode) throws SQLException {
        String sql = "UPDATE submissions SET source_code = ? WHERE id = ?";

        try (PreparedStatement ps = db.getConnection().prepareStatement(sql)) {
            ps.setString(1, sourceCode);
            ps.setLong(2, id);
            ps.executeUpdate();
        }
    }

    private Submission mapRow(ResultSet rs) throws SQLException {
        Submission sub = new Submission(rs.getInt("id"));
        sub.setUserHandle(rs.getString("user_handle"));
        sub.setContestId(rs.getInt("contest_id"));
        sub.setLanguage(rs.getString("language"));
        sub.setCreationTimeSeconds(rs.getInt("creation_time_seconds"));
        sub.setRelativeTimeSeconds(rs.getInt("relative_time_seconds"));
        sub.setProblemId(rs.getInt("problem_id"));
        sub.setProgrammingLanguage(rs.getString("programming_language"));
        sub.setVerdict(parseVerdict(rs.getString("verdict")));
        sub.setTestSet(parseTestSet(rs.getString("test_set")));
        sub.setPassedTestCount(rs.getInt("passed_test_count"));
        sub.setTimeConsumedMillis(rs.getInt("time_consumed_millis"));
        sub.setMemoryConsumedBytes(rs.getInt("memory_consumed_bytes"));
        sub.setPoints(rs.getFloat("points"));
        sub.setSourceCode(rs.getString("source_code"));
        sub.setAnalyzed(rs.getBoolean(ANALYZED_COLUMN));

        Timestamp submittedTs = rs.getTimestamp("submitted_at");
        if (submittedTs != null) sub.setSubmittedAt(submittedTs.toLocalDateTime());

        Timestamp crawledTs = rs.getTimestamp("crawled_at");
        if (crawledTs != null) sub.setCrawledAt(crawledTs.toLocalDateTime());

        return sub;
    }

    private int safeInt(Integer value) {
        return value != null ? value : 0;
    }

    private Verdict parseVerdict(String raw) {
        if (raw == null || raw.isBlank()) return Verdict.TESTING;
        try {
            return Verdict.valueOf(raw);
        } catch (IllegalArgumentException ex) {
            return Verdict.TESTING;
        }
    }

    private TestSet parseTestSet(String raw) {
        if (raw == null || raw.isBlank()) return TestSet.SAMPLES;
        try {
            return TestSet.valueOf(raw);
        } catch (IllegalArgumentException ex) {
            return TestSet.SAMPLES;
        }
    }
}
