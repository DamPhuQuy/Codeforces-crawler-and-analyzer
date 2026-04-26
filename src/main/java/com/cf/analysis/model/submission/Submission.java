package com.cf.analysis.model.submission;

import com.cf.analysis.model.party.Party;
import com.cf.analysis.model.problem.Problem;

public class Submission {

    private Integer id = 0;
    private Integer contestId = 0;
    private Integer creationTimeSeconds = 0;
    private Integer relativeTimeSeconds = 0;
    private Problem problem = new Problem();
    private Party party = new Party();
    private String programmingLanguage = "";
    private Verdict verdict = Verdict.TESTING;
    private TestSet testSet = TestSet.SAMPLES;
    private Integer passedTestCount = 0;
    private Integer timeConsumedMillis = 0;
    private Integer memoryConsumedBytes = 0;
    private Float points = 0.0f;

    private boolean analyzed = false;

    public Submission() {}

    public Submission(Integer id, Integer contestId, Integer creationTimeSeconds, Integer relativeTimeSeconds, Problem problem, Party party, String programmingLanguage, Verdict verdict, TestSet testSet, Integer passedTestCount, Integer timeConsumedMillis, Integer memoryConsumedBytes, Float points) {
        this.id = id;
        this.contestId = contestId;
        this.creationTimeSeconds = creationTimeSeconds;
        this.relativeTimeSeconds = relativeTimeSeconds;
        this.problem = problem;
        this.party = party;
        this.programmingLanguage = programmingLanguage;
        this.verdict = verdict;
        this.testSet = testSet;
        this.passedTestCount = passedTestCount;
        this.timeConsumedMillis = timeConsumedMillis;
        this.memoryConsumedBytes = memoryConsumedBytes;
        this.points = points;
    }

    public String getShortLanguage() {
        if (programmingLanguage == null) return "?";
        if (programmingLanguage.contains("Java")) return "Java";
        if (programmingLanguage.contains("Python")) return "Python";
        if (programmingLanguage.contains("C++") || programmingLanguage.contains("G++") || programmingLanguage.contains("Clang")) return "C++";
        if (programmingLanguage.contains("C#")) return "C#";
        if (programmingLanguage.contains("Kotlin")) return "Kotlin";
        if (programmingLanguage.contains("Rust")) return "Rust";
        if (programmingLanguage.contains("Go")) return "Go";
        if (programmingLanguage.contains("Pascal")) return "Pascal";
        return programmingLanguage.length() > 10 ? programmingLanguage.substring(0, 10) + "…" : programmingLanguage;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
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

    public Problem getProblem() {
        return problem;
    }

    public void setProblem(Problem problem) {
        this.problem = problem;
    }

    public Party getParty() {
        return party;
    }

    public void setParty(Party party) {
        this.party = party;
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

    public boolean isAnalyzed() {
        return analyzed;
    }

    public void setAnalyzed(boolean analyzed) {
        this.analyzed = analyzed;
    }
}
