package com.webinfra.gateway.ratelimit;

public class RateLimitResult {
    private final boolean allowed;
    private final long remainingTokens;
    private final long capacity;
    private final double refillRate;

    public RateLimitResult(boolean allowed, long remainingTokens, long capacity, double refillRate) {
        this.allowed = allowed;
        this.remainingTokens = remainingTokens;
        this.capacity = capacity;
        this.refillRate = refillRate;
    }

    public boolean isAllowed() {
        return allowed;
    }

    public long getRemainingTokens() {
        return remainingTokens;
    }

    public long getCapacity() {
        return capacity;
    }

    public double getRefillRate() {
        return refillRate;
    }
}
