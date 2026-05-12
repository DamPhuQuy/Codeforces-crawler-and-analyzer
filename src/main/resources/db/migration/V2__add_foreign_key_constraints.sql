-- Add foreign key constraints with CASCADE delete to maintain referential integrity

-- user_scores references users
ALTER TABLE user_scores
ADD CONSTRAINT fk_user_scores_handle
FOREIGN KEY (handle) REFERENCES users(handle)
ON DELETE CASCADE;

-- submissions references users
ALTER TABLE submissions
ADD CONSTRAINT fk_submissions_user_handle
FOREIGN KEY (user_handle) REFERENCES users(handle)
ON DELETE CASCADE;

-- submissions references problems
ALTER TABLE submissions
ADD CONSTRAINT fk_submissions_problem_id
FOREIGN KEY (problem_id) REFERENCES problems(id)
ON DELETE CASCADE;

-- analyses references submissions
ALTER TABLE analyses
ADD CONSTRAINT fk_analyses_submission_id
FOREIGN KEY (submission_id) REFERENCES submissions(id)
ON DELETE CASCADE;
