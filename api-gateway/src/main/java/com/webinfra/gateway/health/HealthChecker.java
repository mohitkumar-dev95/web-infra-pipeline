package com.webinfra.gateway.health;

import com.webinfra.gateway.loadbalancer.BackendPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class HealthChecker {

    private static final Logger log = LoggerFactory.getLogger(HealthChecker.class);

    private final BackendPool backendPool;
    private final RestTemplate restTemplate;

    public HealthChecker(BackendPool backendPool) {
        this.backendPool = backendPool;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(2000);
        this.restTemplate = new RestTemplate(factory);
    }

    @Scheduled(fixedRate = 5000)
    public void checkBackendHealth() {
        for (String backendUrl : backendPool.getAllBackends()) {
            boolean wasHealthy = backendPool.isHealthy(backendUrl);
            boolean isHealthyNow = pingBackend(backendUrl);

            if (isHealthyNow && !wasHealthy) {
                log.info("Backend [{}] recovered! Marking UP.", backendUrl);
                backendPool.markHealthy(backendUrl);
            } else if (!isHealthyNow && wasHealthy) {
                log.warn("Backend [{}] failed health check! Marking DOWN.", backendUrl);
                backendPool.markUnhealthy(backendUrl);
            }
        }
    }

    private boolean pingBackend(String backendUrl) {
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(backendUrl + "/api/health", String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            return false;
        }
    }
}
