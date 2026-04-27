package com.cf.analysis.bll;

import java.sql.SQLException;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.cf.analysis.ai.GeminiAnalyzer;
import com.cf.analysis.dal.AnalysisDAO;
import com.cf.analysis.dal.SubmissionDAO;
import com.cf.analysis.model.analysis.Analysis;
import com.cf.analysis.model.submission.Submission;

public class AnalysisService {

    private final SubmissionDAO submissionDAO;
    private final AnalysisDAO analysisDAO;
    private final SettingsService settings;

    private GeminiAnalyzer analyzer;  // Lazy initialization
    private volatile boolean analyzing = false;

    public AnalysisService(SubmissionDAO submissionDAO, AnalysisDAO analysisDAO, SettingsService settings) {
        this.submissionDAO = submissionDAO;
        this.analysisDAO = analysisDAO;
        this.settings = settings;
    }

    /**
     * Lấy GeminiAnalyzer với API key mới nhất từ settings.
     * Lazy init: chỉ tạo khi cần lần đầu tiên.
     */
    private GeminiAnalyzer getAnalyzer() {
        String key = settings.getGeminiApiKey();
        if (analyzer == null) {
            analyzer = new GeminiAnalyzer(key);
        } else {
            analyzer.setApiKey(key); // Cập nhật key mới nhất
        }
        return analyzer;
    }

    // ==================== Single Analysis ====================

    /**
     * Phân tích một submission theo DB id.
     *
     * @param submissionDbId  ID của submission trong DB (cột id, không phải submission_id)
     * @param logCallback     Callback để log tiến trình (có thể null)
     * @return Kết quả Analysis đã lưu vào DB
     */
    public Analysis analyzeSubmission(Integer submissionDbId, Consumer<String> logCallback) throws Exception {
        Submission sub = submissionDAO.findById(submissionDbId);
        if (sub == null) {
            throw new IllegalArgumentException("Không tìm thấy submission id=" + submissionDbId);
        }

        log(logCallback, "🤖 Phân tích: " + sub.getProblemName() + " (" + sub.getShortLanguage() + ")...");

        Analysis result = getAnalyzer().analyze(sub);
        analysisDAO.insert(result);

        return result;
    }

    // ==================== Bulk Analysis ====================

    /**
     * Phân tích tất cả submissions chưa có kết quả AI.
     * Chạy trên background thread → KHÔNG block UI.
     *
     * @param logCallback      Log từng bước
     * @param progressCallback (current, total) để cập nhật progress bar
     */
    public void analyzeAllPending(Consumer<String> logCallback, BiConsumer<Integer, Integer> progressCallback) {
        if (analyzing) {
            log(logCallback, "⚠️ Đang có analysis session đang chạy!");
            return;
        }

        new Thread(() -> {
            analyzing = true;
            try {
                List<Submission> pending = submissionDAO.findUnanalyzed();
                int total = pending.size();

                if (total == 0) {
                    log(logCallback, "ℹ️ Không có submission nào cần phân tích.");
                    return;
                }

                log(logCallback, "🤖 Bắt đầu phân tích " + total + " submissions...");

                for (int i = 0; i < pending.size(); i++) {
                    if (!analyzing) break;

                    Submission sub = pending.get(i);
                    try {
                        log(logCallback, "[" + (i + 1) + "/" + total + "] " + sub.getProblemName());
                        Analysis result = getAnalyzer().analyze(sub);
                        analysisDAO.insert(result);

                        // Callbacks phải chạy lại dùng invokeLater từ caller
                        if (progressCallback != null) {
                            progressCallback.accept(i + 1, total);
                        }

                        // Delay nhỏ tránh spam Gemini API
                        Thread.sleep(1200);

                    } catch (Exception e) {
                        log(logCallback, "  ❌ Lỗi: " + e.getMessage());
                    }
                }

                log(logCallback, "✅ Phân tích hoàn tất!");
                if (progressCallback != null) {
                    progressCallback.accept(total, total);
                }

            } catch (SQLException e) {
                log(logCallback, "❌ Lỗi DB: " + e.getMessage());
            } finally {
                analyzing = false;
            }
        }, "analysis-thread").start();
    }

    // ==================== Getters ====================

    /**
     * Lấy kết quả phân tích của một submission (theo DB id của submission).
     */
    public Analysis getAnalysis(Integer submissionDbId) throws SQLException {
        return analysisDAO.findBySubmissionId(submissionDbId);
    }

    /**
     * Lấy tất cả analyses của một user.
     */
    public List<Analysis> getAnalysesByHandle(String handle) throws SQLException {
        return analysisDAO.findByHandle(handle);
    }

    /**
     * Lấy submissions của một user từ DB.
     * Wrapper của SubmissionDAO để UI không cần import DAL trực tiếp.
     */
    public List<Submission> getSubmissionsByHandle(String handle) throws SQLException {
        return submissionDAO.findByHandle(handle);
    }

    public void stopAnalysis()    { analyzing = false; }
    public boolean isAnalyzing()  { return analyzing; }

    // ==================== Util ====================

    private void log(Consumer<String> cb, String msg) {
        if (cb != null) cb.accept(msg);
    }

    private int safeSize(List<?> list) {
        return list != null ? list.size() : 0;
    }
}
