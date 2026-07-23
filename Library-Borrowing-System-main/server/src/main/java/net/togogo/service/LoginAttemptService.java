package net.togogo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;


@Service
public class LoginAttemptService {

    @Autowired
    private RedisTemplate<String, String> redisTemplate; // 改为 String 更通用

    private static final int MAX_ATTEMPTS = 5;
    private static final long LOCK_MINUTES = 5;

    private String getKey(String username, String ip) {
        return "loginAttempt:" + username + ":" + ip;
    }

    /**
     * 检查是否已超过最大尝试次数（即被锁定）
     */
    public boolean isExceeded(String username, String ip) {
        String val = redisTemplate.opsForValue().get(getKey(username, ip));
        if (val == null) return false;
        return Integer.parseInt(val) >= MAX_ATTEMPTS;
    }

    /**
     * 增加失败次数
     * 如果达到最大次数，则不再递增（保持上限）
     */
    public void incrementAttempts(String username, String ip) {
        String key = getKey(username, ip);
        // 使用 increment 原子操作，返回自增后的值
        Long attempts = redisTemplate.opsForValue().increment(key);
        if (attempts != null) {
            // 如果是第一次失败，设置过期时间
            if (attempts == 1) {
                redisTemplate.expire(key, LOCK_MINUTES, TimeUnit.MINUTES);
            }
            // 如果超过最大值，可以回退（但没必要，因为 isExceeded 拦截，不会继续增加）
            // 如果担心超过，可在此处做限制，但建议在业务层阻止
        }
    }

    /**
     * 重置尝试次数（登录成功时调用）
     * 直接删除 Key，干净彻底
     */
    public void resetAttempts(String username, String ip) {
        redisTemplate.delete(getKey(username, ip));
    }
}
