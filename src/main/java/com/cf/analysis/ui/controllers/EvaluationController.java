package com.cf.analysis.ui.controllers;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.cf.analysis.bll.EvaluationService;
import com.cf.analysis.model.user.UserScore;

/**
 * Controller cho Evaluation Panel.
 * Xử lý logic điều khiển đánh giá và xếp hạng users.
 */
public class EvaluationController {

    private final EvaluationService evaluationService;

    public EvaluationController(EvaluationService evaluationService) {
        this.evaluationService = evaluationService;
    }

    /**
     * Lấy bảng xếp hạng tất cả users (async).
     */
    public CompletableFuture<List<UserScore>> getRankingAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return evaluationService.getRanking();
            } catch (Exception e) {
                throw new RuntimeException("Lỗi tải bảng xếp hạng: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Lấy điểm chi tiết của một user (async).
     */
    public CompletableFuture<UserScore> getUserScoreAsync(String handle) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return evaluationService.getUserScore(handle);
            } catch (Exception e) {
                throw new RuntimeException("Lỗi tải điểm user: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Tính lại điểm cho tất cả users (async).
     */
    public CompletableFuture<Void> recalculateAllScoresAsync() {
        return CompletableFuture.runAsync(() -> {
            try {
                evaluationService.recalculateAllScores();
            } catch (Exception e) {
                throw new RuntimeException("Lỗi tính lại điểm: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Format điểm số để hiển thị.
     */
    public String formatScore(double score) {
        if (score >= 100) {
            return String.format("%.0f", score);
        } else if (score >= 10) {
            return String.format("%.1f", score);
        } else {
            return String.format("%.2f", score);
        }
    }

    /**
     * Lấy màu sắc theo level.
     */
    public String getLevelColor(String level) {
        return switch (level.toUpperCase()) {
            case "EXPERT" -> "#C8C8C8";       // 200,200,200
            case "ADVANCED" -> "#B4B4B4";     // 180,180,180
            case "INTERMEDIATE" -> "#A0A0A0"; // 160,160,160
            default -> "#808080";             // 128,128,128 (BEGINNER)
        };
    }

    /**
     * Lấy màu sắc theo AI usage rate.
     */
    public String getAiUsageColor(double rate) {
        if (rate >= 0.5) {
            return "#C8C8C8"; // 200,200,200 - High usage
        } else if (rate >= 0.2) {
            return "#B4B4B4"; // 180,180,180 - Medium usage
        } else {
            return "#A0A0A0"; // 160,160,160 - Low usage
        }
    }
}
