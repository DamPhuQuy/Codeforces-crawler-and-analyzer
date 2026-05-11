package com.cf.analysis.model;

import com.cf.analysis.model.problem.Problem;
import com.cf.analysis.model.submission.Submission;

/**
 * Wrapper class to hold both Submission and its associated Problem metadata.
 * Used during crawling to ensure problem data is saved alongside submissions.
 */
public class SubmissionWithProblem {
    private final Submission submission;
    private final Problem problem;

    public SubmissionWithProblem(Submission submission, Problem problem) {
        this.submission = submission;
        this.problem = problem;
    }

    public Submission getSubmission() {
        return submission;
    }

    public Problem getProblem() {
        return problem;
    }
}
