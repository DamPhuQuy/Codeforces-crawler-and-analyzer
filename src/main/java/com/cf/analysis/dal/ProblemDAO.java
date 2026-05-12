package com.cf.analysis.dal;

import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.cf.analysis.db.Database;
import com.cf.analysis.model.problem.Problem;

public class ProblemDAO implements DataAccessInterface<Problem, Integer> {

    private final Database database;

    public ProblemDAO(Database database) {
        this.database = database;
    }

    @Override
    public void insert(Problem problem) throws SQLException {
        String sql = """
            INSERT INTO problems (contest_id, problemset_name, index, name, type, points, rating, tags)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (contest_id, index) DO UPDATE SET
                problemset_name = EXCLUDED.problemset_name,
                name = EXCLUDED.name,
                type = EXCLUDED.type,
                points = EXCLUDED.points,
                rating = EXCLUDED.rating,
                tags = EXCLUDED.tags
            """;

        try (Connection conn = database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, problem.getContestId());
            ps.setString(2, problem.getProblemsetName());
            ps.setString(3, problem.getIndex());
            ps.setString(4, problem.getName());
            ps.setString(5, problem.getType());
            ps.setFloat(6, problem.getPoints());
            ps.setInt(7, problem.getRating());
            Array tagsArray = conn.createArrayOf("VARCHAR", problem.getTags());
            ps.setArray(8, tagsArray);
            ps.executeUpdate();
        }
    }

    @Override
    public Problem findById(Integer id) throws SQLException {
        String sql = """
            SELECT id, contest_id, problemset_name, index, name, type, points, rating, tags
            FROM problems WHERE id = ?
            """;

        try (Connection conn = database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        }
        return null;
    }

    public Problem findByContestAndIndex(Integer contestId, String index) throws SQLException {
        String sql = """
            SELECT id, contest_id, problemset_name, index, name, type, points, rating, tags
            FROM problems WHERE contest_id = ? AND index = ?
            """;

        try (Connection conn = database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, contestId);
            ps.setString(2, index);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        }
        return null;
    }

    @Override
    public List<Problem> findAll() throws SQLException {
        String sql = """
            SELECT id, contest_id, problemset_name, index, name, type, points, rating, tags
            FROM problems ORDER BY contest_id DESC, index ASC
            """;
        List<Problem> problems = new ArrayList<>();

        try (Connection conn = database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                problems.add(mapRow(rs));
            }
        }
        return problems;
    }

    public List<Problem> findByRatingRange(int minRating, int maxRating) throws SQLException {
        String sql = """
            SELECT id, contest_id, problemset_name, index, name, type, points, rating, tags
            FROM problems WHERE rating BETWEEN ? AND ?
            ORDER BY rating ASC
            """;
        List<Problem> problems = new ArrayList<>();

        try (Connection conn = database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, minRating);
            ps.setInt(2, maxRating);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                problems.add(mapRow(rs));
            }
        }
        return problems;
    }

    public List<Problem> findByTag(String tag) throws SQLException {
        String sql = """
            SELECT id, contest_id, problemset_name, index, name, type, points, rating, tags
            FROM problems WHERE ? = ANY(tags)
            ORDER BY rating DESC
            """;
        List<Problem> problems = new ArrayList<>();

        try (Connection conn = database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tag);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                problems.add(mapRow(rs));
            }
        }
        return problems;
    }

    @Override
    public void delete(Integer id) throws SQLException {
        String sql = "DELETE FROM problems WHERE id = ?";

        try (Connection conn = database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private Problem mapRow(ResultSet rs) throws SQLException {
        Problem problem = new Problem(rs.getInt("id"));
        problem.setContestId(rs.getInt("contest_id"));
        problem.setProblemsetName(rs.getString("problemset_name"));
        problem.setIndex(rs.getString("index"));
        problem.setName(rs.getString("name"));
        problem.setType(rs.getString("type"));
        problem.setPoints(rs.getFloat("points"));
        problem.setRating(rs.getInt("rating"));

        Array tagsArray = rs.getArray("tags");
        if (tagsArray != null) {
            problem.setTags((String[]) tagsArray.getArray());
        }

        return problem;
    }
}
