package com.cf.analysis.ui.controllers;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.cf.analysis.bll.UserService;
import com.cf.analysis.model.user.User;

/**
 * Controller cho User Management Panel.
 * Xử lý logic điều khiển giữa UI và Service layer.
 */
public class UserManagementController {

    private final UserService userService;

    public UserManagementController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Lấy danh sách tất cả users (async).
     */
    public CompletableFuture<List<User>> getAllUsers() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return userService.getAllUsers();
            } catch (Exception e) {
                throw new RuntimeException("Loi tai danh sach users: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Thêm user mới (async).
     */
    public CompletableFuture<User> addUser(String handle) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return userService.addUser(handle);
            } catch (Exception e) {
                throw new RuntimeException("Loi them user: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Xóa user (async).
     */
    public CompletableFuture<Void> removeUser(String handle) {
        return CompletableFuture.runAsync(() -> {
            try {
                userService.removeUser(handle);
            } catch (Exception e) {
                throw new RuntimeException("Loi xoa user: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Cập nhật rating của user từ Codeforces API (async).
     */
    public CompletableFuture<User> refreshUserInfo(String handle) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return userService.refreshUserInfo(handle);
            } catch (Exception e) {
                throw new RuntimeException("Loi cap nhat rating: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Validate handle format.
     */
    public boolean isValidHandle(String handle) {
        if (handle == null || handle.trim().isEmpty()) {
            return false;
        }
        // Codeforces handle: 3-24 ký tự, chỉ chứa chữ, số, gạch dưới
        return handle.matches("^[a-zA-Z0-9_]{3,24}$");
    }
}
