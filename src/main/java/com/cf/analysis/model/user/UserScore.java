package com.cf.analysis.model.user;

import java.time.LocalDateTime;

/**
 * Model tổng hợp điểm đánh giá năng lực của một user.
 * Được tính toán từ tất cả analyses của user đó trong EvaluationService.
 */
public class UserScore {

    private String handle;
    private int rating = 0;

    private double dsScore = 0.0;
    private double algorithmScore = 0.0;
    private double aiScore = 100.0;
    private double overallScore = 0.0;

    private int totalSubmissions = 0;
    private int analyzedSubmissions = 0;
    private int aiDetectedCount = 0;
    private double aiUsageRate = 0.0;

    private Level level = Level.BEGINNER;
    private String topDataStructure = "";
    private String topAlgorithm = "";

    private LocalDateTime lastEvaluatedAt;
    private LocalDateTime createdAt;

    public UserScore() {}

    public UserScore(String handle) {
        this.handle = handle;
    }

    public void computeLevel() {
        if (overallScore >= 75 && rating >= 1600) {
            this.level = Level.EXPERT;
        } else if (overallScore >= 55 && rating >= 1200) {
            this.level = Level.ADVANCED;
        } else if (overallScore >= 30 && rating >= 800) {
            this.level = Level.INTERMEDIATE;
        } else {
            this.level = Level.BEGINNER;
        }
    }

    public String getBadge() {
        if (aiUsageRate >= 0.6)       return "AI Heavy User";
        if (algorithmScore >= 80)     return "Algorithm Master";
        if (dsScore >= 80)            return "Data Structure Expert";
        if (aiUsageRate <= 0.05)      return "Clean Coder";
        return "Developer";
    }

    public String getHandle() {
        return handle;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public double getDsScore() {
        return dsScore;
    }

    public void setDsScore(double dsScore) {
        this.dsScore = dsScore;
    }

    public double getAlgorithmScore() {
        return algorithmScore;
    }

    public void setAlgorithmScore(double algorithmScore) {
        this.algorithmScore = algorithmScore;
    }

    public double getAiScore() {
        return aiScore;
    }

    public void setAiScore(double aiScore) {
        this.aiScore = aiScore;
    }

    public double getOverallScore() {
        return overallScore;
    }

    public void setOverallScore(double overallScore) {
        this.overallScore = overallScore;
    }

    public int getTotalSubmissions() {
        return totalSubmissions;
    }

    public void setTotalSubmissions(int totalSubmissions) {
        this.totalSubmissions = totalSubmissions;
    }

    public int getAnalyzedSubmissions() {
        return analyzedSubmissions;
    }

    public void setAnalyzedSubmissions(int analyzedSubmissions) {
        this.analyzedSubmissions = analyzedSubmissions;
    }

    public int getAiDetectedCount() {
        return aiDetectedCount;
    }

    public void setAiDetectedCount(int aiDetectedCount) {
        this.aiDetectedCount = aiDetectedCount;
    }

    public double getAiUsageRate() {
        return aiUsageRate;
    }

    public void setAiUsageRate(double aiUsageRate) {
        this.aiUsageRate = aiUsageRate;
    }

    public Level getLevel() {
        return level;
    }

    public void setLevel(Level level) {
        this.level = level;
    }

    public String getTopDataStructure() {
        return topDataStructure;
    }

    public void setTopDataStructure(String topDataStructure) {
        this.topDataStructure = topDataStructure;
    }

    public String getTopAlgorithm() {
        return topAlgorithm;
    }

    public void setTopAlgorithm(String topAlgorithm) {
        this.topAlgorithm = topAlgorithm;
    }

    public LocalDateTime getLastEvaluatedAt() {
        return lastEvaluatedAt;
    }

    public void setLastEvaluatedAt(LocalDateTime lastEvaluatedAt) {
        this.lastEvaluatedAt = lastEvaluatedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
