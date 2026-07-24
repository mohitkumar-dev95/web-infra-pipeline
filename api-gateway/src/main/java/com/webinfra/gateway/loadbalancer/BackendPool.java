package com.webinfra.gateway.loadbalancer;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class BackendPool {

    private final List<String> allBackends = List.of(
            "http://localhost:8081",
            "http://localhost:8082",
            "http://localhost:8083"
    );

    private final Set<String> healthyBackends = ConcurrentHashMap.newKeySet();

    public BackendPool() {
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
