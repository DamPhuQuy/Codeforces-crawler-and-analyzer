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
    public CompletableFuture<List<User>> getAllUsersAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return userService.getAllUsers();
            } catch (Exception e) {
                throw new RuntimeException("Lỗi tải danh sách users: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Thêm user mới (async).
     */
    public CompletableFuture<User> addUserAsync(String handle) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return userService.addUser(handle);
            } catch (Exception e) {
                throw new RuntimeException("Lỗi thêm user: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Xóa user (async).
     */
    public CompletableFuture<Void> removeUserAsync(String handle) {
        return CompletableFuture.runAsync(() -> {
            try {
                userService.removeUser(handle);
            } catch (Exception e) {
                throw new RuntimeException("Lỗi xóa user: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Cập nhật rating của user từ Codeforces API (async).
     */
    public CompletableFuture<User> refreshUserInfoAsync(String handle) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return userService.refreshUserInfo(handle);
            } catch (Exception e) {
                throw new RuntimeException("Lỗi cập nhật rating: " + e.getMessage(), e);
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
