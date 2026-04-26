-- ============================================================
-- V1__create_users_table.sql
-- Tạo bảng users để lưu thông tin Codeforces users
-- ============================================================

CREATE TABLE IF NOT EXISTS users (
    handle                      VARCHAR(50) PRIMARY KEY,
    email                       VARCHAR(255) DEFAULT '',
    vk_id                       VARCHAR(100) DEFAULT '',
    open_id                     VARCHAR(100) DEFAULT '',
    first_name                  VARCHAR(100) DEFAULT '',
    last_name                   VARCHAR(100) DEFAULT '',
    country                     VARCHAR(100) DEFAULT '',
    city                        VARCHAR(100) DEFAULT '',
    organization                VARCHAR(255) DEFAULT '',
    contribution                INTEGER NOT NULL DEFAULT 0,
    rank                        VARCHAR(50) DEFAULT '',
    rating                      INTEGER NOT NULL DEFAULT 0,
    max_rank                    VARCHAR(50) DEFAULT '',
    max_rating                  INTEGER NOT NULL DEFAULT 0,
    last_online_time_seconds    INTEGER NOT NULL DEFAULT 0,
    registration_time_seconds   INTEGER NOT NULL DEFAULT 0,
    friend_of_count             INTEGER NOT NULL DEFAULT 0,
    avatar_url                  VARCHAR(500) DEFAULT '',
    title_photo_url             VARCHAR(500) DEFAULT '',
    added_date                  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_crawl_at               TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_users_rating ON users(rating DESC);
CREATE INDEX IF NOT EXISTS idx_users_country ON users(country);
