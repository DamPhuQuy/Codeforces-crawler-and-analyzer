-- ============================================================
-- V4__create_analyses_table.sql
-- Tạo bảng analyses để lưu kết quả phân tích AI
-- ============================================================

CREATE TABLE IF NOT EXISTS analyses (
    id                  BIGSERIAL PRIMARY KEY,
    submission_id       BIGINT NOT NULL
                            REFERENCES submissions(id)
                            ON DELETE CASCADE,

    ai_confidence       REAL NOT NULL DEFAULT 0.0
                            CHECK (ai_confidence >= 0.0 AND ai_confidence <= 1.0),

    too_clean           BOOLEAN DEFAULT FALSE,
    too_clean_evidence  TEXT DEFAULT '',
    textbook_comments   BOOLEAN DEFAULT FALSE,
    textbook_evidence   TEXT DEFAULT '',
    perfect_naming      BOOLEAN DEFAULT FALSE,
    naming_evidence     TEXT DEFAULT '',
    ai_pattern          BOOLEAN DEFAULT FALSE,
    pattern_evidence    TEXT DEFAULT '',
    too_perfect         BOOLEAN DEFAULT FALSE,
    perfect_evidence    TEXT DEFAULT '',
    wrong_style         BOOLEAN DEFAULT FALSE,
    style_evidence      TEXT DEFAULT '',

    data_structures     TEXT,  -- JSON array
    algorithms          TEXT,  -- JSON array
    time_complexity     VARCHAR(50) DEFAULT '',
    space_complexity    VARCHAR(50) DEFAULT '',
    difficulty_score    INTEGER NOT NULL DEFAULT 0
                            CHECK (difficulty_score >= 0 AND difficulty_score <= 10),

    explanation         TEXT DEFAULT '',
    raw_json            TEXT DEFAULT '',

    analyzed_at         TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    UNIQUE(submission_id)
);

CREATE INDEX IF NOT EXISTS idx_analyses_submission_id ON analyses(submission_id);
CREATE INDEX IF NOT EXISTS idx_analyses_ai_confidence ON analyses(ai_confidence DESC);
