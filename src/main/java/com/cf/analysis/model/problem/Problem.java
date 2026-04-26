package com.cf.analysis.model.problem;

public class Problem {
    private Integer id;
    private Integer contestId = 0;
    private String problemsetName = "";
    private String index = "";
    private String name = "";
    private String type = "";
    private Float points = 0.0f;
    private Integer rating = 0;
    private String[] tags = new String[0];

    public Problem() {}

    public Problem(Integer id) {
        this.id = id;
    }

    public Problem(Integer contestId, String problemsetName, String index, String name, String type, Float points, Integer rating, String[] tags) {
        this.contestId = contestId;
        this.problemsetName = problemsetName;
        this.index = index;
        this.name = name;
        this.type = type;
        this.points = points;
        this.rating = rating;
        this.tags = tags;
    }

    public Integer getId() {
        return id;
    }

    public Integer getContestId() {
        return contestId;
    }

    public void setContestId(Integer contestId) {
        this.contestId = contestId;
    }

    public String getProblemsetName() {
        return problemsetName;
    }

    public void setProblemsetName(String problemsetName) {
        this.problemsetName = problemsetName;
    }

    public String getIndex() {
        return index;
    }

    public void setIndex(String index) {
        this.index = index;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Float getPoints() {
        return points;
    }

    public void setPoints(Float points) {
        this.points = points;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public String[] getTags() {
        return tags;
    }

    public void setTags(String[] tags) {
        this.tags = tags;
    }
}
