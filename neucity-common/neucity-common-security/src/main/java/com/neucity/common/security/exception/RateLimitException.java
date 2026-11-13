package com.neucity.common.security.exception;

/**
 * 限流异常
 *
 * @author ShuPF
 * @date 2025/10/5 13:48
 */
public class RateLimitException extends RuntimeException {

    private final String key;
    private final double qps;

    public RateLimitException(String message) {
        super(message);
        this.key = "";
        this.qps = 0;
    }

    public RateLimitException(String message, String key, double qps) {
        super(message);
        this.key = key;
        this.qps = qps;
    }

    public String getKey() {
        return key;
    }

    public double getQps() {
        return qps;
    }

}
