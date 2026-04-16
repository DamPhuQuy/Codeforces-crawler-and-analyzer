package com.cf.analysis.model;

import java.sql.Timestamp;

/**
 * Model đại diện cho một submission trên Codeforces.
 * Tương ứng với bảng "submissions" trong PostgreSQL.
 */
public class Submission {

    private long      id;              // ID trong DB (auto-generated)
    private String    userHandle;      // Handle của user (FK)
    private long      submissionId;    // ID gốc trên Codeforces
    private int       contestId;       // ID contest
    private String    problemIndex;    // Index bài: A, B, C, ...
    private String    problemName;     // Tên bài toán
    private String    language;        // Ngôn ngữ lập trình
    private String    verdict;         // OK, WRONG_ANSWER, TIME_LIMIT_EXCEEDED, ...
    private int       timeMs;          // Thời gian chạy (ms)
    private int       memoryKb;        // Bộ nhớ sử dụng (KB)
    private String    sourceCode;      // Source code đã crawl
    private Timestamp submittedAt;     // Thời gian nộp bài trên CF
    private Timestamp crawledAt;       // Thời gian crawl về hệ thống

    // Không lưu vào DB - tính toán khi query
    private boolean analyzed;          // Đã có phân tích AI chưa?

    public Submission() {}

    /**
     * Trả về tên ngôn ngữ ngắn gọn để hiển thị trên bảng.
     * VD: "GNU G++17 7.3.0" → "C++"
     */
    public String getShortLanguage() {
        if (language == null) return "?";
        if (language.contains("Java"))                       return "Java";
        if (language.contains("Python"))                     return "Python";
        if (language.contains("C++") || language.contains("G++") || language.contains("Clang")) return "C++";
        if (language.contains("C#"))                         return "C#";
        if (language.contains("Kotlin"))                     return "Kotlin";
        if (language.contains("Rust"))                       return "Rust";
        if (language.contains("Go"))                         return "Go";
        if (language.contains("Pascal"))                     return "Pascal";
        return language.length() > 10 ? language.substring(0, 10) + "…" : language;
    }

    // ==================== Getters & Setters ====================

    public long   getId()                      { return id; }
    public void   setId(long id)               { this.id = id; }

    public String getUserHandle()                    { return userHandle; }
    public void   setUserHandle(String userHandle)   { this.userHandle = userHandle; }

    public long   getSubmissionId()                      { return submissionId; }
    public void   setSubmissionId(long submissionId)     { this.submissionId = submissionId; }

    public int    getContestId()                     { return contestId; }
    public void   setContestId(int contestId)        { this.contestId = contestId; }

    public String getProblemIndex()                      { return problemIndex; }
    public void   setProblemIndex(String problemIndex)   { this.problemIndex = problemIndex; }

    public String getProblemName()                       { return problemName; }
    public void   setProblemName(String problemName)     { this.problemName = problemName; }

    public String getLanguage()                  { return language; }
    public void   setLanguage(String language)   { this.language = language; }

    public String getVerdict()                   { return verdict; }
    public void   setVerdict(String verdict)     { this.verdict = verdict; }

    public int  getTimeMs()              { return timeMs; }
    public void setTimeMs(int timeMs)    { this.timeMs = timeMs; }

    public int  getMemoryKb()                { return memoryKb; }
    public void setMemoryKb(int memoryKb)   { this.memoryKb = memoryKb; }

    public String getSourceCode()                    { return sourceCode; }
    public void   setSourceCode(String sourceCode)   { this.sourceCode = sourceCode; }

    public Timestamp getSubmittedAt()                { return submittedAt; }
    public void      setSubmittedAt(Timestamp t)     { this.submittedAt = t; }

    public Timestamp getCrawledAt()                  { return crawledAt; }
    public void      setCrawledAt(Timestamp t)       { this.crawledAt = t; }

    public boolean isAnalyzed()                  { return analyzed; }
    public void    setAnalyzed(boolean analyzed) { this.analyzed = analyzed; }

    @Override
    public String toString() {
        return "Submission{id=" + submissionId + ", problem='" + problemName
               + "', verdict='" + verdict + "', lang='" + getShortLanguage() + "'}";
    }
}
