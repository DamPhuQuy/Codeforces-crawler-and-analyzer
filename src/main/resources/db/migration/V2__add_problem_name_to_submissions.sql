-- Migration to add problem_name to submissions table
ALTER TABLE submissions ADD COLUMN problem_name TEXT;
