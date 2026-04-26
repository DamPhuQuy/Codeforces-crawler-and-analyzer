-- ============================================================
-- V2__create_problems_table.sql
-- Tạo bảng problems để lưu thông tin bài toán từ Codeforces
-- ============================================================

CREATE TABLE IF NOT EXISTS problems (
    id                  SERIAL PRIMARY KEY,
    contest_id          INTEGER NOT NULL DEFAULT 0,
    problemset_name     VARCHAR(100) DEFAULT '',
    index               VARCHAR(10) DEFAULT '',
    name                VARCHAR(255) DEFAULT '',
    type                VARCHAR(50) DEFAULT '',
    points              REAL DEFAULT 0.0,
    rating              INTEGER DEFAULT 0,
    tags                TEXT,  -- JSON array of tags

    UNIQUE(contest_id, index)
);

CREATE INDEX IF NOT EXISTS idx_problems_contest_id ON problems(contest_id);
CREATE INDEX IF NOT EXISTS idx_problems_rating ON problems(rating);
