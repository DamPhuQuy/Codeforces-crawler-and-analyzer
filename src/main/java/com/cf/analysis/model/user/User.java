package com.cf.analysis.model.user;

/**
 * Model đại diện cho một Codeforces user.
 * Tương ứng với bảng "users" trong PostgreSQL.
 *
 * Đây là một POJO (Plain Old Java Object) - chỉ chứa dữ liệu,
 * không có logic nghiệp vụ nào ở đây.
 */
public class User {

    private String handle = "";
    private String email = "";
    private String vkId = "";
    private String openId = "";
    private String firstName = "";
    private String lastName = "";
    private String country = "";
    private String city = "";
    private String organization = "";
    private int contribution = 0;
    private String rank = "";
    private int rating = 0;
    private String maxRank = "";
    private int maxRating = 0;
    private int lastOnlineTimeSeconds = 0;
    private int registrationTimeSeconds = 0;
    private int friendOfCount = 0;
    private String avatarUrl = "";
    private String titlePhotoUrl = "";


    public User() {}

    public User(String handle, String email, String vkId, String openId, String firstName, String lastName,
                String country, String city, String organization, int contribution, String rank, int rating,
                String maxRank, int maxRating, int lastOnlineTimeSeconds, int registrationTimeSeconds,
                int friendOfCount, String avatarUrl, String titlePhotoUrl) {
        this.handle = handle;
        this.email = email;
        this.vkId = vkId;
        this.openId = openId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.country = country;
        this.city = city;
        this.organization = organization;
        this.contribution = contribution;
        this.rank = rank;
        this.rating = rating;
        this.maxRank = maxRank;
        this.maxRating = maxRating;
        this.lastOnlineTimeSeconds = lastOnlineTimeSeconds;
        this.registrationTimeSeconds = registrationTimeSeconds;
        this.friendOfCount = friendOfCount;
        this.avatarUrl = avatarUrl;
        this.titlePhotoUrl = titlePhotoUrl;
    }

    public String getHandle() {
        return handle;
    }

    public void setHandle(String handle) {
        this.handle = handle;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getVkId() {
        return vkId;
    }

    public void setVkId(String vkId) {
        this.vkId = vkId;
    }

    public String getOpenId() {
        return openId;
    }

    public void setOpenId(String openId) {
        this.openId = openId;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getOrganization() {
        return organization;
    }

    public void setOrganization(String organization) {
        this.organization = organization;
    }

    public int getContribution() {
        return contribution;
    }

    public void setContribution(int contribution) {
        this.contribution = contribution;
    }

    public String getRank() {
        return rank;
    }

    public void setRank(String rank) {
        this.rank = rank;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public String getMaxRank() {
        return maxRank;
    }

    public void setMaxRank(String maxRank) {
        this.maxRank = maxRank;
    }

    public int getMaxRating() {
        return maxRating;
    }

    public void setMaxRating(int maxRating) {
        this.maxRating = maxRating;
    }

    public int getLastOnlineTimeSeconds() {
        return lastOnlineTimeSeconds;
    }

    public void setLastOnlineTimeSeconds(int lastOnlineTimeSeconds) {
        this.lastOnlineTimeSeconds = lastOnlineTimeSeconds;
    }

    public int getRegistrationTimeSeconds() {
        return registrationTimeSeconds;
    }

    public void setRegistrationTimeSeconds(int registrationTimeSeconds) {
        this.registrationTimeSeconds = registrationTimeSeconds;
    }

    public int getFriendOfCount() {
        return friendOfCount;
    }

    public void setFriendOfCount(int friendOfCount) {
        this.friendOfCount = friendOfCount;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getTitlePhotoUrl() {
        return titlePhotoUrl;
    }

    public void setTitlePhotoUrl(String titlePhotoUrl) {
        this.titlePhotoUrl = titlePhotoUrl;
    }


}
