package com.cf.analysis.ui.controllers;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import com.cf.analysis.bll.AnalysisService;
import com.cf.analysis.bll.UserService;
import com.cf.analysis.model.analysis.Analysis;
import com.cf.analysis.model.submission.Submission;
import com.cf.analysis.model.user.User;

/**
 * Controller cho Submission Analysis Panel.
 * Xử lý logic điều khiển phân tích submissions với AI.
 */
public class SubmissionAnalysisController {

    private final UserService userService;
    private final AnalysisService analysisService;

    public SubmissionAnalysisController(UserService userService, AnalysisService analysisService) {
        this.userService = userService;
        this.analysisService = analysisService;
    }

    /**
     * Lấy danh sách tất cả users (async).
     */
    public CompletableFuture<List<User>> getAllUsersAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return userService.getAllUsers();
            } catch (Exception e) {
                throw new RuntimeException("Lỗi tải danh sách users: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Lấy submissions của một user (async).
     */
    public CompletableFuture<List<Submission>> getSubmissionsByHandleAsync(String handle) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return analysisService.getSubmissionsByHandle(handle);
            } catch (Exception e) {
                throw new RuntimeException("Lỗi tải submissions: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Phân tích một submission với AI (async).
     *
     * @param submissionDbId ID của submission trong DB (không phải submission_id)
     * @param logCallback Callback để log tiến trình
     */
    public CompletableFuture<Analysis> analyzeSubmissionAsync(Integer submissionDbId, Consumer<String> logCallback) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return analysisService.analyzeSubmission(submissionDbId, logCallback);
            } catch (Exception e) {
                throw new RuntimeException("Lỗi phân tích submission: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Lấy kết quả phân tích của một submission (async).
     */
    public CompletableFuture<Analysis> getAnalysisAsync(Integer submissionDbId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return analysisService.getAnalysis(submissionDbId);
            } catch (Exception e) {
                throw new RuntimeException("Lỗi tải kết quả phân tích: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Lấy tất cả analyses của một user (async).
     */
    public CompletableFuture<List<Analysis>> getAnalysesByHandleAsync(String handle) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return analysisService.getAnalysesByHandle(handle);
            } catch (Exception e) {
                throw new RuntimeException("Lỗi tải analyses: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Kiểm tra xem đang phân tích hay không.
     */
    public boolean isAnalyzing() {
        return analysisService.isAnalyzing();
    }

    /**
     * Dừng phân tích đang chạy.
     */
    public void stopAnalysis() {
        analysisService.stopAnalysis();
    }

    /**
     * Tính toán thời gian ước tính cho batch analysis.
     *
     * @param count Số lượng submissions
     * @return Thời gian ước tính (giây)
     */
    public int estimateBatchTime(int count) {
        // Mỗi submission mất khoảng 3-5 giây để phân tích với AI
        return count * 4; // Average 4 seconds per submission
    }

    /**
     * Phân tích tất cả submissions pending (async).
     *
     * @param logCallback Callback để log tiến trình
     * @param progressCallback Callback để cập nhật progress (current, total)
     */
    public void analyzeAllPending(Consumer<String> logCallback, java.util.function.BiConsumer<Integer, Integer> progressCallback) {
        analysisService.analyzeAllPending(logCallback, progressCallback);
    }

    /**
     * Lấy kết quả phân tích của một submission (sync).
     */
    public Analysis getAnalysis(Integer submissionDbId) throws Exception {
        return analysisService.getAnalysis(submissionDbId);
    }
}
