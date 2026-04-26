package com.cf.analysis.model.analysis;

import java.sql.Timestamp;

public class Analysis {

    private long id = 0;
    private long submissionId = 0;

    private AiResult aiResult = new AiResult();

    private ComplexityAnalysis complexityAnalysis = new ComplexityAnalysis();

    private AnalysisOutput analysisOutput = new AnalysisOutput();

    private Timestamp analyzedAt = new Timestamp(System.currentTimeMillis());

    public Analysis() {}

    public Analysis(long submissionId, AiResult aiResult, ComplexityAnalysis complexityAnalysis, AnalysisOutput analysisOutput) {
        this.submissionId = submissionId;
        this.aiResult = aiResult;
        this.complexityAnalysis = complexityAnalysis;
        this.analysisOutput = analysisOutput;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getSubmissionId() {
        return submissionId;
    }

    public void setSubmissionId(long submissionId) {
        this.submissionId = submissionId;
    }

    public AiResult getAiResult() {
        return aiResult;
    }

    public void setAiResult(AiResult aiResult) {
        this.aiResult = aiResult;
    }

    public ComplexityAnalysis getComplexityAnalysis() {
        return complexityAnalysis;
    }

    public void setComplexityAnalysis(ComplexityAnalysis complexityAnalysis) {
        this.complexityAnalysis = complexityAnalysis;
    }

    public AnalysisOutput getAnalysisOutput() {
        return analysisOutput;
    }

    public void setAnalysisOutput(AnalysisOutput analysisOutput) {
        this.analysisOutput = analysisOutput;
    }

    public Timestamp getAnalyzedAt() {
        return analyzedAt;
    }

    public void setAnalyzedAt(Timestamp analyzedAt) {
        this.analyzedAt = analyzedAt;
    }
}
