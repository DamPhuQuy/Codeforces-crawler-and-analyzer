package com.cf.analysis.model;

import java.sql.Timestamp;

/**
 * Model đại diện cho một Codeforces user.
 * Tương ứng với bảng "users" trong PostgreSQL.
 *
 * Đây là một POJO (Plain Old Java Object) - chỉ chứa dữ liệu,
 * không có logic nghiệp vụ nào ở đây.
 */
public class User {

    private String    handle;       // Tên đăng nhập CF (Primary Key)
    private String    displayName;  // Tên đầy đủ
    private int       rating;       // Rating hiện tại
    private int       maxRating;    // Rating cao nhất từng đạt
    private String    rank;         // Rank (newbie, pupil, specialist, expert, ...)
    private String    country;      // Quốc gia
    private String    avatarUrl;    // URL ảnh đại diện
    private Timestamp addedDate;    // Ngày thêm vào hệ thống
    private Timestamp lastCrawlAt;  // Lần crawl gần nhất

    public User() {}

    public User(String handle) {
        this.handle = handle;
    }

    // ==================== Getters & Setters ====================

    public String getHandle()                        { return handle; }
    public void   setHandle(String handle)           { this.handle = handle; }

    public String getDisplayName()                   { return displayName; }
    public void   setDisplayName(String displayName) { this.displayName = displayName; }

    public int  getRating()              { return rating; }
    public void setRating(int rating)    { this.rating = rating; }

    public int  getMaxRating()               { return maxRating; }
    public void setMaxRating(int maxRating)  { this.maxRating = maxRating; }

    public String getRank()            { return rank; }
    public void   setRank(String rank) { this.rank = rank; }

    public String getCountry()               { return country; }
    public void   setCountry(String country) { this.country = country; }

    public String getAvatarUrl()                 { return avatarUrl; }
    public void   setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public Timestamp getAddedDate()                  { return addedDate; }
    public void      setAddedDate(Timestamp d)       { this.addedDate = d; }

    public Timestamp getLastCrawlAt()                { return lastCrawlAt; }
    public void      setLastCrawlAt(Timestamp t)     { this.lastCrawlAt = t; }

    @Override
    public String toString() {
        return "User{handle='" + handle + "', rating=" + rating + ", rank='" + rank + "'}";
    }
}
