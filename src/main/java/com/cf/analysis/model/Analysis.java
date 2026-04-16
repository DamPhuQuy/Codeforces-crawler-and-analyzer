package com.cf.analysis.model;

import java.sql.Timestamp;
import java.util.List;

/**
 * Model đại diện cho kết quả phân tích AI của một submission.
 * Tương ứng với bảng "analyses" trong PostgreSQL.
 *
 * Chứa:
 * - Danh sách CTDL và thuật toán phát hiện được
 * - 6 tiêu chí phát hiện AI (AiIndicators)
 * - Danh sách dòng code bị highlight (HighlightedLine)
 * - Độ phức tạp time/space
 * - Giải thích tổng quan
 */
public class Analysis {

    private long      id;                  // ID trong DB
    private long      submissionId;        // FK -> submissions.id

    private List<String> dataStructures;   // VD: ["Array", "HashMap", "PriorityQueue"]
    private List<String> algorithms;       // VD: ["Dijkstra", "BFS"]

    private boolean  aiDetected;           // Có phát hiện dùng AI không?
    private double   aiConfidence;         // Độ tin cậy: 0.0 → 1.0

    private AiIndicators          aiIndicators;    // 6 tiêu chí chi tiết
    private List<HighlightedLine> highlightedLines; // Các dòng code nghi ngờ

    private String   timeComplexity;       // VD: "O(n log n)"
    private String   spaceComplexity;      // VD: "O(n)"
    private int      difficultyScore;      // Điểm độ khó: 1-10
    private String   explanation;          // Giải thích bằng tiếng Việt
    private String   rawJson;              // JSON gốc từ Gemini (để debug)
    private Timestamp analyzedAt;          // Thời gian phân tích

    public Analysis() {}

    // ==================== Inner Classes ====================

    /**
     * 6 tiêu chí phát hiện AI-generated code.
     * Mỗi tiêu chí có flag detected và chuỗi evidence (bằng chứng cụ thể).
     */
    public static class AiIndicators {
        // 1. Code quá sạch sẽ, không có debug print, không có code thừa
        public boolean tooClean;
        public String  tooCleanEvidence;

        // 2. Comment giải thích theo kiểu sách giáo khoa
        public boolean textbookComments;
        public String  textbookCommentsEvidence;

        // 3. Tên biến/hàm quá chuẩn (adjacencyList thay vì adj)
        public boolean perfectNaming;
        public String  perfectNamingEvidence;

        // 4. Cấu trúc code theo pattern AI (helper functions, generic approach)
        public boolean aiPattern;
        public String  aiPatternEvidence;

        // 5. Không có lỗi vặt nào, edge cases hoàn hảo
        public boolean tooPerfect;
        public String  tooPerfectEvidence;

        // 6. Style không khớp competitive programming
        public boolean wrongStyle;
        public String  wrongStyleEvidence;

        /** Đếm số tiêu chí dương tính (detected = true). */
        public int getPositiveCount() {
            int count = 0;
            if (tooClean)          count++;
            if (textbookComments)  count++;
            if (perfectNaming)     count++;
            if (aiPattern)         count++;
            if (tooPerfect)        count++;
            if (wrongStyle)        count++;
            return count;
        }
    }

    /**
     * Thông tin một dòng code bị highlight sau phân tích AI.
     */
    public static class HighlightedLine {
        public int    line;      // Số dòng (1-based)
        public String reason;    // Lý do bằng tiếng Việt
        public String category;  // Tiêu chí: "textbook_comments", "ai_pattern", ...

        public HighlightedLine() {}
        public HighlightedLine(int line, String reason, String category) {
            this.line     = line;
            this.reason   = reason;
            this.category = category;
        }
    }

    /**
     * Trả về nhãn hiển thị cho mức độ dùng AI.
     */
    public String getAiLabel() {
        if (!aiDetected)          return "✅ Không phát hiện dùng AI";
        if (aiConfidence >= 0.8)  return "🤖 Rất khả năng dùng AI";
        if (aiConfidence >= 0.5)  return "⚠️ Nghi ngờ dùng AI";
        return "🔍 Có dấu hiệu nhỏ";
    }

    // ==================== Getters & Setters ====================

    public long      getId()                                 { return id; }
    public void      setId(long id)                         { this.id = id; }

    public long      getSubmissionId()                      { return submissionId; }
    public void      setSubmissionId(long submissionId)     { this.submissionId = submissionId; }

    public List<String> getDataStructures()                          { return dataStructures; }
    public void         setDataStructures(List<String> ds)           { this.dataStructures = ds; }

    public List<String> getAlgorithms()                              { return algorithms; }
    public void         setAlgorithms(List<String> algorithms)       { this.algorithms = algorithms; }

    public boolean  isAiDetected()                           { return aiDetected; }
    public void     setAiDetected(boolean aiDetected)        { this.aiDetected = aiDetected; }

    public double   getAiConfidence()                        { return aiConfidence; }
    public void     setAiConfidence(double aiConfidence)     { this.aiConfidence = aiConfidence; }

    public AiIndicators getAiIndicators()                            { return aiIndicators; }
    public void         setAiIndicators(AiIndicators aiIndicators)   { this.aiIndicators = aiIndicators; }

    public List<HighlightedLine> getHighlightedLines()                           { return highlightedLines; }
    public void                  setHighlightedLines(List<HighlightedLine> hl)   { this.highlightedLines = hl; }

    public String   getTimeComplexity()                          { return timeComplexity; }
    public void     setTimeComplexity(String timeComplexity)     { this.timeComplexity = timeComplexity; }

    public String   getSpaceComplexity()                         { return spaceComplexity; }
    public void     setSpaceComplexity(String spaceComplexity)   { this.spaceComplexity = spaceComplexity; }

    public int      getDifficultyScore()                         { return difficultyScore; }
    public void     setDifficultyScore(int difficultyScore)      { this.difficultyScore = difficultyScore; }

    public String   getExplanation()                     { return explanation; }
    public void     setExplanation(String explanation)   { this.explanation = explanation; }

    public String   getRawJson()                 { return rawJson; }
    public void     setRawJson(String rawJson)   { this.rawJson = rawJson; }

    public Timestamp getAnalyzedAt()                 { return analyzedAt; }
    public void      setAnalyzedAt(Timestamp t)      { this.analyzedAt = t; }
}
