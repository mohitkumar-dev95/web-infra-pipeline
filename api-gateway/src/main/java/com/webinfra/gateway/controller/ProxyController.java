package com.webinfra.gateway.controller;

import com.webinfra.gateway.loadbalancer.BackendPool;
import com.webinfra.gateway.loadbalancer.LoadBalancingStrategy;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.Collections;

@RestController
public class ProxyController {

    private final BackendPool backendPool;
    private final LoadBalancingStrategy loadBalancingStrategy;
    private final RestTemplate restTemplate;
    private final com.webinfra.gateway.metrics.MetricsService metricsService;

    public ProxyController(BackendPool backendPool,
                           LoadBalancingStrategy loadBalancingStrategy,
                           com.webinfra.gateway.metrics.MetricsService metricsService) {
        this.backendPool = backendPool;
        this.loadBalancingStrategy = loadBalancingStrategy;
        this.metricsService = metricsService;
        this.restTemplate = new RestTemplate();
    }

    @RequestMapping("/api/**")
    public ResponseEntity<byte[]> proxyRequest(HttpServletRequest request, @RequestBody(required = false) byte[] body) {
        String backendUrl = loadBalancingStrategy.getNextBackend(backendPool.getHealthyBackends());
        metricsService.recordBackendRouting(backendUrl);
        String targetUri = backendUrl + request.getRequestURI();
        if (request.getQueryString() != null) {
            targetUri += "?" + request.getQueryString();
        }

        HttpHeaders headers = new HttpHeaders();
        Collections.list(request.getHeaderNames()).forEach(headerName -> {
            if (!headerName.equalsIgnoreCase("host")) {
                headers.addAll(headerName, Collections.list(request.getHeaders(headerName)));
            }
        });

        HttpMethod method = HttpMethod.valueOf(request.getMethod());
        HttpEntity<byte[]> httpEntity = new HttpEntity<>(body, headers);

        try {
            return restTemplate.exchange(URI.create(targetUri), method, httpEntity, byte[].class);
        } catch (HttpStatusCodeException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .headers(e.getResponseHeaders())
                    .body(e.getResponseBodyAsByteArray());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(("Proxy error: " + e.getMessage()).getBytes());
        }
    }
}
