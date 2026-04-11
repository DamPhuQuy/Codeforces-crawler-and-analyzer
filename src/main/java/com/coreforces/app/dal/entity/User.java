package com.coreforces.app.dal.entity;

import java.time.LocalDateTime;

public class User {
    private Long id = 0L;
    private String danhHieu;
    private String xepHang;
    private LocalDateTime layDuLieuLanCuoi;

    public User() { id += 1; }

    public User(String danhHieu, String xepHang, LocalDateTime layDuLieuLanCuoi) {
        id += 1;
        this.danhHieu = danhHieu;
        this.xepHang = xepHang;
        this.layDuLieuLanCuoi = layDuLieuLanCuoi;
    }

    public User(User user) {
        this.id = user.getId();
        this.danhHieu = user.getDanhHieu();
        this.xepHang = user.getXepHang();
    }

    public Long getId() {
        return id;
    }

    public String getDanhHieu() {
        return danhHieu;
    }

    public void setDanhHieu(String danhHieu) {
        this.danhHieu = danhHieu;
    }

    public LocalDateTime getLayDuLieuLanCuoi() {
        return layDuLieuLanCuoi;
    }

    public void setLayDuLieuLanCuoi(LocalDateTime layDuLieuLanCuoi) {
        this.layDuLieuLanCuoi = layDuLieuLanCuoi;
    }

    public String getXepHang() {
        return xepHang;
    }

    public void setXephang(String xephang) {
        this.xepHang = xephang;
    }
}
