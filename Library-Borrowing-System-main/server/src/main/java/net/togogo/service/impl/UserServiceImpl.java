package net.togogo.service.impl;

import lombok.RequiredArgsConstructor;
import net.togogo.common.BusinessException;
import net.togogo.common.ResultCode;
import net.togogo.dto.ChangePasswordRequest;
import net.togogo.dto.LoginRequest;
import net.togogo.dto.LoginResponse;
import net.togogo.dto.PageResponse;
import net.togogo.dto.RegisterRequest;
import net.togogo.dto.ResetPasswordRequest;
import net.togogo.dto.UpdateUserRequest;
import net.togogo.dto.UserDTO;
import net.togogo.entity.User;
import net.togogo.entity.UserArchive;
import net.togogo.repository.UserRepository;
import net.togogo.security.JwtUtil;
import net.togogo.service.UserService;
import net.togogo.util.PasswordUtil;

import net.togogo.service.LoginAttemptService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import net.togogo.repository.UserArchiveRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import com.wf.captcha.SpecCaptcha;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import net.togogo.mapper.UserMapper;
import java.time.LocalDateTime;

@Service("userServiceImpl")
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    // 注入 UserRepository
    private final UserRepository userRepository;
    // 注入 JwtUtil
    private final JwtUtil jwtUtil;
    // 注入 LoginAttemptService
    private final LoginAttemptService loginAttemptService;
    // 注入 RedisTemplate
    private final RedisTemplate<String, String> redisTemplate;
    // 注入 UserArchiveRepository
    private final UserArchiveRepository userArchiveRepository;

    // Lua 脚本：原子校验并删除验证码，返回 -1=过期 0=错误 1=成功
    private static final DefaultRedisScript<Long> VERIFY_CAPTCHA_SCRIPT = new DefaultRedisScript<>(
            "local stored = redis.call('GET', KEYS[1])\n" +
            "if not stored then\n" +
            "    return -1\n" +
            "end\n" +
            "if stored == ARGV[1] then\n" +
            "    redis.call('DEL', KEYS[1])\n" +
            "    return 1\n" +
            "end\n" +
            "return 0",
            Long.class);


    @Override
    @Transactional
    @CacheEvict(value = "users", allEntries = true)
    public UserDTO register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException(ResultCode.USERNAME_EXIST);
        }
        if (userRepository.existsByPhone(request.getPhone())) {
            throw new BusinessException(ResultCode.PHONE_EXIST);
        }

        User user = User.builder()
                .username(request.getUsername())
                .password(PasswordUtil.encode(request.getPassword()))
                .phone(request.getPhone())
                .role(User.Role.USER)
                .build();

        User savedUser = userRepository.save(user);
        return UserMapper.toDTO(savedUser);
    }

    @Override
    @Transactional
    public LoginResponse login(LoginRequest request, String ip) {
        String username = request.getAccount();
        String captchaKey = request.getCaptchaKey();
        String captchaText = request.getCaptcha();
           // 验证验证码
        verifyCaptcha(captchaKey, captchaText);

        // 检查账号是否被锁定
        if (loginAttemptService.isExceeded(username, ip)) {
            throw new BusinessException(ResultCode.ACCOUNT_LOCKED);
        }

        // 查找用户
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ResultCode.USER_NOT_FOUND));

        // 密码校验
        if (!PasswordUtil.matches(request.getPassword(), user.getPassword())) {
            loginAttemptService.incrementAttempts(username, ip);
            throw new BusinessException(ResultCode.PASSWORD_ERROR);
        }

        // 登录成功，清除失败记录
        loginAttemptService.resetAttempts(username, ip);

        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        return LoginResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .phone(user.getPhone())
                .token(token)
                .role(user.getRole().name())
                .build();
    }

    @Override
    @Cacheable(value = "users", key = "'all:' + #pageable.pageNumber + '-' + #pageable.pageSize")
    public PageResponse<UserDTO> getAllUsers(Pageable pageable) {
        Page<UserDTO> page = userRepository.findAll(pageable).map(UserMapper::toDTO);
        return PageResponse.from(page);
    }

    @Override
    @Cacheable(value = "users", key = "'id:' + #id")
    public UserDTO getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND));
        return UserMapper.toDTO(user);
    }

    @Override
    @Cacheable(value = "users", key = "'username:' + #username")
    public UserDTO getUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND));
        return UserMapper.toDTO(user);
    }

    @Override
    @Cacheable(value = "users", key = "'phone:' + #phone")
    public UserDTO getUserByPhone(String phone) {
        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND));
        return UserMapper.toDTO(user);
    }

    @Override
    @Transactional
    @CacheEvict(value = "users", allEntries = true)
    public void deleteUser(Long id) {
    User user = userRepository.findById(id)
            .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND));
    // 新增：管理员不能被删除
    if (user.getRole() == User.Role.ADMIN) {
        throw new BusinessException(ResultCode.ADMIN_CANNOT_DELETE);
    }

    // 新增：先保存到归档表
    UserArchive archive = UserArchive.fromUser(user);
    userArchiveRepository.save(archive);

    // 再删除原记录
    userRepository.delete(user);
}
    //添加恢复的方法
    @Override
    @Transactional
    @CacheEvict(value = "users", allEntries = true)
    public UserDTO restoreUser(Long id) {
        UserArchive archive = userArchiveRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND));
        archive.markAsRestored();
        userArchiveRepository.save(archive);

        User user = User.builder()
                .username(archive.getUsername())
                .password(archive.getPassword())
                .email(archive.getEmail())
                .phone(archive.getPhone())
                .build();
        User restored = userRepository.save(user);
        return UserMapper.toDTO(restored);
    }
    @Override
    public PageResponse<UserDTO> getDeletedUsers(Pageable pageable) {
        Page<UserDTO> page = userArchiveRepository.findByStatusOrderByDeleteTimeDesc("DELETED", pageable)
                .map(archive -> UserMapper.toDTO(archive.toUser()));
        return PageResponse.from(page);
    }
    //添加删除已经删除的用户的方法
    @Override
    @Transactional
    @CacheEvict(value = "users", allEntries = true)
    public void cleanExpiredUsers(int retentionDays) {
        LocalDateTime threshold = LocalDateTime.now().minusDays(retentionDays);
        userArchiveRepository.deleteByDeleteTimeBefore(threshold);
    }

    @Override
    @Transactional
    @CacheEvict(value = "users", allEntries = true)
    public UserDTO updateUser(Long id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND));

        if (request.getUsername() != null) {
            user.setUsername(request.getUsername());
        }
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getRole() != null) {
            user.setRole(User.Role.valueOf(request.getRole()));
        }

        User updatedUser = userRepository.save(user);
        return UserMapper.toDTO(updatedUser);
    }
    @Override
    //生成验证码
    public Map<String, String> generateCaptcha(){
        //生成校验码
        SpecCaptcha captcha = new SpecCaptcha(130, 48, 4);//验证码宽度、高度、字符数
        String captchaKey = UUID.randomUUID().toString();//生成验证码key
        String text = captcha.text().toLowerCase();//获取验证码文本并转换为小写
        //将验证码文本缓存到redis
        redisTemplate.opsForValue().set("captcha:" + captchaKey, text, 60, TimeUnit.SECONDS);//缓存60秒
        return Map.of("captchaKey", captchaKey, "image", captcha.toBase64());//返回验证码key和base64编码的验证码图片

    }
    @Override
    @Transactional
    @CacheEvict(value = "users", allEntries = true)
    public void changePassword(ChangePasswordRequest request) { 
    // 1. 取当前登录用户
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    String username = auth.getName();

    // 2. 查询用户
    User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new BusinessException(ResultCode.USER_NOT_FOUND));

    // 3. 校验新密码与确认密码一致
    if (!request.getNewPassword().equals(request.getConfirmPassword())) {
        throw new BusinessException(ResultCode.BAD_REQUEST);
    }

    // 4. 校验原密码
    if (!PasswordUtil.matches(request.getOldPassword(), user.getPassword())) {
        throw new BusinessException(ResultCode.PASSWORD_ERROR);
    }

    // 5. 加密并保存新密码
    user.setPassword(PasswordUtil.encode(request.getNewPassword()));
    userRepository.save(user);
    }
    @Override
    @Transactional
    @CacheEvict(value = "users", allEntries = true)
    public void resetPassword(Long userId, ResetPasswordRequest request) {
    // 1. 查询目标用户
    User user = userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND));

    // 2. 加密并设置新密码
    user.setPassword(PasswordUtil.encode(request.getNewPassword()));
    userRepository.save(user);
    }


    //校验验证码（Lua 脚本原子操作，防止并发重复使用）
    @Override
    public void verifyCaptcha(String captchaKey, String captchaText) {
        String normalized = captchaText.replaceAll("\\s+", "").toLowerCase();
        Long result = redisTemplate.execute(
                VERIFY_CAPTCHA_SCRIPT,
                Collections.singletonList("captcha:" + captchaKey),
                normalized);
        if (result == null) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR);
        }
        if (result == -1) {
            throw new BusinessException(ResultCode.CAPTCHA_EXPIRED);
        }
        if (result == 0) {
            throw new BusinessException(ResultCode.CAPTCHA_ERROR);
        }
        // result == 1，校验成功且已原子删除
    }

    public boolean isOwnUser(Long userId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        
        Object principal = auth.getPrincipal();
        if (!(principal instanceof UserDetails)) {
            return false;
        }
        
        String username = ((UserDetails) principal).getUsername();
        return userRepository.findById(userId)
                .map(user -> user.getUsername().equals(username))
                .orElse(false);
    }

}
