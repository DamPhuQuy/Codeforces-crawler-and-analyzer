# Note

## User Panel

### 1. AddUserDialog

- Luồng:

1. Mở dialog → user nhập handle → nhấn "Thêm"
2. Gọi CF API để validate + lấy thông tin (background thread)
3. Nếu thành công → lưu DB → đóng dialog (sau 1.5s)
4. Nếu lỗi → hiển thị lỗi → cho nhập lại

- MODAL = true: Phải đóng dialog này mới dùng được MainFrame.

### 2. User panel

- Panel Tab 1: 👥 Quản Lý Nick Codeforces.
-
- Chức năng:
- - Hiển thị bảng danh sách tất cả nick đã thêm
- - Thêm nick mới (qua AddUserDialog)
- - Xóa nick đã chọn (có confirm dialog)
- - Cập nhật rating mới nhất từ Codeforces
-
- Cấu trúc class (cho người mới học Swing):
- - Constructor: gọi initComponents() và loadData()
- - initComponents(): tạo toolbar và bảng
- - loadData(): lấy data từ DB và đưa vào bảng (dùng SwingWorker)
- - refreshData(): public, để MainFrame gọi khi cần reload
