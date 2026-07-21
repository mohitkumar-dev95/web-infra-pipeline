package com.webinfra.gateway.loadbalancer;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class BackendPool {

    private final List<String> allBackends = new CopyOnWriteArrayList<>(List.of(
            "http://localhost:8081",
            "http://localhost:8082",
            "http://localhost:8083"
    ));

    public List<String> getBackends() {
        return allBackends;
    }
}
