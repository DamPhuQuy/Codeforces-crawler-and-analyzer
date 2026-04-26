package com.cf.analysis.bll;

import com.cf.analysis.crawler.CodeforcesApiClient;
import com.cf.analysis.dal.UserDAO;
import com.cf.analysis.model.user.User;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

/**
 * BLL - Nghiệp vụ quản lý Users.
 * Phối hợp giữa CodeforcesApiClient (lấy data từ CF) và UserDAO (lưu vào DB).
 */
public class UserService {

    private final UserDAO              userDAO  = new UserDAO();
    private final CodeforcesApiClient  cfClient = new CodeforcesApiClient();

    /**
     * Thêm Codeforces handle vào hệ thống.
     * Tự động gọi CF API để lấy thông tin và lưu vào DB.
     *
     * @throws IllegalArgumentException nếu handle đã tồn tại
     * @throws IOException              nếu handle không tồn tại trên CF hoặc lỗi mạng
     */
    public User addUser(String handle) throws Exception {
        // Kiểm tra trùng trước
        User existing = userDAO.findByHandle(handle.trim());
        if (existing != null) {
            throw new IllegalArgumentException("Handle '" + handle + "' đã tồn tại trong hệ thống!");
        }

        // Gọi CF API để lấy thông tin
        User user = cfClient.getUserInfo(handle.trim());
        if (user == null) {
            throw new IOException("Không tìm thấy handle '" + handle + "' trên Codeforces!");
        }

        // Lưu vào DB
        userDAO.insert(user);
        return user;
    }

    /** Xóa user và tất cả data liên quan. */
    public void removeUser(String handle) throws SQLException {
        userDAO.delete(handle);
    }

    /** Lấy tất cả users trong hệ thống. */
    public List<User> getAllUsers() throws SQLException {
        return userDAO.findAll();
    }

    /** Tìm user theo handle. */
    public User getUserByHandle(String handle) throws SQLException {
        return userDAO.findByHandle(handle);
    }

    /**
     * Cập nhật thông tin mới nhất (rating, rank) từ Codeforces.
     * Dùng khi user muốn refresh thông tin.
     */
    public User refreshUserInfo(String handle) throws Exception {
        User updated = cfClient.getUserInfo(handle);
        if (updated != null) {
            userDAO.updateRating(handle, updated.getRating(), updated.getMaxRating(), updated.getRank());
        }
        return updated;
    }

    /**
     * Kiểm tra handle có tồn tại trên Codeforces không.
     * Dùng để validate trước khi thêm vào hệ thống.
     */
    public boolean validateHandle(String handle) {
        try {
            return cfClient.getUserInfo(handle.trim()) != null;
        } catch (Exception e) {
            return false;
        }
    }
}
