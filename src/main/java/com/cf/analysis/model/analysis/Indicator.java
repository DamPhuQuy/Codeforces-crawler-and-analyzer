package com.cf.analysis.model.analysis;

public class Indicator {
    private boolean detected = false;
    private String evidence = "";

    public Indicator() {}

    public Indicator(boolean detected, String evidence) {
        this.detected = detected;
        this.evidence = evidence;
    }

    public boolean isDetected() {
        return detected;
    }

    public String getEvidence() {
        return evidence;
    }
}
