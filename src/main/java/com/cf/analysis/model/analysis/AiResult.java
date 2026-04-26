package com.cf.analysis.model.analysis;

public class AiResult {
    private Float aiConfidence = 0.0f;
    private AiIndicators aiIndicators = new AiIndicators();

    public AiResult() {}

    public AiResult(Float aiConfidence, AiIndicators aiIndicators) {
        this.aiConfidence = aiConfidence;
        this.aiIndicators = aiIndicators;
    }

    public Float getAiConfidence() {
        return aiConfidence;
    }

    public void setAiConfidence(Float aiConfidence) {
        this.aiConfidence = aiConfidence;
    }

    public AiIndicators getAiIndicators() {
        return aiIndicators;
    }

    public void setAiIndicators(AiIndicators aiIndicators) {
        this.aiIndicators = aiIndicators;
    }
}
