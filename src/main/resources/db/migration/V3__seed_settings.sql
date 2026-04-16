-- ============================================================
-- V3__seed_settings.sql
-- Migration thứ 3: Seed dữ liệu mặc định cho bảng settings.
--
-- Dùng INSERT ... ON CONFLICT DO NOTHING để:
--   - Thêm nếu key chưa tồn tại
--   - Bỏ qua (không ghi đè) nếu user đã cấu hình
-- ============================================================

INSERT INTO settings (key, value, description) VALUES
    -- ========== Gemini AI ==========
    (
        'gemini_api_key',
        '',
        'Google Gemini API Key. Lấy miễn phí tại https://aistudio.google.com/app/apikey (free: 15 req/min)'
    ),
    (
        'gemini_model',
        'gemini-2.0-flash',
        'Model Gemini sử dụng để phân tích. Khuyến nghị: gemini-2.0-flash (nhanh, rẻ) hoặc gemini-1.5-pro (chính xác hơn)'
    ),
    (
        'gemini_temperature',
        '0.1',
        'Temperature khi gọi Gemini (0.0 = nhất quán, 1.0 = sáng tạo). Giữ thấp để kết quả phân tích ổn định'
    ),
    (
        'gemini_max_tokens',
        '2048',
        'Số token tối đa trong response Gemini. 2048 là đủ cho đầu ra JSON phân tích'
    ),

    -- ========== Crawl Settings ==========
    (
        'crawl_interval_hours',
        '24',
        'Số giờ giữa 2 lần crawl tự động. Mặc định 24h (1 ngày 1 lần). Tối thiểu 1h'
    ),
    (
        'max_submissions_per_crawl',
        '50',
        'Số submissions tối đa lấy từ API mỗi lần crawl 1 user. Tăng nếu user nộp nhiều'
    ),
    (
        'crawl_accepted_only',
        'true',
        'Chỉ crawl submission Accepted (verdict=OK). true = chỉ lấy AC; false = lấy tất cả'
    ),
    (
        'crawl_delay_api_ms',
        '1200',
        'Độ trễ (ms) giữa 2 request đến Codeforces API để tránh bị rate-limit. Tối thiểu 1000ms'
    ),
    (
        'crawl_delay_scrape_ms',
        '2500',
        'Độ trễ (ms) khi scrape HTML lấy source code. Dài hơn API delay để tránh IP bị block'
    ),

    -- ========== Analysis Settings ==========
    (
        'analysis_batch_size',
        '10',
        'Số submissions phân tích mỗi batch khi chạy "Phân Tích Tất Cả". Nhỏ hơn = ổn định hơn'
    ),
    (
        'analysis_code_max_chars',
        '8000',
        'Độ dài tối đa source code gửi cho Gemini (ký tự). Code dài hơn sẽ bị cắt bớt'
    ),

    -- ========== App Settings ==========
    (
        'app_version',
        '1.0.0',
        'Phiên bản ứng dụng hiện tại. Không chỉnh sửa thủ công'
    ),
    (
        'app_first_run',
        'true',
        'true = lần đầu chạy app (dùng để hiển thị hướng dẫn). Tự động chuyển thành false sau lần đầu'
    )
ON CONFLICT (key) DO NOTHING;

-- Thêm comment cho bảng settings
COMMENT ON TABLE settings IS 'Cài đặt ứng dụng đã được khởi tạo bởi V3 migration';
