package com.webinfra.backend.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class BackendController {

    @Value("${server.port:8081}")
    private int port;

    @GetMapping("/hello")
    public Map<String, Object> hello() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Hello from backend server!");
        response.put("port", port);
        response.put("timestamp", Instant.now().toString());
        return response;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP", "port", String.valueOf(port));
    }
}
