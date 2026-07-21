# Mini Web Infrastructure Pipeline

A high-performance, distributed-ready web infrastructure pipeline in Java (Spring Boot) simulating a modern edge-to-backend request architecture:

```
Client -> CDN (Cache) -> Rate Limiter -> Load Balancer -> Backend Servers
```

## Features & Milestones

1. **Dummy Backend Servers**: Spring Boot backend instances serving port-identified responses.
2. **Load Balancer**: Custom reverse proxy supporting round-robin load balancing strategy.
3. **Health Checks**: Dynamic backend health monitoring & failover.
4. **Rate Limiter**: Distributed Token Bucket rate limiting backed by Redis.
5. **CDN / Cache Layer**: In-memory TTL-based cache short-circuiting rate limiter & load balancer on cache hit.
6. **Metrics**: Real-time stats on cache hits, routing distribution, and rate-limiting blocks.
7. **Docker Compose**: End-to-end containerized setup.

## Getting Started

Instructions for running and testing individual components will be added per milestone.
