-- ============================================================
-- V2__add_views_and_functions.sql
-- Migration thứ 2: tạo Views và Functions hỗ trợ truy vấn.
--
-- Views giúp:
--   - Tham chiếu dữ liệu JOIN phức tạp bằng tên đơn giản
--   - Tách biệt logic truy vấn với code Java
--   - Dễ debug trực tiếp trên pgAdmin / psql
-- ============================================================

-- ============================================================
-- VIEW 1: v_submissions_with_analysis
-- Xem submissions kèm trạng thái đã phân tích AI chưa
-- ============================================================
CREATE OR REPLACE VIEW v_submissions_with_analysis AS
SELECT
    s.id,
    s.user_handle,
    s.submission_id,
    s.contest_id,
    s.problem_index,
    s.problem_name,
    s.language,
    s.verdict,
    s.time_ms,
    s.memory_kb,
    s.submitted_at,
    s.crawled_at,

    -- Đã phân tích hay chưa (TRUE nếu có bản ghi trong bảng analyses)
    (a.id IS NOT NULL)      AS analyzed,

    -- Kết quả AI cơ bản (NULL nếu chưa phân tích)
    a.ai_detected,
    a.ai_confidence,
    a.difficulty_score,
    a.analyzed_at

FROM submissions s
LEFT JOIN analyses a ON a.submission_id = s.id;

COMMENT ON VIEW v_submissions_with_analysis
    IS 'Submissions kèm thông tin phân tích AI; analyzed=false nếu chưa có kết quả';

-- ============================================================
-- VIEW 2: v_user_stats
-- Thống kê tổng hợp cho từng user
-- ============================================================
CREATE OR REPLACE VIEW v_user_stats AS
SELECT
    u.handle,
    u.display_name,
    u.rating,
    u.rank,
    u.last_crawl_at,

    -- Tổng submissions đã crawl
    COUNT(DISTINCT s.id)                                    AS total_submissions,

    -- Số submissions đã có phân tích AI
    COUNT(DISTINCT a.id)                                    AS analyzed_count,

    -- Số submissions bị phát hiện dùng AI
    COUNT(DISTINCT a.id) FILTER (WHERE a.ai_detected = TRUE) AS ai_detected_count,

    -- Tỷ lệ dùng AI (0.0 nếu chưa có phân tích nào)
    CASE
        WHEN COUNT(DISTINCT a.id) > 0
        THEN ROUND(
            (COUNT(DISTINCT a.id) FILTER (WHERE a.ai_detected = TRUE))::NUMERIC
            / COUNT(DISTINCT a.id)::NUMERIC * 100,
            2
        )
        ELSE 0
    END                                                     AS ai_usage_percent,

    -- Điểm khó trung bình
    ROUND(AVG(a.difficulty_score)::NUMERIC, 1)              AS avg_difficulty,

    -- Confidence trung bình
    ROUND(AVG(a.ai_confidence)::NUMERIC, 3)                 AS avg_ai_confidence

FROM users u
LEFT JOIN submissions s ON s.user_handle = u.handle
LEFT JOIN analyses    a ON a.submission_id = s.id
GROUP BY u.handle, u.display_name, u.rating, u.rank, u.last_crawl_at;

COMMENT ON VIEW v_user_stats
    IS 'Thống kê tổng hợp cho mỗi user: số submission, tỷ lệ AI, độ khó trung bình';

-- ============================================================
-- VIEW 3: v_unanalyzed_submissions
-- Danh sách submissions cần phân tích AI (chưa có analysis, có source code)
-- ============================================================
CREATE OR REPLACE VIEW v_unanalyzed_submissions AS
SELECT
    s.id,
    s.user_handle,
    s.submission_id,
    s.problem_name,
    s.language,
    s.submitted_at,
    LENGTH(s.source_code) AS code_length   -- Độ dài code để ưu tiên phân tích
FROM submissions s
LEFT JOIN analyses a ON a.submission_id = s.id
WHERE a.id IS NULL                          -- Chưa có analysis
  AND s.source_code IS NOT NULL             -- Phải có source code
  AND s.source_code <> ''
ORDER BY s.submitted_at DESC;

COMMENT ON VIEW v_unanalyzed_submissions
    IS 'Submissions có source code nhưng chưa được phân tích AI; dùng cho batch analysis';

-- ============================================================
-- FUNCTION: fn_get_user_top_ds(handle TEXT)
-- Lấy top 5 cấu trúc dữ liệu thường dùng nhất của một user
-- Cần unnest JSON array → aggregate
-- ============================================================
CREATE OR REPLACE FUNCTION fn_get_user_top_ds(p_handle TEXT)
RETURNS TABLE (data_structure TEXT, usage_count BIGINT)
LANGUAGE SQL
STABLE
AS $$
    SELECT
        TRIM(ds_item) AS data_structure,
        COUNT(*)      AS usage_count
    FROM submissions s
    JOIN analyses a ON a.submission_id = s.id
    -- Unnest JSON array string thành các row riêng
    -- VD: '["Array","HashMap"]' → row "Array", row "HashMap"
    CROSS JOIN LATERAL (
        SELECT TRIM(BOTH '"' FROM json_array_elements_text(a.data_structures::JSON)) AS ds_item
        WHERE a.data_structures IS NOT NULL AND a.data_structures <> '[]' AND a.data_structures <> 'null'
    ) AS ds
    WHERE s.user_handle = p_handle
    GROUP BY TRIM(ds_item)
    ORDER BY COUNT(*) DESC
    LIMIT 5;
$$;

COMMENT ON FUNCTION fn_get_user_top_ds(TEXT)
    IS 'Trả về top 5 CTDL thường dùng nhất của user, kèm số lần xuất hiện';

-- ============================================================
-- FUNCTION: fn_get_user_top_algos(handle TEXT)
-- Tương tự fn_get_user_top_ds nhưng cho thuật toán
-- ============================================================
CREATE OR REPLACE FUNCTION fn_get_user_top_algos(p_handle TEXT)
RETURNS TABLE (algorithm TEXT, usage_count BIGINT)
LANGUAGE SQL
STABLE
AS $$
    SELECT
        TRIM(algo_item) AS algorithm,
        COUNT(*)        AS usage_count
    FROM submissions s
    JOIN analyses a ON a.submission_id = s.id
    CROSS JOIN LATERAL (
        SELECT TRIM(BOTH '"' FROM json_array_elements_text(a.algorithms::JSON)) AS algo_item
        WHERE a.algorithms IS NOT NULL AND a.algorithms <> '[]' AND a.algorithms <> 'null'
    ) AS algo
    WHERE s.user_handle = p_handle
    GROUP BY TRIM(algo_item)
    ORDER BY COUNT(*) DESC
    LIMIT 5;
$$;

COMMENT ON FUNCTION fn_get_user_top_algos(TEXT)
    IS 'Trả về top 5 thuật toán thường dùng nhất của user, kèm số lần xuất hiện';
