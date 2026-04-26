package com.cf.analysis.model.analysis;

import java.util.ArrayList;
import java.util.List;

public class ComplexityAnalysis {
    private List<String> dataStructures = new ArrayList<>();
    private List<String> algorithms = new ArrayList<>();

    private String timeComplexity = "";
    private String spaceComplexity = "";

    private int difficultyScore = 0;

    public ComplexityAnalysis() {}

    public ComplexityAnalysis(List<String> dataStructures, List<String> algorithms, String timeComplexity, String spaceComplexity, int difficultyScore) {
        this.dataStructures = dataStructures;
        this.algorithms = algorithms;
        this.timeComplexity = timeComplexity;
        this.spaceComplexity = spaceComplexity;
        this.difficultyScore = difficultyScore;
    }

    public List<String> getDataStructures() {
        return dataStructures;
    }

    public void setDataStructures(List<String> dataStructures) {
        this.dataStructures = dataStructures;
    }

    public List<String> getAlgorithms() {
        return algorithms;
    }

    public void setAlgorithms(List<String> algorithms) {
        this.algorithms = algorithms;
    }

    public String getTimeComplexity() {
        return timeComplexity;
    }

    public void setTimeComplexity(String timeComplexity) {
        this.timeComplexity = timeComplexity;
    }

    public String getSpaceComplexity() {
        return spaceComplexity;
    }

    public void setSpaceComplexity(String spaceComplexity) {
        this.spaceComplexity = spaceComplexity;
    }

    public int getDifficultyScore() {
        return difficultyScore;
    }

    public void setDifficultyScore(int difficultyScore) {
        this.difficultyScore = difficultyScore;
    }


}
