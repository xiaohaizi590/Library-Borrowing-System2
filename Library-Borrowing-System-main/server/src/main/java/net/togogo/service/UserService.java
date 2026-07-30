package net.togogo.service;

import net.togogo.dto.ChangePasswordRequest;
import net.togogo.dto.LoginRequest;
import net.togogo.dto.LoginResponse;
import net.togogo.dto.PageResponse;
import net.togogo.dto.RegisterRequest;
import net.togogo.dto.ResetPasswordRequest;
import net.togogo.dto.UpdateUserRequest;
import net.togogo.dto.UserDTO;
import org.springframework.data.domain.Pageable;

import java.util.Map;

public interface UserService {
    UserDTO register(RegisterRequest request);
    LoginResponse login(LoginRequest request, String ip);
    PageResponse<UserDTO> getAllUsers(Pageable pageable);
    UserDTO getUserById(Long id);
    UserDTO getUserByUsername(String username);
    UserDTO getUserByPhone(String phone);
    void deleteUser(Long id);
    UserDTO updateUser(Long id, UpdateUserRequest request);

    Map<String, String> generateCaptcha();
    void verifyCaptcha(String captchaKey, String inputCaptcha);
    UserDTO restoreUser(Long id);
    PageResponse<UserDTO> getDeletedUsers(Pageable pageable);
    void cleanExpiredUsers(int retentionDays);
    /**
    * 当前登录用户修改自己的密码（需验证原密码）
     */
    void changePassword(ChangePasswordRequest request);
    /**
     * 管理员重置指定用户的密码（无需原密码）
     */
    void resetPassword(Long userId, ResetPasswordRequest request);
}
