package com.webinfra.gateway.metrics;

import com.webinfra.gateway.loadbalancer.BackendPool;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class MetricsService {

    private final long startTimeMs = System.currentTimeMillis();
    private final BackendPool backendPool;

    private final AtomicLong cacheHits = new AtomicLong(0);
    private final AtomicLong cacheMisses = new AtomicLong(0);

    private final AtomicLong rateLimitAllowed = new AtomicLong(0);
    private final AtomicLong rateLimitBlocked = new AtomicLong(0);

    private final Map<String, AtomicLong> backendRoutingCount = new ConcurrentHashMap<>();

    public MetricsService(BackendPool backendPool) {
        this.backendPool = backendPool;
    }

    public void recordCacheHit() {
        cacheHits.incrementAndGet();
    }

    public void recordCacheMiss() {
        cacheMisses.incrementAndGet();
    }

    public void recordRateLimitAllowed() {
        rateLimitAllowed.incrementAndGet();
    }

    public void recordRateLimitBlocked() {
        rateLimitBlocked.incrementAndGet();
    }

    public void recordBackendRouting(String backendUrl) {
        backendRoutingCount.computeIfAbsent(backendUrl, k -> new AtomicLong(0)).incrementAndGet();
    }

    public Map<String, Object> getMetricsSnapshot(int currentCacheSize) {
        long uptimeSeconds = (System.currentTimeMillis() - startTimeMs) / 1000;

        long hits = cacheHits.get();
        long misses = cacheMisses.get();
        long totalCdnRequests = hits + misses;
        double hitRatioPercentage = totalCdnRequests > 0 ? (hits * 100.0 / totalCdnRequests) : 0.0;

        long allowed = rateLimitAllowed.get();
        long blocked = rateLimitBlocked.get();
        long totalRateLimitRequests = allowed + blocked;
        double blockRatePercentage = totalRateLimitRequests > 0 ? (blocked * 100.0 / totalRateLimitRequests) : 0.0;

        Map<String, Long> distribution = new HashMap<>();
        long totalProxied = 0;
        for (Map.Entry<String, AtomicLong> entry : backendRoutingCount.entrySet()) {
            long count = entry.getValue().get();
            distribution.put(entry.getKey(), count);
            totalProxied += count;
        }

        List<String> healthyBackends = backendPool.getHealthyBackends();

        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("timestamp", Instant.now().toString());
        snapshot.put("uptimeSeconds", uptimeSeconds);
        snapshot.put("totalRequests", totalCdnRequests);

        Map<String, Object> cdnMetrics = new LinkedHashMap<>();
        cdnMetrics.put("hits", hits);
        cdnMetrics.put("misses", misses);
        cdnMetrics.put("size", currentCacheSize);
        cdnMetrics.put("hitRatioPercentage", Math.round(hitRatioPercentage * 100.0) / 100.0);
        snapshot.put("cdn", cdnMetrics);

        Map<String, Object> rateLimiterMetrics = new LinkedHashMap<>();
        rateLimiterMetrics.put("allowed", allowed);
        rateLimiterMetrics.put("blocked", blocked);
        rateLimiterMetrics.put("blockRatePercentage", Math.round(blockRatePercentage * 100.0) / 100.0);
        snapshot.put("rateLimiter", rateLimiterMetrics);

        Map<String, Object> loadBalancerMetrics = new LinkedHashMap<>();
        loadBalancerMetrics.put("totalProxied", totalProxied);
        loadBalancerMetrics.put("healthyBackendCount", healthyBackends.size());
        loadBalancerMetrics.put("healthyBackends", healthyBackends);
        loadBalancerMetrics.put("backendDistribution", distribution);
        snapshot.put("loadBalancer", loadBalancerMetrics);

        return snapshot;
    }
}
