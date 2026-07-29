# Docker Local Deployment Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Containerize 5 Spring Boot microservices with Docker Compose for local deployment, keeping MongoDB and Kafka on the host.

**Architecture:** Each microservice gets a multi-stage Dockerfile (Maven build + JRE runtime). A root `docker-compose.yml` orchestrates all services with environment variable overrides for MongoDB/Kafka/Eureka URLs via `host.docker.internal`.

**Tech Stack:** Docker, Docker Compose, Maven 3.9, Eclipse Temurin 17

## Global Constraints

- Java 17, Spring Boot 3.4.4
- Maven multi-module project (parent POM + 6 modules, `common` is a library)
- All `application*.yml` files are gitignored (`**/application*.yml` in .gitignore)
- MongoDB on host: `host.docker.internal:27017`
- Kafka on host: `host.docker.internal:9092`
- Eureka server at container name `eureka-server:8761`
- All Dockerfiles stored at project root (not in module directories)

---

### Task 1: Create .dockerignore and Dockerfiles

**Files:**
- Create: `.dockerignore`
- Create: `Dockerfile.eureka-server`
- Create: `Dockerfile.api-gateway`
- Create: `Dockerfile.user-service`
- Create: `Dockerfile.product-service`
- Create: `Dockerfile.media-service`

**Pattern for all Dockerfiles (same structure, differing module name and port):**

| Dockerfile | Module | Port | Needs `common` | JAR path glob |
|---|---|---|---|---|
| `Dockerfile.eureka-server` | `eureka-server` | 8761 | No | `eureka-server/target/eureka-server-*.jar` |
| `Dockerfile.api-gateway` | `api-gateway` | 9000 | Yes | `api-gateway/target/api-gateway-*.jar` |
| `Dockerfile.user-service` | `user-service` | 8081 | Yes | `user-service/target/user-service-*.jar` |
| `Dockerfile.product-service` | `product-service` | 8082 | Yes | `product-service/target/product-service-*.jar` |
| `Dockerfile.media-service` | `media-service` | 8083 | Yes | `media-service/target/media-service-*.jar` |

- [ ] **Step 1: Create `.dockerignore`**

```
target/
.git/
.gitattributes
.gitignore
*.iml
.idea/
.idea
*.md
HELP.md
```

- [ ] **Step 2: Create `Dockerfile.eureka-server`**

```dockerfile
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY . .
RUN mvn package -pl eureka-server -am -DskipTests

FROM eclipse-temurin:17-jre
COPY --from=build /app/eureka-server/target/eureka-server-*.jar app.jar
EXPOSE 8761
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

- [ ] **Step 3: Create `Dockerfile.api-gateway`**

```dockerfile
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY . .
RUN mvn package -pl api-gateway -am -DskipTests

FROM eclipse-temurin:17-jre
COPY --from=build /app/api-gateway/target/api-gateway-*.jar app.jar
EXPOSE 9000
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

- [ ] **Step 4: Create `Dockerfile.user-service`**

```dockerfile
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY . .
RUN mvn package -pl user-service -am -DskipTests

FROM eclipse-temurin:17-jre
COPY --from=build /app/user-service/target/user-service-*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

- [ ] **Step 5: Create `Dockerfile.product-service`**

```dockerfile
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY . .
RUN mvn package -pl product-service -am -DskipTests

FROM eclipse-temurin:17-jre
COPY --from=build /app/product-service/target/product-service-*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

- [ ] **Step 6: Create `Dockerfile.media-service`**

```dockerfile
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY . .
RUN mvn package -pl media-service -am -DskipTests

FROM eclipse-temurin:17-jre
COPY --from=build /app/media-service/target/media-service-*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

- [ ] **Step 7: Verify Dockerfiles exist**

Run: `Get-ChildItem -Path . -Filter "Dockerfile.*" | Select-Object Name`
Expected: 5 Dockerfiles and .dockerignore listed

---

### Task 2: Create docker-compose.yml

**Files:**
- Create: `docker-compose.yml`

- [ ] **Step 1: Create `docker-compose.yml`**

```yaml
services:
  eureka-server:
    build:
      context: .
      dockerfile: Dockerfile.eureka-server
    ports:
      - "8761:8761"

  api-gateway:
    build:
      context: .
      dockerfile: Dockerfile.api-gateway
    ports:
      - "9000:9000"
    environment:
      EUREKA_CLIENT_SERVICEURL_DEFAULTZONE: http://eureka-server:8761/eureka/
    depends_on:
      - eureka-server

  user-service:
    build:
      context: .
      dockerfile: Dockerfile.user-service
    ports:
      - "8081:8081"
    environment:
      EUREKA_CLIENT_SERVICEURL_DEFAULTZONE: http://eureka-server:8761/eureka/
      SPRING_DATA_MONGODB_URI: mongodb://host.docker.internal:27017/users_db
      SPRING_KAFKA_BOOTSTRAP_SERVERS: host.docker.internal:9092
    depends_on:
      - eureka-server

  product-service:
    build:
      context: .
      dockerfile: Dockerfile.product-service
    ports:
      - "8082:8082"
    environment:
      EUREKA_CLIENT_SERVICEURL_DEFAULTZONE: http://eureka-server:8761/eureka/
      SPRING_DATA_MONGODB_URI: mongodb://host.docker.internal:27017/products_db
      SPRING_KAFKA_BOOTSTRAP_SERVERS: host.docker.internal:9092
    depends_on:
      - eureka-server

  media-service:
    build:
      context: .
      dockerfile: Dockerfile.media-service
    ports:
      - "8083:8083"
    environment:
      EUREKA_CLIENT_SERVICEURL_DEFAULTZONE: http://eureka-server:8761/eureka/
      SPRING_DATA_MONGODB_URI: mongodb://host.docker.internal:27017/media_db
      SPRING_KAFKA_BOOTSTRAP_SERVERS: host.docker.internal:9092
    volumes:
      - media-storage:/app/media-storage
    depends_on:
      - eureka-server

volumes:
  media-storage:
```

- [ ] **Step 2: Validate docker-compose config**

Run: `docker compose config`
Expected: No errors, valid compose file printed

---

### Task 3: Build and verify all services

**Prerequisites:** Docker Desktop running, MongoDB and Kafka running on host

- [ ] **Step 1: Build all images**

Run: `docker compose build`
Expected: Each service builds successfully (5 images created)

- [ ] **Step 2: Start services**

Run: `docker compose up -d`
Expected: All 5 containers start

- [ ] **Step 3: Verify Eureka server is healthy**

Run: `docker compose ps`
Expected: All services "Up" status

Run: `curl -s http://localhost:8761/eureka/apps | Select-Xml -XPath "//application"` (or open in browser)
Expected: Eureka dashboard shows registered services

- [ ] **Step 4: Verify a service endpoint**

Run: `curl -s http://localhost:9000/api/users/health` (or whatever health endpoint exists)
Expected: Response from API gateway

- [ ] **Step 5: Stop services**

Run: `docker compose down`
Expected: All containers stopped and removed
