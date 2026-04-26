-- ============================================================
-- V3__create_submissions_table.sql
-- Tạo bảng submissions để lưu các bài nộp từ Codeforces
-- ============================================================

CREATE TABLE IF NOT EXISTS submissions (
    id                      SERIAL PRIMARY KEY,
    user_handle             VARCHAR(50) NOT NULL
                                REFERENCES users(handle)
                                ON DELETE CASCADE,
    language                VARCHAR(100) DEFAULT '',
    contest_id              INTEGER NOT NULL DEFAULT 0,
    creation_time_seconds   INTEGER NOT NULL DEFAULT 0,
    relative_time_seconds   INTEGER NOT NULL DEFAULT 0,
    problem_id              INTEGER NOT NULL DEFAULT 0
                                REFERENCES problems(id)
                                ON DELETE SET NULL,
    programming_language    VARCHAR(100) DEFAULT '',
    verdict                 VARCHAR(50) DEFAULT 'TESTING',
    test_set                VARCHAR(50) DEFAULT 'SAMPLES',
    passed_test_count       INTEGER NOT NULL DEFAULT 0,
    time_consumed_millis    INTEGER NOT NULL DEFAULT 0,
    memory_consumed_bytes   INTEGER NOT NULL DEFAULT 0,
    points                  REAL DEFAULT 0.0,
    source_code             TEXT DEFAULT '',
    submitted_at            TIMESTAMP,
    crawled_at              TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    analyzed                BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_submissions_user_handle ON submissions(user_handle);
CREATE INDEX IF NOT EXISTS idx_submissions_problem_id ON submissions(problem_id);
CREATE INDEX IF NOT EXISTS idx_submissions_verdict ON submissions(verdict);
CREATE INDEX IF NOT EXISTS idx_submissions_analyzed ON submissions(analyzed) WHERE analyzed = FALSE;
CREATE INDEX IF NOT EXISTS idx_submissions_submitted_at ON submissions(submitted_at DESC);
