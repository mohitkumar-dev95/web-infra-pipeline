package com.webinfra.gateway.loadbalancer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class BackendPool {

    private final List<String> allBackends;
    private final Set<String> healthyBackends = ConcurrentHashMap.newKeySet();

    public BackendPool(@Value("${gateway.backends:http://localhost:8081,http://localhost:8082,http://localhost:8083}") String backendsConfig) {
        this.allBackends = Arrays.stream(backendsConfig.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        healthyBackends.addAll(allBackends);
    }

    public List<String> getAllBackends() {
        return allBackends;
    }

    public List<String> getHealthyBackends() {
        return new CopyOnWriteArrayList<>(
                allBackends.stream()
                        .filter(healthyBackends::contains)
                        .toList()
        );
    }

    public void markHealthy(String backend) {
        healthyBackends.add(backend);
    }

    public void markUnhealthy(String backend) {
        healthyBackends.remove(backend);
    }

    public boolean isHealthy(String backend) {
        return healthyBackends.contains(backend);
    }
}
