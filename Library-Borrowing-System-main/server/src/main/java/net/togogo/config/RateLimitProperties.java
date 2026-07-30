package net.togogo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import lombok.Data;
import java.util.HashMap;
import java.util.Map;

// 限流配置类, 配置令牌桶的容量和充填时间
@Data
@Component
@ConfigurationProperties(prefix = "rate-limit")
public class RateLimitProperties {
    //编写令牌桶
    private Bucket global = new Bucket(100, 60);
    //编写令牌桶
    private Bucket user = new Bucket(200, 60);
    private Map<String, Bucket> endpoints = new HashMap<>();

    @Data
    public static class Bucket {
        private int capacity;
        private int refillSeconds;
        
        public Bucket() {}
        
        public Bucket(int capacity, int refillSeconds) {
            this.capacity = capacity;
            this.refillSeconds = refillSeconds;
        }
    }

    
}
