package net.togogo.security;

import lombok.RequiredArgsConstructor;
import net.togogo.common.RateLimitException;
import net.togogo.config.RateLimitProperties;
import net.togogo.service.TokenBucketService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {
    
    private final TokenBucketService tokenBucketService;
    private final RateLimitProperties rateLimitProperties;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {
        
        String method = request.getMethod();
        String uri = request.getRequestURI();

        if ("OPTIONS".equalsIgnoreCase(method)) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIP = getClientIP(request);

        // 第一层：IP全局限流
        if (!tokenBucketService.tryAcquire("ip:" + clientIP,
                rateLimitProperties.getGlobal().getCapacity(),
                rateLimitProperties.getGlobal().getRefillSeconds())) {
            throw new RateLimitException("IP请求过于频繁");
        }

        // 第二层：接口限流
        String endpointKey = getEndpointKey(uri);
        if (endpointKey != null && rateLimitProperties.getEndpoints().containsKey(endpointKey)) {
            RateLimitProperties.Bucket bucket = rateLimitProperties.getEndpoints().get(endpointKey);
            if (!tokenBucketService.tryAcquire("endpoint:" + endpointKey + ":" + clientIP,
                    bucket.getCapacity(),
                    bucket.getRefillSeconds())) {
                throw new RateLimitException("该接口请求过于频繁");
            }
        }

        // 第三层：用户级限流
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() != null) {
            try {
                UserDetails userDetails = (UserDetails) auth.getPrincipal();
                String username = userDetails.getUsername();
                if (!tokenBucketService.tryAcquire("user:" + username,
                        rateLimitProperties.getUser().getCapacity(),
                        rateLimitProperties.getUser().getRefillSeconds())) {
                    throw new RateLimitException("用户请求过于频繁");
                }
            } catch (RateLimitException e) {
                throw e;  // 限流异常直接抛出
            } catch (Exception ignored) {
                // 其他异常忽略，不影响请求
            }
        }

        filterChain.doFilter(request, response);
    }

    private String getClientIP(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {  // 修复：ip!=null
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    private String getEndpointKey(String uri) {
        if (uri.contains("/api/users/login")) return "login";
        if (uri.contains("/api/users/register")) return "register";
        if (uri.contains("/api/users/captcha")) return "captcha";
        return null;
    }
}