package net.togogo.common;

// 限流异常
public class RateLimitException extends RuntimeException {
    private final int code;
    
    public RateLimitException( String message) {
        super(message);
        this.code = 429;
    }
    
    public int getCode() {
        return code;
    }
}
