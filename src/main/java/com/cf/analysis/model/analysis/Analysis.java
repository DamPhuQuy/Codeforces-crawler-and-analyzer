package com.cf.analysis.model.analysis;

import java.time.LocalDateTime;

public class Analysis {

    private Long id;
    private Long submissionId;

    private AiResult aiResult = new AiResult();

    private ComplexityAnalysis complexityAnalysis = new ComplexityAnalysis();

    private AnalysisOutput analysisOutput = new AnalysisOutput();

    private LocalDateTime analyzedAt = LocalDateTime.now();

    public Analysis() {}

    public Analysis(Long submissionId, AiResult aiResult, ComplexityAnalysis complexityAnalysis, AnalysisOutput analysisOutput) {
        this.submissionId = submissionId;
        this.aiResult = aiResult;
        this.complexityAnalysis = complexityAnalysis;
        this.analysisOutput = analysisOutput;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSubmissionId() {
        return submissionId;
    }

    public void setSubmissionId(Long submissionId) {
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

    public LocalDateTime getAnalyzedAt() {
        return analyzedAt;
    }

    public void setAnalyzedAt(LocalDateTime analyzedAt) {
        this.analyzedAt = analyzedAt;
    }
}
