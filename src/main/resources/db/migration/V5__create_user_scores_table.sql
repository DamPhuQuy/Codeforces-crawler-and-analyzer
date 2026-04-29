-- ============================================================
-- V5__create_user_scores_table.sql
-- Tạo bảng user_scores để lưu điểm đánh giá của từng user
-- Mối quan hệ: 1 User - 1 UserScore (1:1)
-- ============================================================

CREATE TABLE IF NOT EXISTS user_scores (
    handle              VARCHAR(50) PRIMARY KEY
                            REFERENCES users(handle)
                            ON DELETE CASCADE,
    rating              INTEGER NOT NULL DEFAULT 0,

    ds_score            DOUBLE PRECISION NOT NULL DEFAULT 0.0
                            CHECK (ds_score >= 0.0 AND ds_score <= 100.0),
    algorithm_score     DOUBLE PRECISION NOT NULL DEFAULT 0.0
                            CHECK (algorithm_score >= 0.0 AND algorithm_score <= 100.0),
    ai_score            DOUBLE PRECISION NOT NULL DEFAULT 100.0
                            CHECK (ai_score >= 0.0 AND ai_score <= 100.0),
    overall_score       DOUBLE PRECISION NOT NULL DEFAULT 0.0
                            CHECK (overall_score >= 0.0 AND overall_score <= 100.0),

    total_submissions   INTEGER NOT NULL DEFAULT 0,
    analyzed_submissions INTEGER NOT NULL DEFAULT 0,
    ai_detected_count   INTEGER NOT NULL DEFAULT 0,
    ai_usage_rate       DOUBLE PRECISION NOT NULL DEFAULT 0.0
                            CHECK (ai_usage_rate >= 0.0 AND ai_usage_rate <= 1.0),

    level               VARCHAR(20) NOT NULL DEFAULT 'BEGINNER',
    top_data_structure  VARCHAR(100) DEFAULT '',
    top_algorithm       VARCHAR(100) DEFAULT '',

    last_evaluated_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_user_scores_overall_score ON user_scores(overall_score DESC);
CREATE INDEX IF NOT EXISTS idx_user_scores_level ON user_scores(level);
CREATE INDEX IF NOT EXISTS idx_user_scores_ai_usage_rate ON user_scores(ai_usage_rate DESC);
