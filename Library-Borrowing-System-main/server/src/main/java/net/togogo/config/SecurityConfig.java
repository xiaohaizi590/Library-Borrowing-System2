package net.togogo.config;

import net.togogo.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import net.togogo.security.RateLimitFilter;
//安全配置类，配置Spring Security的安全策略
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RateLimitFilter rateLimitFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,RateLimitFilter rateLimitFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.rateLimitFilter = rateLimitFilter;
    }

    @Bean
    //设置白名单，只允许这两个api在无密钥的时候运行
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/users/register", "/api/users/login", "/api/users/captcha", "/api/users/captcha/verify", "/actuator/**").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(jwtAuthenticationFilter, rateLimitFilter.getClass());
              

        return http.build();
    }

    @Bean
    //使用BCryptPasswordEncoder进行密码加密
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    //配置AuthenticationManager，用于处理身份验证
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
