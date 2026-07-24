package com.webinfra.gateway.metrics;

import com.webinfra.gateway.cdn.CdnCacheService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class MetricsController {

    private final MetricsService metricsService;
    private final CdnCacheService cdnCacheService;

    public MetricsController(MetricsService metricsService, CdnCacheService cdnCacheService) {
        this.metricsService = metricsService;
        this.cdnCacheService = cdnCacheService;
    }

    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Object>> getMetrics() {
        return ResponseEntity.ok(metricsService.getMetricsSnapshot(cdnCacheService.getCacheSize()));
    }
}
