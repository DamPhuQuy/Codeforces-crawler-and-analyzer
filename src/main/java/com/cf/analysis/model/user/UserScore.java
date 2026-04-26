package com.cf.analysis.model.user;

/**
 * Model tổng hợp điểm đánh giá năng lực của một user.
 * Được tính toán từ tất cả analyses của user đó trong EvaluationService.
 */
public class UserScore {

    private String handle = "";           // Handle Codeforces
    private String displayName = "";      // Tên hiển thị
    private int    rating = 0;           // Rating CF hiện tại

    // ===== Điểm đánh giá (0 - 100) =====
    private double dsScore = 0.0;          // Điểm CTDL: dựa trên số loại CTDL unique đã dùng
    private double algorithmScore = 0.0;   // Điểm thuật toán: số loại + độ phức tạp
    private double aiScore = 100.0;          // Điểm "sạch" (100 = không dùng AI, 0 = dùng AI toàn bộ)
    private double overallScore = 0.0;     // Điểm tổng (trung bình có trọng số)

    // ===== Thống kê submission =====
    private int    totalSubmissions = 0;    // Tổng số submission đã crawl
    private int    analyzedSubmissions = 0; // Số submission đã phân tích AI
    private int    aiDetectedCount = 0;     // Số submission bị phát hiện dùng AI
    private double aiUsageRate = 0.0;         // Tỷ lệ dùng AI: 0.0 → 1.0

    // ===== Xếp hạng & phân loại =====
    private String level = "Beginner";            // Beginner / Intermediate / Advanced / Expert
    private String topDataStructure = ""; // CTDL xuất hiện nhiều nhất
    private String topAlgorithm = "";     // Thuật toán xuất hiện nhiều nhất

    public UserScore() {}

    public UserScore(String handle) {
        this.handle = handle;
    }

    /**
     * Xác định level tự động dựa trên overall score và rating CF.
     * Gọi sau khi đã tính xong các điểm số.
     */
    public void computeLevel() {
        if (overallScore >= 75 && rating >= 1600) {
            level = "Expert";
        } else if (overallScore >= 55 && rating >= 1200) {
            level = "Advanced";
        } else if (overallScore >= 30 && rating >= 800) {
            level = "Intermediate";
        } else {
            level = "Beginner";
        }
    }

    /**
     * Trả về badge emoji hiển thị cho user.
     * Ưu tiên badge AI Heavy User nếu tỷ lệ cao.
     */
    public String getBadge() {
        if (aiUsageRate >= 0.6)       return "🤖 AI Heavy User";
        if (algorithmScore >= 80)     return "🧠 Algorithm Master";
        if (dsScore >= 80)            return "📚 DS Expert";
        if (aiUsageRate <= 0.05)      return "✅ Clean Coder";
        return "👨‍💻 Developer";
    }

    // ==================== Getters & Setters ====================

    public String getHandle()                    { return handle; }
    public void   setHandle(String handle)       { this.handle = handle; }

    public String getDisplayName()                       { return displayName; }
    public void   setDisplayName(String displayName)     { this.displayName = displayName; }

    public int  getRating()              { return rating; }
    public void setRating(int rating)    { this.rating = rating; }

    public double getDsScore()                   { return dsScore; }
    public void   setDsScore(double dsScore)     { this.dsScore = dsScore; }

    public double getAlgorithmScore()                        { return algorithmScore; }
    public void   setAlgorithmScore(double algorithmScore)   { this.algorithmScore = algorithmScore; }

    public double getAiScore()                   { return aiScore; }
    public void   setAiScore(double aiScore)     { this.aiScore = aiScore; }

    public double getOverallScore()                      { return overallScore; }
    public void   setOverallScore(double overallScore)   { this.overallScore = overallScore; }

    public int  getTotalSubmissions()                        { return totalSubmissions; }
    public void setTotalSubmissions(int totalSubmissions)    { this.totalSubmissions = totalSubmissions; }

    public int  getAnalyzedSubmissions()                         { return analyzedSubmissions; }
    public void setAnalyzedSubmissions(int analyzedSubmissions)  { this.analyzedSubmissions = analyzedSubmissions; }

    public int  getAiDetectedCount()                         { return aiDetectedCount; }
    public void setAiDetectedCount(int aiDetectedCount)      { this.aiDetectedCount = aiDetectedCount; }

    public double getAiUsageRate()                       { return aiUsageRate; }
    public void   setAiUsageRate(double aiUsageRate)     { this.aiUsageRate = aiUsageRate; }

    public String getLevel()             { return level; }
    public void   setLevel(String level) { this.level = level; }

    public String getTopDataStructure()                          { return topDataStructure; }
    public void   setTopDataStructure(String topDataStructure)   { this.topDataStructure = topDataStructure; }

    public String getTopAlgorithm()                      { return topAlgorithm; }
    public void   setTopAlgorithm(String topAlgorithm)   { this.topAlgorithm = topAlgorithm; }
}
