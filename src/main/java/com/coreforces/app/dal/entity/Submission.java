package com.coreforces.app.dal.entity;

import java.time.LocalDateTime;

public class Submission {
    private Long id;
    private String danhHieuId;
    private String vanDe;
    private String language;
    private String text;
    private LocalDateTime nopVaoLuc;

    public Submission() { id += 1; }

    public Submission(Long id, String danhHieuId, String vanDe, String language, String text, LocalDateTime nopVaoLuc) {
        this.id = id;
        this.danhHieuId = danhHieuId;
        this.vanDe = vanDe;
        this.language = language;
        this.text = text;
        this.nopVaoLuc = nopVaoLuc;
    }

    public Submission(Submission submission) {
        this.id = submission.getId();
        this.danhHieuId = submission.getDanhHieuId();
        this.vanDe = submission.getVanDe();
        this.language = submission.getLanguage();
        this.text = submission.getText();
        this.nopVaoLuc = submission.getNopVaoLuc();
    }

    public Long getId() {
        return id;
    }

    public String getDanhHieuId() {
        return danhHieuId;
    }

    public void setDanhHieuId(String danhHieuId) {
        this.danhHieuId = danhHieuId;
    }

    public String getVanDe() {
        return vanDe;
    }

    public void setVanDe(String vanDe) {
        this.vanDe = vanDe;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public LocalDateTime getNopVaoLuc() {
        return nopVaoLuc;
    }

    public void setNopVaoLuc(LocalDateTime nopVaoLuc) {
        this.nopVaoLuc = nopVaoLuc;
    }
}
