package com.cf.analysis.model;

import com.cf.analysis.model.problem.Problem;
import com.cf.analysis.model.submission.Submission;

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
