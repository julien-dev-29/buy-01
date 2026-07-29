# Docker Local Deployment Design

## Overview

Containerize the 5 Spring Boot microservices (eureka-server, api-gateway, user-service, product-service, media-service) using Docker and orchestrate them with Docker Compose. MongoDB and Kafka remain running on the host machine.

## Architecture

```
┌──────────────────────────────────────────────────────┐
│                   Docker Network                      │
│                                                       │
│  ┌──────────────┐    ┌──────────────┐                │
│  │ eureka-server │◄──►│  api-gateway │                │
│  │    :8761      │    │    :9000     │                │
│  └──────┬───────┘    └──────────────┘                │
│          │                                            │
│  ┌───────┴────────┬────────────┐                      │
│  │                │            │                      │
│  ▼                ▼            ▼                      │
│ ┌──────────┐ ┌──────────┐ ┌───────────┐              │
│ │   user   │ │ product  │ │   media   │              │
│ │  :8081   │ │  :8082   │ │   :8083   │              │
│ └──────────┘ └──────────┘ └───────────┘              │
│       │              │             │                   │
└───────┼──────────────┼─────────────┼───────────────────┘
        │              │             │
        ▼              ▼             ▼
   host.docker.internal:27017    host.docker.internal:9092
   ┌──────────────────┐     ┌──────────────────┐
   │     MongoDB       │     │  Kafka + ZK      │
   │   (Host)         │     │   (Host)         │
   └──────────────────┘     └──────────────────┘
```

## Dockerfiles

Each service gets a multi-stage Dockerfile at the project root:

| File | Service | Port |
|------|---------|------|
| `Dockerfile.eureka-server` | Eureka Discovery Server | 8761 |
| `Dockerfile.api-gateway` | Spring Cloud Gateway | 9000 |
| `Dockerfile.user-service` | User/Auth Service | 8081 |
| `Dockerfile.product-service` | Product Service | 8082 |
| `Dockerfile.media-service` | Media/File Service | 8083 |

**Multi-stage build pattern (all services identical structure):**
- Stage 1: `maven:3.9-eclipse-temurin-17` — build with Maven
- Stage 2: `eclipse-temurin:17-jre` — run the JAR

Each Dockerfile copies the root `pom.xml`, the `common/` module (dependency), and the specific service module. Dependencies are resolved with `mvn dependency:go-offline` for caching, then `mvn package`.

The module `common` is a library only — no Dockerfile.

## Docker Compose

File: `docker-compose.yml` at project root.

### Services

All services share:
- `network_mode: bridge` (default Docker network)
- Service discovery via container names (e.g., `http://eureka-server:8761/eureka/`)

### Environment Variable Overrides

Since `application.yml` files reference `localhost` (for MongoDB, Kafka, Eureka), Docker Compose overrides the relevant Spring properties via environment variables:

| Service | Variables |
|---------|-----------|
| eureka-server | *(none needed — runs standalone)* |
| api-gateway | `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE: http://eureka-server:8761/eureka/` |
| user-service | `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE: http://eureka-server:8761/eureka/` |
| | `SPRING_DATA_MONGODB_URI: mongodb://host.docker.internal:27017/users_db` |
| | `SPRING_KAFKA_BOOTSTRAP_SERVERS: host.docker.internal:9092` |
| product-service | `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE: http://eureka-server:8761/eureka/` |
| | `SPRING_DATA_MONGODB_URI: mongodb://host.docker.internal:27017/products_db` |
| | `SPRING_KAFKA_BOOTSTRAP_SERVERS: host.docker.internal:9092` |
| media-service | `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE: http://eureka-server:8761/eureka/` |
| | `SPRING_DATA_MONGODB_URI: mongodb://host.docker.internal:27017/media_db` |
| | `SPRING_KAFKA_BOOTSTRAP_SERVERS: host.docker.internal:9092` |

### Port Mapping

All ports map 1:1 from container to host.

### Startup Order

- `api-gateway`, `user-service`, `product-service`, `media-service` all have `depends_on: eureka-server`
- Eureka client retry logic handles any race condition on startup
- `healthcheck` on eureka-server can be added if needed but is not required

## Files to Create

1. `Dockerfile.eureka-server`
2. `Dockerfile.api-gateway`
3. `Dockerfile.user-service`
4. `Dockerfile.product-service`
5. `Dockerfile.media-service`
6. `docker-compose.yml`
7. `.dockerignore`

## Usage

```bash
# Build and start all services
docker compose up --build

# Start in background
docker compose up --build -d

# Stop
docker compose down
```

No local JDK, Maven, or build tools required — everything runs in containers.
