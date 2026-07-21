package com.webinfra.gateway.loadbalancer;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RoundRobinStrategy implements LoadBalancingStrategy {

    private final AtomicInteger index = new AtomicInteger(0);

    @Override
    public String getNextBackend(List<String> backends) {
        if (backends == null || backends.isEmpty()) {
            throw new IllegalStateException("No available backend instances to route request.");
        }
        int nextIndex = Math.abs(index.getAndIncrement() % backends.size());
        return backends.get(nextIndex);
    }
}
