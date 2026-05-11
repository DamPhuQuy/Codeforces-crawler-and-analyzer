-- Add problem_name and submitted_at to submissions table
ALTER TABLE submissions ADD COLUMN IF NOT EXISTS problem_name TEXT;
ALTER TABLE submissions ADD COLUMN IF NOT EXISTS submitted_at TIMESTAMP;
