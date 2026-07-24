package com.webinfra.gateway.ratelimit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TokenBucketRateLimiter {

    private final StringRedisTemplate redisTemplate;
    private final RedisScript<List> rateLimiterScript;

    @Value("${rate-limiter.capacity:10}")
    private long capacity;

    @Value("${rate-limiter.refill-rate:2.0}")
    private double refillRate;

    public TokenBucketRateLimiter(StringRedisTemplate redisTemplate, RedisScript<List> rateLimiterScript) {
        this.redisTemplate = redisTemplate;
        this.rateLimiterScript = rateLimiterScript;
    }

    public RateLimitResult tryConsume(String clientKey, int requestedTokens) {
        String redisKey = "rate_limit:" + clientKey;
        double currentTimeSeconds = System.currentTimeMillis() / 1000.0;

        List<Object> result = redisTemplate.execute(
                rateLimiterScript,
                List.of(redisKey),
                String.valueOf(capacity),
                String.valueOf(refillRate),
                String.valueOf(currentTimeSeconds),
                String.valueOf(requestedTokens)
        );

        if (result != null && result.size() >= 2) {
            long allowedVal = Long.parseLong(String.valueOf(result.get(0)));
            double tokensVal = Double.parseDouble(String.valueOf(result.get(1)));
            long remaining = (long) Math.floor(Math.max(0, tokensVal));
            return new RateLimitResult(allowedVal == 1, remaining, capacity, refillRate);
        }

        // Fallback: allow request if Redis check returns unexpected output
        return new RateLimitResult(true, capacity, capacity, refillRate);
    }

    public long getCapacity() {
        return capacity;
    }

    public double getRefillRate() {
        return refillRate;
    }
}
