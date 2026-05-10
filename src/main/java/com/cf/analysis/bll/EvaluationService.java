package com.cf.analysis.bll;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.cf.analysis.dal.AnalysisDAO;
import com.cf.analysis.dal.SubmissionDAO;
import com.cf.analysis.dal.UserDAO;
import com.cf.analysis.dal.UserScoreDAO;
import com.cf.analysis.model.analysis.Analysis;
import com.cf.analysis.model.user.Level;
import com.cf.analysis.model.user.User;
import com.cf.analysis.model.user.UserScore;

public class EvaluationService {

    private final UserDAO       userDAO;
    private final UserScoreDAO  userScoreDAO;
    private final SubmissionDAO submissionDAO;
    private final AnalysisDAO   analysisDAO;

    // Thuật toán nâng cao (mang lại bonus điểm)
    private static final Set<String> ADVANCED_ALGOS = Set.of(
        "Dynamic Programming", "Segment Tree", "Fenwick Tree", "Dijkstra",
        "Bellman-Ford", "Floyd-Warshall", "KMP", "Suffix Array",
        "Heavy-Light Decomposition", "Centroid Decomposition",
        "Network Flow", "Convex Hull", "FFT", "Game Theory", "SCC",
        "Topological Sort", "Cartesian Tree", "Li Chao Tree"
    );

    // Kỳ vọng: user giỏi biết ~15 CTDL và ~20 thuật toán khác nhau
    private static final int MAX_EXPECTED_DS   = 15;
    private static final int MAX_EXPECTED_ALGO = 20;

    public EvaluationService(UserDAO userDAO, UserScoreDAO userScoreDAO, SubmissionDAO submissionDAO, AnalysisDAO analysisDAO) {
        this.userDAO = userDAO;
        this.userScoreDAO = userScoreDAO;
        this.submissionDAO = submissionDAO;
        this.analysisDAO = analysisDAO;
    }

    // ==================== Evaluate Single User ====================

    /**
     * Lấy UserScore từ cache (database).
     * @return UserScore đã được tính trước đó, hoặc null nếu chưa có.
     */
    public UserScore getUserScore(String handle) throws SQLException {
        return userScoreDAO.findByHandle(handle);
    }

    /**
     * Tính lại điểm đánh giá cho một user và lưu vào database.
     * @return UserScore với đầy đủ điểm, hoặc null nếu user không tồn tại.
     */
    public UserScore evaluateUser(String handle) throws SQLException {
        User user = userDAO.findByHandle(handle);
        if (user == null) return null;

        List<Analysis> analyses = analysisDAO.findByHandle(handle);
        int totalSubs = submissionDAO.countByHandle(handle);

        UserScore score = new UserScore(handle);
        score.setRating(user.getRating());
        score.setTotalSubmissions(totalSubs);
        score.setAnalyzedSubmissions(analyses.size());

        // Nếu chưa có data phân tích → trả về điểm 0
        if (analyses.isEmpty()) {
            score.setDsScore(0);
            score.setAlgorithmScore(0);
            score.setAiScore(50);
            score.setOverallScore(0);
            score.setTopDataStructure("N/A");
            score.setTopAlgorithm("N/A");
            score.setLevel(Level.BEGINNER);
            userScoreDAO.insert(score);
            return score;
        }

        calculateAiScore(score, analyses);
        calculateDsScore(score, analyses);
        calculateAlgorithmScore(score, analyses);
        calculateOverallScore(score);

        score.computeLevel();

        // Lưu vào database
        userScoreDAO.insert(score);

        return score;
    }

    // ==================== Ranking ====================

    /**
     * Lấy danh sách điểm của tất cả users từ cache (database).
     * Nhanh hơn nhiều so với tính toán lại.
     */
    public List<UserScore> getRanking() throws SQLException {
        return userScoreDAO.findAll();
    }

    /**
     * Tính lại điểm cho tất cả users và cập nhật vào database.
     * Dùng khi cần refresh toàn bộ ranking.
     */
    public List<UserScore> recalculateAllScores() throws SQLException {
        List<User>      users  = userDAO.findAll();
        List<UserScore> scores = new ArrayList<>();

        for (User user : users) {
            try {
                UserScore s = evaluateUser(user.getHandle());
                if (s != null) scores.add(s);
            } catch (Exception e) {
                System.err.println("Lỗi đánh giá " + user.getHandle() + ": " + e.getMessage());
            }
        }

        scores.sort((a, b) -> Double.compare(b.getOverallScore(), a.getOverallScore()));
        return scores;
    }

    /**
     * Lấy danh sách handle của tất cả users trong hệ thống.
     */
    public List<String> getAllUserHandles() throws SQLException {
        List<User> users = userDAO.findAll();
        List<String> handles = new ArrayList<>();
        for (User u : users) handles.add(u.getHandle());
        return handles;
    }

    // ==================== Score Helpers ====================

    private void calculateAiScore(UserScore score, List<Analysis> analyses) {
        long aiCount = analyses.stream()
            .filter(a -> a.getAiResult() != null && a.getAiResult().getAiConfidence() > 0.5f)
            .count();
        double aiRate = (double) aiCount / analyses.size();
        score.setAiDetectedCount((int) aiCount);
        score.setAiUsageRate(aiRate);

        // AI-Free Score: nhiều AI → điểm thấp
        double avgConf = analyses.stream()
            .filter(a -> a.getAiResult() != null)
            .mapToDouble(a -> a.getAiResult().getAiConfidence())
            .average()
            .orElse(0.0);
        double aiScore = (1.0 - aiRate) * 100.0 * (1.0 - avgConf * 0.15);
        score.setAiScore(clamp(aiScore));
    }

    private void calculateDsScore(UserScore score, List<Analysis> analyses) {
        Set<String> uniqueDS = new HashSet<>();
        for (Analysis a : analyses) {
            uniqueDS.addAll(getDataStructures(a));
        }
        score.setDsScore(clamp(logScore(uniqueDS.size(), MAX_EXPECTED_DS)));
        score.setTopDataStructure(getMostCommon(analyses, true));
    }

    private void calculateAlgorithmScore(UserScore score, List<Analysis> analyses) {
        Set<String> uniqueAlgos = new HashSet<>();
        for (Analysis a : analyses) {
            uniqueAlgos.addAll(getAlgorithms(a));
        }
        double algoBase  = logScore(uniqueAlgos.size(), MAX_EXPECTED_ALGO);
        double advBonus  = calculateAdvancedBonus(analyses);
        score.setAlgorithmScore(clamp(algoBase + advBonus));
        score.setTopAlgorithm(getMostCommon(analyses, false));
    }

    private void calculateOverallScore(UserScore score) {
        double overall = score.getDsScore() * 0.30
                       + score.getAlgorithmScore() * 0.40
                       + score.getAiScore() * 0.30;
        score.setOverallScore(clamp(overall));
    }

    private List<String> getAlgorithms(Analysis a) {
        if (a.getComplexityAnalysis() == null || a.getComplexityAnalysis().getAlgorithms() == null) {
            return List.of();
        }
        return a.getComplexityAnalysis().getAlgorithms();
    }

    private List<String> getDataStructures(Analysis a) {
        if (a.getComplexityAnalysis() == null || a.getComplexityAnalysis().getDataStructures() == null) {
            return List.of();
        }
        return a.getComplexityAnalysis().getDataStructures();
    }

    /**
     * Logarithmic scaling: 0 items = 0 điểm, maxExp items ≈ 70 điểm, 2x maxExp ≈ 90 điểm.
     * Không bao giờ đạt 100 quá dễ.
     */
    private double logScore(int count, int maxExp) {
        if (count == 0) return 0.0;
        double ratio = (double) count / maxExp;
        return 100.0 * Math.log1p(ratio * Math.E) / Math.log1p(Math.E);
    }

    /**
     * Bonus điểm cho việc dùng thuật toán nâng cao.
     * Mỗi thuật toán nâng cao độc lập = +3 điểm, tối đa +20 điểm.
     */
    private double calculateAdvancedBonus(List<Analysis> analyses) {
        Set<String> foundAdv = new HashSet<>();
        for (Analysis a : analyses) {
            for (String algo : getAlgorithms(a)) {
                for (String adv : ADVANCED_ALGOS) {
                    if (algo.toLowerCase().contains(adv.toLowerCase())) {
                        foundAdv.add(adv);
                    }
                }
            }
        }
        return Math.min(20.0, foundAdv.size() * 3.0);
    }

    /**
     * Lấy CTDL hoặc thuật toán xuất hiện nhiều nhất.
     * @param isDS true = CTDL, false = thuật toán
     */
    private String getMostCommon(List<Analysis> analyses, boolean isDS) {
        Map<String, Integer> freq = new HashMap<>();
        for (Analysis a : analyses) {
            List<String> items = isDS
                ? getDataStructures(a)
                : getAlgorithms(a);
            for (String item : items) freq.merge(item, 1, Integer::sum);
        }
        return freq.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("N/A");
    }

    /** Giới hạn giá trị trong [0, 100]. */
    private double clamp(double v) {
        return Math.max(0, Math.min(100, v));
    }
}
