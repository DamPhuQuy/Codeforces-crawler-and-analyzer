package com.cf.analysis.bll;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import com.cf.analysis.crawler.CodeforcesApiCaller;
import com.cf.analysis.dal.UserDAO;
import com.cf.analysis.model.user.User;

public class UserService {

    private final UserDAO userDAO;
    private final CodeforcesApiCaller cfClient;

    public UserService(UserDAO userDAO, CodeforcesApiCaller cfClient) {
        this.userDAO = userDAO;
        this.cfClient = cfClient;
    }

    public User addUser(String handle) throws Exception {
        User existing = userDAO.findByHandle(handle.trim());
        if (existing != null) {
            throw new IllegalArgumentException("Handle '" + handle + "' da ton tai trong he thong!");
        }

        List<User> users = cfClient.getUserInfo(List.of(handle.trim()));
        if (users == null || users.isEmpty()) {
            throw new IOException("Khong tim thay handle '" + handle + "' tren Codeforces!");
        }

        User user = users.get(0);

        System.out.println("firstname: " + user.getFirstName());
        System.out.println("lastname: " + user.getLastName());
        System.out.println("email: " + user.getEmail());

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
        List<User> users = cfClient.getUserInfo(List.of(handle.trim()));
        if (users != null && !users.isEmpty()) {
            User updated = users.get(0);
            userDAO.updateRating(handle, updated.getRating(), updated.getMaxRating(), updated.getRank(), updated.getMaxRank());
            return updated;
        }
        return null;
    }

    /**
     * Kiểm tra handle có tồn tại trên Codeforces không.
     * Dùng để validate trước khi thêm vào hệ thống.
     */
    public boolean validateHandle(String handle) {
        try {
            List<User> users = cfClient.getUserInfo(List.of(handle.trim()));
            return users != null && !users.isEmpty();
        } catch (Exception _) {
            return false;
        }
    }
}
