package net.togogo.service;

import lombok.RequiredArgsConstructor;
import net.togogo.config.RateLimitProperties;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class TokenBucketService {
    private final RateLimitProperties rateLimitProperties;// 限流配置类
    private final RedisTemplate<String, String> redisTemplate;// Redis模板

    private static final String PREFIX = "token:bucket:";// 令牌桶的key前缀

    public boolean tryAcquire (String key) {
       RateLimitProperties.Bucket bucket = rateLimitProperties.getEndpoints().get(key);
       if (bucket == null) {
           bucket = rateLimitProperties.getGlobal();
       }
       //调用重载的 tryAcquire 方法，将具体限流参数（容量和补充秒数）传入，并返回其执行结果
         return tryAcquire(key, bucket.getCapacity(), bucket.getRefillSeconds());
    }
    public boolean tryAcquire (String key, int capacity, int refillSeconds) {
        String redisKey = PREFIX + key;
        //拼接前缀和业务 key，生成最终存储于 Redis 中的键名
        Long current = redisTemplate.opsForValue().increment(redisKey);
        if (current == 1) {
            redisTemplate.expire(redisKey, refillSeconds, TimeUnit.SECONDS);
        }
     return current <= capacity;
    }
}
