package com.cf.analysis.model.analysis;

public class AnalysisOutput {
    private String explanation = "";
    private String rawJson = "";

    public AnalysisOutput() {}

    public AnalysisOutput(String explanation, String rawJson) {
        this.explanation = explanation;
        this.rawJson = rawJson;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public String getRawJson() {
        return rawJson;
    }

    public void setRawJson(String rawJson) {
        this.rawJson = rawJson;
    }


}
