-- ============================================================
-- V1__create_schema.sql
-- Migration đầu tiên: tạo toàn bộ schema cơ sở dữ liệu.
--
-- Quy tắc đặt tên file Flyway:
--   V{version}__{description}.sql
--   V = Versioned (chạy 1 lần, có checksum)
--   R = Repeatable   (chạy lại mỗi khi nội dung thay đổi)
--   U = Undo (chỉ có trong Flyway Pro)
--
-- Tất cả DDL dùng IF NOT EXISTS để an toàn khi chạy lại.
-- ============================================================

-- ============================================================
-- BẢNG 1: users
-- Lưu thông tin tài khoản Codeforces được theo dõi
-- ============================================================
CREATE TABLE IF NOT EXISTS users (
    handle          VARCHAR(50) PRIMARY KEY,              -- Codeforces handle (duy nhất)
    display_name    VARCHAR(100),                         -- Tên đầy đủ (firstName + lastName)
    rating          INTEGER NOT NULL DEFAULT 0,           -- Rating hiện tại
    max_rating      INTEGER NOT NULL DEFAULT 0,           -- Rating cao nhất từng đạt
    rank            VARCHAR(50) DEFAULT 'newbie',         -- Rank: newbie / pupil / specialist / expert / ...
    country         VARCHAR(100),                         -- Quốc gia
    avatar_url      VARCHAR(500),                         -- URL ảnh đại diện
    added_date      TIMESTAMPTZ NOT NULL DEFAULT NOW(),   -- Ngày thêm vào hệ thống
    last_crawl_at   TIMESTAMPTZ                           -- Lần crawl submission gần nhất (NULL = chưa crawl)
);

-- Comment bảng và cột cho documentation
COMMENT ON TABLE  users               IS 'Danh sách Codeforces accounts được theo dõi trong hệ thống';
COMMENT ON COLUMN users.handle        IS 'Codeforces handle, case-sensitive, là Primary Key';
COMMENT ON COLUMN users.rating        IS 'Rating hiện tại trên Codeforces (0 nếu chưa thi)';
COMMENT ON COLUMN users.rank          IS 'Rank CF: newbie, pupil, specialist, expert, candidate master, master, international master, grandmaster, international grandmaster, legendary grandmaster';
COMMENT ON COLUMN users.last_crawl_at IS 'Thời điểm crawl submissions lần cuối; NULL = chưa bao giờ crawl';

-- ============================================================
-- BẢNG 2: submissions
-- Lưu các bài nộp đã crawl từ Codeforces
-- ============================================================
CREATE TABLE IF NOT EXISTS submissions (
    id              BIGSERIAL PRIMARY KEY,                -- ID nội bộ (auto-increment)
    user_handle     VARCHAR(50) NOT NULL
                        REFERENCES users(handle)
                        ON DELETE CASCADE,                -- Xóa user → xóa submissions
    submission_id   BIGINT UNIQUE NOT NULL,               -- ID gốc trên Codeforces (unique toàn hệ thống)
    contest_id      INTEGER,                              -- NULL nếu nộp trong practice (không có contest)
    problem_index   VARCHAR(10),                          -- Index bài: A, B1, C2, ...
    problem_name    VARCHAR(255),                         -- Tên bài toán đầy đủ
    language        VARCHAR(100),                         -- VD: "GNU G++17 7.3.0", "Java 17", "PyPy3"
    verdict         VARCHAR(50),                          -- OK / WRONG_ANSWER / TIME_LIMIT_EXCEEDED / ...
    time_ms         INTEGER NOT NULL DEFAULT 0,           -- Thời gian chạy (milliseconds)
    memory_kb       INTEGER NOT NULL DEFAULT 0,           -- Bộ nhớ sử dụng (kilobytes)
    source_code     TEXT,                                 -- Source code đã crawl (NULL nếu code bị ẩn)
    submitted_at    TIMESTAMPTZ,                          -- Thời gian nộp bài gốc trên Codeforces
    crawled_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()    -- Thời gian crawl về hệ thống
);

COMMENT ON TABLE  submissions               IS 'Các bài nộp (submissions) đã crawl từ Codeforces API';
COMMENT ON COLUMN submissions.submission_id IS 'ID gốc trên Codeforces, dùng để tránh crawl trùng';
COMMENT ON COLUMN submissions.source_code   IS 'Source code đầy đủ; NULL nếu submission bị ẩn (private contest)';
COMMENT ON COLUMN submissions.contest_id    IS 'NULL nếu đây là submission trong Practice (không phải contest)';

-- Index cho các query thường dùng
CREATE INDEX IF NOT EXISTS idx_submissions_user_handle
    ON submissions(user_handle);

CREATE INDEX IF NOT EXISTS idx_submissions_submission_id
    ON submissions(submission_id);

CREATE INDEX IF NOT EXISTS idx_submissions_submitted_at
    ON submissions(submitted_at DESC);

-- ============================================================
-- BẢNG 3: analyses
-- Lưu kết quả phân tích AI (Gemini) của từng submission
-- ============================================================
CREATE TABLE IF NOT EXISTS analyses (
    id                  BIGSERIAL PRIMARY KEY,
    submission_id       BIGINT UNIQUE NOT NULL
                            REFERENCES submissions(id)
                            ON DELETE CASCADE,            -- Xóa submission → xóa analysis

    -- Kết quả phân tích cấu trúc dữ liệu & thuật toán
    data_structures     TEXT,                             -- JSON array: ["Array", "HashMap", "PriorityQueue"]
    algorithms          TEXT,                             -- JSON array: ["BFS", "Dynamic Programming"]

    -- Phát hiện AI
    ai_detected         BOOLEAN NOT NULL DEFAULT FALSE,   -- TRUE = phát hiện code dùng AI
    ai_confidence       DOUBLE PRECISION
                            NOT NULL DEFAULT 0.0
                            CHECK (ai_confidence >= 0.0 AND ai_confidence <= 1.0),

    -- 6 tiêu chí phát hiện AI (lưu dạng JSON object)
    ai_indicators       TEXT,                             -- JSON: {too_clean, textbook_comments, ...}

    -- Dòng code bị highlight nghi dùng AI
    highlighted_lines   TEXT,                             -- JSON array: [{line, reason, category}]

    -- Phân tích độ phức tạp
    time_complexity     VARCHAR(50),                      -- VD: "O(n log n)"
    space_complexity    VARCHAR(50),                      -- VD: "O(n)"
    difficulty_score    INTEGER
                            NOT NULL DEFAULT 0
                            CHECK (difficulty_score >= 0 AND difficulty_score <= 10),

    -- Nhận xét tổng quan
    explanation         TEXT,                             -- Giải thích bằng tiếng Việt

    -- Debug
    raw_json            TEXT,                             -- JSON gốc từ Gemini (để debug khi cần)
    analyzed_at         TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE  analyses                  IS 'Kết quả phân tích AI (Google Gemini) cho từng submission';
COMMENT ON COLUMN analyses.ai_confidence    IS 'Độ tin cậy phát hiện AI: 0.0 (chắc chắn không) đến 1.0 (chắc chắn có)';
COMMENT ON COLUMN analyses.difficulty_score IS 'Độ khó ước tính: 1 (dễ nhất) đến 10 (khó nhất)';
COMMENT ON COLUMN analyses.raw_json         IS 'Lưu response thô từ Gemini để debug, không dùng trong app';

CREATE INDEX IF NOT EXISTS idx_analyses_submission_id
    ON analyses(submission_id);

CREATE INDEX IF NOT EXISTS idx_analyses_ai_detected
    ON analyses(ai_detected)
    WHERE ai_detected = TRUE;                             -- Partial index: chỉ index row bị phát hiện

-- ============================================================
-- BẢNG 4: settings
-- Cài đặt ứng dụng dạng key-value
-- ============================================================
CREATE TABLE IF NOT EXISTS settings (
    key         VARCHAR(100) PRIMARY KEY,
    value       TEXT,
    description VARCHAR(255),                             -- Mô tả ngắn gọn cho từng setting
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE settings IS 'Cài đặt ứng dụng dạng key-value (Gemini API key, crawl interval, ...)';
