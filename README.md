# Mini Web Infrastructure Pipeline

A high-performance, distributed-ready web infrastructure pipeline in Java 17 (Spring Boot) simulating a modern edge-to-backend request architecture:

```
Client -> CDN (Cache) -> Rate Limiter -> Load Balancer -> Backend Servers
                               |              |               |
                               v              v               v
                             Header        Redis          Round Robin
                            X-Cache      Token Bucket      Health Check
```

---

## Features & Milestones

1. **Dummy Backend Servers**: Spring Boot backend instances serving port-identified responses (`/api/hello`, `/api/health`).
2. **Load Balancer**: Custom reverse proxy supporting round-robin load balancing strategy.
3. **Health Checks**: Dynamic backend health monitoring & failover with automatic recovery.
4. **Rate Limiter**: Distributed Token Bucket rate limiting backed by Redis Lua scripting (`X-RateLimit-*` headers, HTTP 429 response).
5. **CDN / Cache Layer**: In-memory TTL-based cache short-circuiting rate limiter & load balancer on cache hit (`X-Cache: HIT / MISS`).
6. **Metrics**: Real-time stats REST endpoint (`GET /metrics`) reporting cache hits/misses, rate-limiting blocks, and routing distribution per backend.
7. **Docker Compose**: Complete containerized multi-service deployment setup (`docker-compose.yml`).

---

## Project Structure

```
web-infra-pipeline/
├── api-gateway/
│   ├── src/main/java/com/webinfra/gateway/
│   │   ├── cdn/              # CDN Cache Filter & Service
│   │   ├── config/           # Redis Configuration & Lua Script
│   │   ├── controller/       # Reverse Proxy Controller
│   │   ├── health/           # Background Health Checker
│   │   ├── loadbalancer/     # Round-Robin Load Balancer & Pool
│   │   ├── metrics/          # Real-time Metrics Service & Controller
│   │   └── ratelimit/        # Redis Token Bucket Rate Limiter
│   ├── src/main/resources/application.properties
│   └── Dockerfile
├── backend-server/
│   ├── src/main/java/com/webinfra/backend/
│   │   └── BackendController.java
│   └── Dockerfile
├── docker-compose.yml
└── README.md
```

---

## Prerequisites

- **Java 17** or higher
- **Maven 3.8+**
- **Redis Server** (listening on `localhost:6379`)
- **Docker & Docker Compose** (optional for containerized deployment)

---

## Quick Start (Local Execution)

### 1. Build Both Modules
```bash
cd backend-server && mvn clean package -DskipTests && cd ..
cd api-gateway && mvn clean package -DskipTests && cd ..
```

### 2. Start Backend Instances
Start 3 backend instances on ports `8081`, `8082`, and `8083`:
```bash
java -jar backend-server/target/backend-server-1.0.0-SNAPSHOT.jar --server.port=8081 &
java -jar backend-server/target/backend-server-1.0.0-SNAPSHOT.jar --server.port=8082 &
java -jar backend-server/target/backend-server-1.0.0-SNAPSHOT.jar --server.port=8083 &
```

### 3. Start API Gateway
Start the API Gateway on port `8090`:
```bash
java -jar api-gateway/target/api-gateway-1.0.0-SNAPSHOT.jar
```

---

## Quick Start (Docker Compose)

To build and run all services (Redis + 3 Backend Servers + API Gateway) in isolated containers:

```bash
# Build JARs first
cd backend-server && mvn clean package -DskipTests && cd ..
cd api-gateway && mvn clean package -DskipTests && cd ..

# Launch containers
docker-compose up --build -d
```

To stop containers:
```bash
docker-compose down
```

---

## Testing & Verification Guide

### 1. Load Balancing
Send requests to `http://localhost:8090/api/hello` to observe round-robin distribution:
```bash
curl -s http://localhost:8090/api/hello
```
*Output rotates through ports 8081, 8082, and 8083.*

### 2. CDN Caching
- **Cache MISS (1st Request)**:
  ```bash
  curl -i http://localhost:8090/api/hello
  # Response Header: X-Cache: MISS
  ```
- **Cache HIT (Subsequent Requests within 30s TTL)**:
  ```bash
  curl -i http://localhost:8090/api/hello
  # Response Header: X-Cache: HIT
  # (Short-circuits Rate Limiter and Load Balancer!)
  ```

### 3. Token Bucket Rate Limiting
Uncached endpoint requests hit the Token Bucket rate limiter (10 capacity, 2 refill/sec):
```bash
# Burst 12 requests
for i in {1..12}; do curl -i -s http://localhost:8090/api/health; done
```
*Requests 1–10 return HTTP 200 with decreasing `X-RateLimit-Remaining`. Requests 11–12 return `HTTP 429 Too Many Requests`.*

### 4. Metrics Endpoint
Query real-time metrics across all components:
```bash
curl -s http://localhost:8090/metrics
```
*Sample JSON Response:*
```json
{
  "timestamp": "2026-07-24T18:30:00Z",
  "uptimeSeconds": 120,
  "totalRequests": 25,
  "cdn": {
    "hits": 19,
    "misses": 2,
    "size": 2,
    "hitRatioPercentage": 90.48
  },
  "rateLimiter": {
    "allowed": 4,
    "blocked": 0,
    "blockRatePercentage": 0.0
  },
  "loadBalancer": {
    "totalProxied": 4,
    "healthyBackendCount": 3,
    "healthyBackends": [
      "http://localhost:8081",
      "http://localhost:8082",
      "http://localhost:8083"
    ],
    "backendDistribution": {
      "http://localhost:8081": 2,
      "http://localhost:8082": 1,
      "http://localhost:8083": 1
    }
  }
}
```
