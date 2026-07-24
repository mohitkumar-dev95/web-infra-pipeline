package com.webinfra.gateway.cdn;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class CdnCacheService {

    private static final Logger log = LoggerFactory.getLogger(CdnCacheService.class);

    @Value("${cdn.cache.enabled:true}")
    private boolean enabled;

    @Value("${cdn.cache.ttl-seconds:30}")
    private long ttlSeconds;

    @Value("${cdn.cache.max-entries:1000}")
    private int maxEntries;

    private final Map<String, CachedResponse> cacheMap = new ConcurrentHashMap<>();
    private final AtomicLong hitCount = new AtomicLong(0);
    private final AtomicLong missCount = new AtomicLong(0);

    private final com.webinfra.gateway.metrics.MetricsService metricsService;

    public CdnCacheService(com.webinfra.gateway.metrics.MetricsService metricsService) {
        this.metricsService = metricsService;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public CachedResponse get(String key) {
        if (!enabled) {
            return null;
        }

        CachedResponse cached = cacheMap.get(key);
        if (cached != null) {
            if (cached.isExpired()) {
                cacheMap.remove(key);
                missCount.incrementAndGet();
                metricsService.recordCacheMiss();
                return null;
            }
            hitCount.incrementAndGet();
            metricsService.recordCacheHit();
            log.info("CDN CACHE HIT for key: [{}]", key);
            return cached;
        }

        missCount.incrementAndGet();
        metricsService.recordCacheMiss();
        log.info("CDN CACHE MISS for key: [{}]", key);
        return null;
    }

    public void put(String key, int status, HttpHeaders headers, byte[] body) {
        if (!enabled) {
            return;
        }

        if (cacheMap.size() >= maxEntries) {
            evictExpired();
            if (cacheMap.size() >= maxEntries) {
                cacheMap.clear(); // Safety fallback if max capacity reached
            }
        }

        cacheMap.put(key, new CachedResponse(status, headers, body, ttlSeconds));
        log.info("CDN CACHED response for key: [{}] with TTL {}s", key, ttlSeconds);
    }

    @Scheduled(fixedRate = 10000)
    public void evictExpired() {
        int initialSize = cacheMap.size();
        cacheMap.entrySet().removeIf(entry -> entry.getValue().isExpired());
        int evicted = initialSize - cacheMap.size();
        if (evicted > 0) {
            log.info("Evicted {} expired entries from CDN cache", evicted);
        }
    }

    public void clear() {
        cacheMap.clear();
        log.info("CDN Cache cleared manually");
    }

    public long getHitCount() {
        return hitCount.get();
    }

    public long getMissCount() {
        return missCount.get();
    }

    public int getCacheSize() {
        return cacheMap.size();
    }
}
