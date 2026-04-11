package com.coreforces.app.dal.entity;

import java.time.LocalDateTime;

public class Analysis {
    private Long submissionId;
    private String danhHieu;

    private String dsaTags;
    private Float aiScore;
    private String doPhucTap;

    private LocalDateTime thoiGianPhanTich;

    public Analysis() {}

    public Analysis(String danhHieu, String dsaTags, Float aiScore, String doPhanTap) {
        this.danhHieu = danhHieu;
        this.dsaTags = dsaTags;
        this.aiScore = aiScore;
    }

    public Analysis(Analysis analysis) {
        this.danhHieu = analysis.danhHieu;
        this.dsaTags = analysis.dsaTags;
        this.aiScore = analysis.aiScore;
    }

    public Long getSubmissionId() {
        return submissionId;
    }

    public String getDanhHieu() {
        return danhHieu;
    }

    public void setDanhHieu(String danhHieu) {
        this.danhHieu = danhHieu;
    }

    public String getDsaTags() {
        return dsaTags;
    }

    public void setDsaTags(String dsaTags) {
        this.dsaTags = dsaTags;
    }

    public Float getAiScore() {
        return aiScore;
    }

    public void setAiScore(Float aiScore) {
        this.aiScore = aiScore;
    }

    public String getDoPhucTap() {
        return doPhucTap;
    }

    public void setDoPhucTap(String doPhucTap) {
        this.doPhucTap = doPhucTap;
    }

    public LocalDateTime getThoiGianPhanTich() {
        return thoiGianPhanTich;
    }

    public void setThoiGianPhanTich(LocalDateTime thoiGianPhanTich) {
        this.thoiGianPhanTich = thoiGianPhanTich;
    }
}
