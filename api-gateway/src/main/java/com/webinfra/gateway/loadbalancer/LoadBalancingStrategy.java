package com.webinfra.gateway.loadbalancer;

import java.util.List;

public interface LoadBalancingStrategy {
    String getNextBackend(List<String> backends);
}
