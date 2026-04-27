package com.cf.analysis.model.analysis;

public class AiIndicators {
    private Indicator tooClean = new Indicator();
    private Indicator textbookComments = new Indicator();
    private Indicator perfectNaming = new Indicator();
    private Indicator aiPattern = new Indicator();
    private Indicator tooPerfect = new Indicator();
    private Indicator wrongStyle = new Indicator();

    public AiIndicators() {}

    public AiIndicators(Indicator tooClean, Indicator textbookComments, Indicator perfectNaming, Indicator aiPattern, Indicator tooPerfect, Indicator wrongStyle) {
        this.tooClean = tooClean;
        this.textbookComments = textbookComments;
        this.perfectNaming = perfectNaming;
        this.aiPattern = aiPattern;
        this.tooPerfect = tooPerfect;
        this.wrongStyle = wrongStyle;
    }

    public int getPositiveCount() {
        int count = 0;
        if (tooClean != null && tooClean.isDetected()) count++;
        if (textbookComments != null && textbookComments.isDetected()) count++;
        if (perfectNaming != null && perfectNaming.isDetected()) count++;
        if (aiPattern != null && aiPattern.isDetected()) count++;
        if (tooPerfect != null && tooPerfect.isDetected()) count++;
        if (wrongStyle != null && wrongStyle.isDetected()) count++;
        return count;
    }

    public Indicator getTooClean() {
        return tooClean;
    }

    public void setTooClean(Indicator tooClean) {
        this.tooClean = tooClean;
    }

    public Indicator getTextbookComments() {
        return textbookComments;
    }

    public void setTextbookComments(Indicator textbookComments) {
        this.textbookComments = textbookComments;
    }

    public Indicator getPerfectNaming() {
        return perfectNaming;
    }

    public void setPerfectNaming(Indicator perfectNaming) {
        this.perfectNaming = perfectNaming;
    }

    public Indicator getAiPattern() {
        return aiPattern;
    }

    public void setAiPattern(Indicator aiPattern) {
        this.aiPattern = aiPattern;
    }

    public Indicator getTooPerfect() {
        return tooPerfect;
    }

    public void setTooPerfect(Indicator tooPerfect) {
        this.tooPerfect = tooPerfect;
    }

    public Indicator getWrongStyle() {
        return wrongStyle;
    }

    public void setWrongStyle(Indicator wrongStyle) {
        this.wrongStyle = wrongStyle;
    }
}
