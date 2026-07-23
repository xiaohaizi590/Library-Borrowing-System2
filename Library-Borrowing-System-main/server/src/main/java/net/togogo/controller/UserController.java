package net.togogo.controller;

import net.togogo.common.Result;
import net.togogo.dto.*;

import net.togogo.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;


    @PostMapping("/register")
    public Result<UserDTO> register(@Valid @RequestBody RegisterRequest request) {
        UserDTO userDTO = userService.register(request);
        return Result.success("注册成功", userDTO);
    }

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        // 获取客户端IP地址
        String clientIp = getClientIpAddress(httpRequest);
        LoginResponse response = userService.login(request, clientIp);
        return Result.success("登录成功", response);
    }

    /**
     * 获取客户端真实IP地址
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多个代理情况，取第一个IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    @GetMapping("/getAllUsers")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<PageResponse<UserDTO>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createTime"));
        PageResponse<UserDTO> users = userService.getAllUsers(pageable);
        return Result.success(users);
    }

    @GetMapping("/getUserById/{id}")
    @PreAuthorize("hasRole('ADMIN') or @userServiceImpl.isOwnUser(#id)")
    public Result<UserDTO> getUserById(@PathVariable Long id) {
        UserDTO userDTO = userService.getUserById(id);
        return Result.success(userDTO);
    }

    @GetMapping("/getUserByUsername/{username}")
    @PreAuthorize("hasRole('ADMIN') or #username == authentication.name")
    public Result<UserDTO> getUserByUsername(@PathVariable String username) {
        UserDTO userDTO = userService.getUserByUsername(username);
        return Result.success(userDTO);
    }
  

    @GetMapping("/getUserByPhone/{phone}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<UserDTO> getUserByPhone(@PathVariable String phone) {
        UserDTO userDTO = userService.getUserByPhone(phone);
        return Result.success(userDTO);
    }

    @PutMapping("/updateUser/{id}")
    @PreAuthorize("hasRole('ADMIN') or @userServiceImpl.isOwnUser(#id)")
    public Result<UserDTO> updateUser(@PathVariable Long id, @RequestBody UpdateUserRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        
        if (!isAdmin && request.getRole() != null) {
            return Result.error(403, "普通用户不能修改角色");
        }
        
        UserDTO userDTO = userService.updateUser(id, request);
        return Result.success("更新成功", userDTO);
    }

    @DeleteMapping("/deleteUser/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return Result.success();
    }

    @GetMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public Result<UserDTO> getCurrentUserProfile() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        UserDTO userDTO = userService.getUserByUsername(username);
        return Result.success(userDTO);
    }

    @GetMapping("/captcha")
    public Result<Map<String, String>> getCaptcha() {
        Map<String, String> captcha = userService.generateCaptcha();
        return Result.success(captcha);
    }//获取验证码
    //校验验证码
    @PostMapping("/captcha/verify")
    public Result<Boolean> verifyCaptcha(@RequestBody Map<String, String> request) {
        String captchaKey = request.get("captchaKey");
        String inputCaptcha = request.get("captcha");

        if (captchaKey == null || inputCaptcha == null) {
            return Result.error(400, "验证码参数不能为空");
        }
       userService.verifyCaptcha(captchaKey, inputCaptcha);
        return Result.success("验证成功", true);
    }

}
