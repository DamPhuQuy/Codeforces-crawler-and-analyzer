package com.cf.analysis.model.submission;

import java.time.LocalDateTime;

public class Submission {

    private Integer id;
    private String userHandle = "";
    private String language = "";
    private Integer contestId = 0;
    private Integer creationTimeSeconds = 0;
    private Integer relativeTimeSeconds = 0;
    private Integer problemId = 0;
    private String programmingLanguage = "";
    private Verdict verdict = Verdict.TESTING;
    private TestSet testSet = TestSet.SAMPLES;
    private Integer passedTestCount = 0;
    private Integer timeConsumedMillis = 0;
    private Integer memoryConsumedBytes = 0;
    private Float points = 0.0f;

    private String sourceCode = "";
    private String problemName = "";

    private LocalDateTime submittedAt;
    private LocalDateTime crawledAt;
    private boolean analyzed = false;

    public Submission() {}

    public Submission(Integer id) {
        this.id = id;
    }

    public Submission(Integer id, Integer contestId, Integer creationTimeSeconds, Integer relativeTimeSeconds, Integer problemId, String programmingLanguage, Verdict verdict, TestSet testSet, Integer passedTestCount, Integer timeConsumedMillis, Integer memoryConsumedBytes, Float points) {
        this.id = id != null ? id : null;
        this.contestId = contestId;
        this.creationTimeSeconds = creationTimeSeconds;
        this.relativeTimeSeconds = relativeTimeSeconds;
        this.problemId = problemId;
        this.programmingLanguage = programmingLanguage;
        this.verdict = verdict;
        this.testSet = testSet;
        this.passedTestCount = passedTestCount;
        this.timeConsumedMillis = timeConsumedMillis;
        this.memoryConsumedBytes = memoryConsumedBytes;
        this.points = points;
    }


    public Integer getId() {
        return id;
    }

    public String getUserHandle() {
        return userHandle;
    }

    public void setUserHandle(String userHandle) {
        this.userHandle = userHandle;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public Integer getContestId() {
        return contestId;
    }

    public void setContestId(Integer contestId) {
        this.contestId = contestId;
    }

    public Integer getCreationTimeSeconds() {
        return creationTimeSeconds;
    }

    public void setCreationTimeSeconds(Integer creationTimeSeconds) {
        this.creationTimeSeconds = creationTimeSeconds;
    }

    public Integer getRelativeTimeSeconds() {
        return relativeTimeSeconds;
    }

    public void setRelativeTimeSeconds(Integer relativeTimeSeconds) {
        this.relativeTimeSeconds = relativeTimeSeconds;
    }

    public Integer getProblemId() {
        return problemId;
    }

    public void setProblemId(Integer problemId) {
        this.problemId = problemId;
    }

    public String getProgrammingLanguage() {
        return programmingLanguage;
    }

    public void setProgrammingLanguage(String programmingLanguage) {
        this.programmingLanguage = programmingLanguage;
    }

    public Verdict getVerdict() {
        return verdict;
    }

    public void setVerdict(Verdict verdict) {
        this.verdict = verdict;
    }

    public TestSet getTestSet() {
        return testSet;
    }

    public void setTestSet(TestSet testSet) {
        this.testSet = testSet;
    }

    public Integer getPassedTestCount() {
        return passedTestCount;
    }

    public void setPassedTestCount(Integer passedTestCount) {
        this.passedTestCount = passedTestCount;
    }

    public Integer getTimeConsumedMillis() {
        return timeConsumedMillis;
    }

    public void setTimeConsumedMillis(Integer timeConsumedMillis) {
        this.timeConsumedMillis = timeConsumedMillis;
    }

    public Integer getMemoryConsumedBytes() {
        return memoryConsumedBytes;
    }

    public void setMemoryConsumedBytes(Integer memoryConsumedBytes) {
        this.memoryConsumedBytes = memoryConsumedBytes;
    }

    public Float getPoints() {
        return points;
    }

    public void setPoints(Float points) {
        this.points = points;
    }

    public String getSourceCode() {
        return sourceCode;
    }

    public void setSourceCode(String sourceCode) {
        this.sourceCode = sourceCode;
    }

    public String getProblemName() {
        return problemName;
    }

    public void setProblemName(String problemName) {
        this.problemName = problemName;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public LocalDateTime getCrawledAt() {
        return crawledAt;
    }

    public void setCrawledAt(LocalDateTime crawledAt) {
        this.crawledAt = crawledAt;
    }

    public boolean isAnalyzed() {
        return analyzed;
    }

    public void setAnalyzed(boolean analyzed) {
        this.analyzed = analyzed;
    }

    public long getSubmissionId() {
        return id != null ? id.longValue() : 0L;
    }

    public int getTimeMs() {
        return timeConsumedMillis != null ? timeConsumedMillis : 0;
    }

    public int getMemoryKb() {
        int bytes = memoryConsumedBytes != null ? memoryConsumedBytes : 0;
        return bytes / 1024;
    }

    // API getters/setters
    public String getShortLanguage() {
        String lang = language != null && !language.isEmpty() ? language : programmingLanguage;
        if (lang == null) return "?";
        if (lang.contains("Java")) return "Java";
        if (lang.contains("Python")) return "Python";
        if (lang.contains("C++") || lang.contains("G++") || lang.contains("Clang")) return "C++";
        if (lang.contains("C#")) return "C#";
        if (lang.contains("Kotlin")) return "Kotlin";
        if (lang.contains("Rust")) return "Rust";
        if (lang.contains("Go")) return "Go";
        if (lang.contains("Pascal")) return "Pascal";
        return lang.length() > 10 ? lang.substring(0, 10) + "…" : lang;
    }
}
