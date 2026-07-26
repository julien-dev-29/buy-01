# Microservices Backend Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a Spring Boot microservices backend with User, Product, and Media services, Eureka discovery, Kafka messaging, and API Gateway.

**Architecture:** Multi-module Maven monorepo with shared common module. Each service has its own MongoDB database. JWT authentication via API Gateway. Kafka for async inter-service communication.

**Tech Stack:** Java 17, Spring Boot 4.1.0, Spring Cloud (Eureka, Gateway), Spring Data MongoDB, Spring Security, Spring Kafka, Maven, MongoDB, Testcontainers

## Global Constraints

- Java version: 17
- Spring Boot version: 4.1.0
- Spring Cloud version: 2024.0.0 (compatible with Spring Boot 4.1.0)
- MongoDB: Separate database per service
- JWT: Shared secret for token signing
- Kafka: Single broker localhost:9092
- Media storage: Local filesystem `./media-storage/{sellerId}/{filename}`
- Max file size: 2MB
- Allowed image types: image/jpeg, image/png, image/gif, image/webp
- Password hashing: BCrypt

---

## File Structure

```
buy-01/
├── pom.xml                          (parent POM - MODIFY)
├── common/
│   ├── pom.xml                      (CREATE)
│   └── src/main/java/com/jurol/buy01/common/
│       ├── dto/
│       │   ├── UserDTO.java         (CREATE)
│       │   ├── ProductDTO.java      (CREATE)
│       │   ├── MediaDTO.java        (CREATE)
│       │   └── AuthRequest.java     (CREATE)
│       ├── security/
│       │   └── JwtUtil.java         (CREATE)
│       └── events/
│           ├── UserRegisteredEvent.java   (CREATE)
│           ├── ProductCreatedEvent.java   (CREATE)
│           └── ProductDeletedEvent.java   (CREATE)
├── eureka-server/
│   ├── pom.xml                      (CREATE)
│   └── src/main/
│       ├── java/com/jurol/buy01/eureka/
│       │   └── EurekaServerApplication.java (CREATE)
│       └── resources/
│           └── application.yml      (CREATE)
├── user-service/
│   ├── pom.xml                      (CREATE)
│   └── src/main/java/com/jurol/buy01/user/
│       ├── UserServiceApplication.java (CREATE)
│       ├── model/
│       │   └── User.java            (CREATE)
│       ├── repository/
│       │   └── UserRepository.java  (CREATE)
│       ├── service/
│       │   └── UserService.java     (CREATE)
│       ├── controller/
│       │   ├── AuthController.java  (CREATE)
│       │   └── UserController.java  (CREATE)
│       ├── security/
│       │   └── SecurityConfig.java  (CREATE)
│       └── kafka/
│           └── UserEventProducer.java (CREATE)
├── product-service/
│   ├── pom.xml                      (CREATE)
│   └── src/main/java/com/jurol/buy01/product/
│       ├── ProductServiceApplication.java (CREATE)
│       ├── model/
│       │   └── Product.java         (CREATE)
│       ├── repository/
│       │   └── ProductRepository.java (CREATE)
│       ├── service/
│       │   └── ProductService.java  (CREATE)
│       ├── controller/
│       │   └── ProductController.java (CREATE)
│       ├── security/
│       │   └── SecurityConfig.java  (CREATE)
│       └── kafka/
│           ├── ProductEventProducer.java (CREATE)
│           └── UserEventConsumer.java (CREATE)
├── media-service/
│   ├── pom.xml                      (CREATE)
│   └── src/main/java/com/jurol/buy01/media/
│       ├── MediaServiceApplication.java (CREATE)
│       ├── model/
│       │   └── Media.java           (CREATE)
│       ├── repository/
│       │   └── MediaRepository.java (CREATE)
│       ├── service/
│       │   └── MediaService.java    (CREATE)
│       ├── controller/
│       │   └── MediaController.java (CREATE)
│       ├── security/
│       │   └── SecurityConfig.java  (CREATE)
│       ├── validation/
│       │   └── FileValidator.java   (CREATE)
│       └── kafka/
│           └── ProductEventConsumer.java (CREATE)
└── api-gateway/
    ├── pom.xml                      (CREATE)
    └── src/main/
        ├── java/com/jurol/buy01/gateway/
        │   └── ApiGatewayApplication.java (CREATE)
        ├── resources/
        │   └── application.yml      (CREATE)
        └── test/java/com/jurol/buy01/gateway/
            └── ApiGatewayApplicationTests.java (CREATE)
```

---

### Task 1: Convert to Multi-Module Maven Project

**Files:**
- Modify: `pom.xml`
- Create: `common/pom.xml`
- Create: `eureka-server/pom.xml`
- Create: `user-service/pom.xml`
- Create: `product-service/pom.xml`
- Create: `media-service/pom.xml`
- Create: `api-gateway/pom.xml`

- [ ] **Step 1: Update parent POM to multi-module**

Replace the entire `pom.xml` with:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>4.1.0</version>
        <relativePath/>
    </parent>
    <groupId>com.jurol</groupId>
    <artifactId>buy-01</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>buy-01</name>
    <description>E-Commerce Microservices Platform</description>
    <packaging>pom</packaging>

    <modules>
        <module>common</module>
        <module>eureka-server</module>
        <module>user-service</module>
        <module>product-service</module>
        <module>media-service</module>
        <module>api-gateway</module>
    </modules>

    <properties>
        <java.version>17</java.version>
        <spring-cloud.version>2024.0.0</spring-cloud.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.cloud</groupId>
                <artifactId>spring-cloud-dependencies</artifactId>
                <version>${spring-cloud.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>
</project>
```

- [ ] **Step 2: Create common module POM**

Create `common/pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.jurol</groupId>
        <artifactId>buy-01</artifactId>
        <version>0.0.1-SNAPSHOT</version>
    </parent>

    <artifactId>common</artifactId>
    <name>buy-01-common</name>

    <dependencies>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-api</artifactId>
            <version>0.12.6</version>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-impl</artifactId>
            <version>0.12.6</version>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-jackson</artifactId>
            <version>0.12.6</version>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 3: Create eureka-server module POM**

Create `eureka-server/pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.jurol</groupId>
        <artifactId>buy-01</artifactId>
        <version>0.0.1-SNAPSHOT</version>
    </parent>

    <artifactId>eureka-server</artifactId>
    <name>buy-01-eureka-server</name>

    <dependencies>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-netflix-eureka-server</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 4: Create user-service module POM**

Create `user-service/pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.jurol</groupId>
        <artifactId>buy-01</artifactId>
        <version>0.0.1-SNAPSHOT</version>
    </parent>

    <artifactId>user-service</artifactId>
    <name>buy-01-user-service</name>

    <dependencies>
        <dependency>
            <groupId>com.jurol</groupId>
            <artifactId>common</artifactId>
            <version>${project.version}</version>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-mongodb</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.kafka</groupId>
            <artifactId>spring-kafka</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.security</groupId>
            <artifactId>spring-security-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 5: Create product-service module POM**

Create `product-service/pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.jurol</groupId>
        <artifactId>buy-01</artifactId>
        <version>0.0.1-SNAPSHOT</version>
    </parent>

    <artifactId>product-service</artifactId>
    <name>buy-01-product-service</name>

    <dependencies>
        <dependency>
            <groupId>com.jurol</groupId>
            <artifactId>common</artifactId>
            <version>${project.version}</version>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-mongodb</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.kafka</groupId>
            <artifactId>spring-kafka</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.security</groupId>
            <artifactId>spring-security-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 6: Create media-service module POM**

Create `media-service/pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.jurol</groupId>
        <artifactId>buy-01</artifactId>
        <version>0.0.1-SNAPSHOT</version>
    </parent>

    <artifactId>media-service</artifactId>
    <name>buy-01-media-service</name>

    <dependencies>
        <dependency>
            <groupId>com.jurol</groupId>
            <artifactId>common</artifactId>
            <version>${project.version}</version>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-mongodb</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.kafka</groupId>
            <artifactId>spring-kafka</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.security</groupId>
            <artifactId>spring-security-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 7: Create api-gateway module POM**

Create `api-gateway/pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.jurol</groupId>
        <artifactId>buy-01</artifactId>
        <version>0.0.1-SNAPSHOT</version>
    </parent>

    <artifactId>api-gateway</artifactId>
    <name>buy-01-api-gateway</name>

    <dependencies>
        <dependency>
            <groupId>com.jurol</groupId>
            <artifactId>common</artifactId>
            <version>${project.version}</version>
        </dependency>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-gateway</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 8: Remove old single-module files**

Delete the old `src/` directory and `mvnw`, `mvnw.cmd`, `.mvn/` files since they belong to the old single-module structure:

```bash
rm -rf src/ mvnw mvnw.cmd .mvn/
```

- [ ] **Step 9: Verify build compiles**

Run: `mvn compile -pl common`
Expected: BUILD SUCCESS

- [ ] **Step 10: Commit**

```bash
git add -A
git commit -m "refactor: convert to multi-module Maven project"
```

---

### Task 2: Common Module - Shared DTOs

**Files:**
- Create: `common/src/main/java/com/jurol/buy01/common/dto/UserDTO.java`
- Create: `common/src/main/java/com/jurol/buy01/common/dto/ProductDTO.java`
- Create: `common/src/main/java/com/jurol/buy01/common/dto/MediaDTO.java`
- Create: `common/src/main/java/com/jurol/buy01/common/dto/AuthRequest.java`

- [ ] **Step 1: Create UserDTO**

Create `common/src/main/java/com/jurol/buy01/common/dto/UserDTO.java`:

```java
package com.jurol.buy01.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserDTO {
    private String id;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "First name is required")
    @Size(min = 1, max = 50)
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(min = 1, max = 50)
    private String lastName;

    private String role;
    private String avatar;
    private Instant createdAt;
    private Instant updatedAt;

    public UserDTO() {}

    public UserDTO(String id, String email, String firstName, String lastName, String role, String avatar, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.role = role;
        this.avatar = avatar;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
```

- [ ] **Step 2: Create ProductDTO**

Create `common/src/main/java/com/jurol/buy01/common/dto/ProductDTO.java`:

```java
package com.jurol.buy01.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductDTO {
    private String id;

    @NotBlank(message = "Product name is required")
    @Size(min = 1, max = 200)
    private String name;

    @Size(max = 2000)
    private String description;

    @NotNull(message = "Price is required")
    @Positive(message = "Price must be positive")
    private BigDecimal price;

    private String sellerId;
    private List<String> mediaIds;
    private Instant createdAt;
    private Instant updatedAt;

    public ProductDTO() {}

    public ProductDTO(String id, String name, String description, BigDecimal price, String sellerId, List<String> mediaIds, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.sellerId = sellerId;
        this.mediaIds = mediaIds;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public String getSellerId() { return sellerId; }
    public void setSellerId(String sellerId) { this.sellerId = sellerId; }
    public List<String> getMediaIds() { return mediaIds; }
    public void setMediaIds(List<String> mediaIds) { this.mediaIds = mediaIds; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
```

- [ ] **Step 3: Create MediaDTO**

Create `common/src/main/java/com/jurol/buy01/common/dto/MediaDTO.java`:

```java
package com.jurol.buy01.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class MediaDTO {
    private String id;
    private String filename;
    private String originalName;
    private String contentType;
    private Long size;
    private String productId;
    private String sellerId;
    private Instant createdAt;

    public MediaDTO() {}

    public MediaDTO(String id, String filename, String originalName, String contentType, Long size, String productId, String sellerId, Instant createdAt) {
        this.id = id;
        this.filename = filename;
        this.originalName = originalName;
        this.contentType = contentType;
        this.size = size;
        this.productId = productId;
        this.sellerId = sellerId;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }
    public String getOriginalName() { return originalName; }
    public void setOriginalName(String originalName) { this.originalName = originalName; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public Long getSize() { return size; }
    public void setSize(Long size) { this.size = size; }
    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
    public String getSellerId() { return sellerId; }
    public void setSellerId(String sellerId) { this.sellerId = sellerId; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
```

- [ ] **Step 4: Create AuthRequest**

Create `common/src/main/java/com/jurol/buy01/common/dto/AuthRequest.java`:

```java
package com.jurol.buy01.common.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AuthRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 100, message = "Password must be between 6 and 100 characters")
    private String password;

    private String firstName;
    private String lastName;
    private String role;

    public AuthRequest() {}

    public AuthRequest(String email, String password) {
        this.email = email;
        this.password = password;
    }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
```

- [ ] **Step 5: Verify build compiles**

Run: `mvn compile -pl common`
Expected: BUILD SUCCESS

- [ ] **Step 6: Commit**

```bash
git add common/
git commit -m "feat: add common module with shared DTOs"
```

---

### Task 3: Common Module - JWT Utility

**Files:**
- Create: `common/src/main/java/com/jurol/buy01/common/security/JwtUtil.java`

- [ ] **Step 1: Create JwtUtil**

Create `common/src/main/java/com/jurol/buy01/common/security/JwtUtil.java`:

```java
package com.jurol.buy01.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

@Component
public class JwtUtil {

    private final SecretKey key;
    private final long expirationMs;

    public JwtUtil(
            @Value("${jwt.secret:defaultSecretKeyThatIsAtLeast32BytesLong!!}") String secret,
            @Value("${jwt.expiration-ms:86400000}") long expirationMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String generateToken(String userId, String email, String role, Map<String, Object> extraClaims) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        var builder = Jwts.builder()
                .subject(userId)
                .claim("email", email)
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key);

        if (extraClaims != null) {
            extraClaims.forEach(builder::claim);
        }

        return builder.compact();
    }

    public String generateToken(String userId, String email, String role) {
        return generateToken(userId, email, role, null);
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean validateToken(String token) {
        try {
            Claims claims = parseToken(token);
            return !claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    public String getUserId(String token) {
        return parseToken(token).getSubject();
    }

    public String getEmail(String token) {
        return parseToken(token).get("email", String.class);
    }

    public String getRole(String token) {
        return parseToken(token).get("role", String.class);
    }
}
```

- [ ] **Step 2: Verify build compiles**

Run: `mvn compile -pl common`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add common/
git commit -m "feat: add JWT utility class"
```

---

### Task 4: Common Module - Kafka Events

**Files:**
- Create: `common/src/main/java/com/jurol/buy01/common/events/UserRegisteredEvent.java`
- Create: `common/src/main/java/com/jurol/buy01/common/events/ProductCreatedEvent.java`
- Create: `common/src/main/java/com/jurol/buy01/common/events/ProductDeletedEvent.java`

- [ ] **Step 1: Create UserRegisteredEvent**

Create `common/src/main/java/com/jurol/buy01/common/events/UserRegisteredEvent.java`:

```java
package com.jurol.buy01.common.events;

import java.time.Instant;

public class UserRegisteredEvent {
    private String eventType = "USER_REGISTERED";
    private String userId;
    private String email;
    private String role;
    private Instant timestamp;

    public UserRegisteredEvent() {}

    public UserRegisteredEvent(String userId, String email, String role) {
        this.userId = userId;
        this.email = email;
        this.role = role;
        this.timestamp = Instant.now();
    }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
}
```

- [ ] **Step 2: Create ProductCreatedEvent**

Create `common/src/main/java/com/jurol/buy01/common/events/ProductCreatedEvent.java`:

```java
package com.jurol.buy01.common.events;

import java.time.Instant;

public class ProductCreatedEvent {
    private String eventType = "PRODUCT_CREATED";
    private String productId;
    private String sellerId;
    private Instant timestamp;

    public ProductCreatedEvent() {}

    public ProductCreatedEvent(String productId, String sellerId) {
        this.productId = productId;
        this.sellerId = sellerId;
        this.timestamp = Instant.now();
    }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
    public String getSellerId() { return sellerId; }
    public void setSellerId(String sellerId) { this.sellerId = sellerId; }
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
}
```

- [ ] **Step 3: Create ProductDeletedEvent**

Create `common/src/main/java/com/jurol/buy01/common/events/ProductDeletedEvent.java`:

```java
package com.jurol.buy01.common.events;

import java.time.Instant;

public class ProductDeletedEvent {
    private String eventType = "PRODUCT_DELETED";
    private String productId;
    private String sellerId;
    private Instant timestamp;

    public ProductDeletedEvent() {}

    public ProductDeletedEvent(String productId, String sellerId) {
        this.productId = productId;
        this.sellerId = sellerId;
        this.timestamp = Instant.now();
    }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
    public String getSellerId() { return sellerId; }
    public void setSellerId(String sellerId) { this.sellerId = sellerId; }
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
}
```

- [ ] **Step 4: Verify build compiles**

Run: `mvn compile -pl common`
Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add common/
git commit -m "feat: add Kafka event classes"
```

---

### Task 5: Eureka Server

**Files:**
- Create: `eureka-server/src/main/java/com/jurol/buy01/eureka/EurekaServerApplication.java`
- Create: `eureka-server/src/main/resources/application.yml`
- Create: `eureka-server/src/test/java/com/jurol/buy01/eureka/EurekaServerApplicationTests.java`

- [ ] **Step 1: Create EurekaServerApplication**

Create `eureka-server/src/main/java/com/jurol/buy01/eureka/EurekaServerApplication.java`:

```java
package com.jurol.buy01.eureka;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

@SpringBootApplication
@EnableEurekaServer
public class EurekaServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(EurekaServerApplication.class, args);
    }
}
```

- [ ] **Step 2: Create application.yml**

Create `eureka-server/src/main/resources/application.yml`:

```yaml
server:
  port: 8761

eureka:
  client:
    register-with-eureka: false
    fetch-registry: false
  server:
    wait-time-in-ms-when-sync-empty: 0

spring:
  application:
    name: eureka-server
```

- [ ] **Step 3: Create test class**

Create `eureka-server/src/test/java/com/jurol/buy01/eureka/EurekaServerApplicationTests.java`:

```java
package com.jurol.buy01.eureka;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class EurekaServerApplicationTests {

    @Test
    void contextLoads() {
    }
}
```

- [ ] **Step 4: Verify build compiles**

Run: `mvn compile -pl eureka-server`
Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add eureka-server/
git commit -m "feat: add Eureka server module"
```

---

### Task 6: User Service - Model & Repository

**Files:**
- Create: `user-service/src/main/java/com/jurol/buy01/user/UserServiceApplication.java`
- Create: `user-service/src/main/java/com/jurol/buy01/user/model/User.java`
- Create: `user-service/src/main/java/com/jurol/buy01/user/repository/UserRepository.java`
- Create: `user-service/src/main/resources/application.yml`

- [ ] **Step 1: Create UserServiceApplication**

Create `user-service/src/main/java/com/jurol/buy01/user/UserServiceApplication.java`:

```java
package com.jurol.buy01.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.jurol.buy01.user", "com.jurol.buy01.common"})
public class UserServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
```

- [ ] **Step 2: Create User model**

Create `user-service/src/main/java/com/jurol/buy01/user/model/User.java`:

```java
package com.jurol.buy01.user.model;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "users")
public class User {

    @Id
    private String id;

    @Indexed(unique = true)
    private String email;

    private String password;
    private String firstName;
    private String lastName;
    private String role;
    private String avatar;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    public User() {}

    public User(String email, String password, String firstName, String lastName, String role) {
        this.email = email;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
        this.role = role;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
```

- [ ] **Step 3: Create UserRepository**

Create `user-service/src/main/java/com/jurol/buy01/user/repository/UserRepository.java`:

```java
package com.jurol.buy01.user.repository;

import com.jurol.buy01.user.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends MongoRepository<User, String> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}
```

- [ ] **Step 4: Create application.yml**

Create `user-service/src/main/resources/application.yml`:

```yaml
server:
  port: 8081

spring:
  application:
    name: user-service
  data:
    mongodb:
      uri: mongodb://localhost:27017/users_db
  kafka:
    bootstrap-servers: localhost:9092

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/

jwt:
  secret: mySecretKeyForJWTTokenGenerationThatIsAtLeast32Bytes!!
  expiration-ms: 86400000

kafka:
  topics:
    user-events: user-events
```

- [ ] **Step 5: Verify build compiles**

Run: `mvn compile -pl user-service -am`
Expected: BUILD SUCCESS

- [ ] **Step 6: Commit**

```bash
git add user-service/
git commit -m "feat: add User model and repository"
```

---

### Task 7: User Service - Security Config

**Files:**
- Create: `user-service/src/main/java/com/jurol/buy01/user/security/SecurityConfig.java`
- Create: `user-service/src/main/java/com/jurol/buy01/user/security/JwtAuthenticationFilter.java`

- [ ] **Step 1: Create JwtAuthenticationFilter**

Create `user-service/src/main/java/com/jurol/buy01/user/security/JwtAuthenticationFilter.java`:

```java
package com.jurol.buy01.user.security;

import com.jurol.buy01.common.security.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                if (jwtUtil.validateToken(token)) {
                    String userId = jwtUtil.getUserId(token);
                    String email = jwtUtil.getEmail(token);
                    String role = jwtUtil.getRole(token);

                    var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
                    var auth = new UsernamePasswordAuthenticationToken(userId, null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } catch (Exception e) {
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }
}
```

- [ ] **Step 2: Create SecurityConfig**

Create `user-service/src/main/java/com/jurol/buy01/user/security/SecurityConfig.java`:

```java
package com.jurol.buy01.user.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/users/**").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

- [ ] **Step 3: Verify build compiles**

Run: `mvn compile -pl user-service -am`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add user-service/
git commit -m "feat: add JWT security configuration for user service"
```

---

### Task 8: User Service - Service Layer

**Files:**
- Create: `user-service/src/main/java/com/jurol/buy01/user/service/UserService.java`
- Create: `user-service/src/main/java/com/jurol/buy01/user/kafka/UserEventProducer.java`

- [ ] **Step 1: Create UserService**

Create `user-service/src/main/java/com/jurol/buy01/user/service/UserService.java`:

```java
package com.jurol.buy01.user.service;

import com.jurol.buy01.common.dto.AuthRequest;
import com.jurol.buy01.common.dto.UserDTO;
import com.jurol.buy01.common.events.UserRegisteredEvent;
import com.jurol.buy01.common.security.JwtUtil;
import com.jurol.buy01.user.kafka.UserEventProducer;
import com.jurol.buy01.user.model.User;
import com.jurol.buy01.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final UserEventProducer eventProducer;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil, UserEventProducer eventProducer) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.eventProducer = eventProducer;
    }

    public UserDTO register(AuthRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered");
        }

        String role = request.getRole() != null ? request.getRole().toUpperCase() : "CLIENT";
        if (!role.equals("CLIENT") && !role.equals("SELLER")) {
            throw new RuntimeException("Invalid role. Must be CLIENT or SELLER");
        }

        User user = new User(
                request.getEmail(),
                passwordEncoder.encode(request.getPassword()),
                request.getFirstName(),
                request.getLastName(),
                role
        );

        User saved = userRepository.save(user);

        eventProducer.sendUserRegisteredEvent(
                new UserRegisteredEvent(saved.getId(), saved.getEmail(), saved.getRole())
        );

        return toDTO(saved);
    }

    public Map<String, String> login(AuthRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid email or password");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRole());
        return Map.of("token", token, "role", user.getRole());
    }

    public UserDTO getProfile(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return toDTO(user);
    }

    public UserDTO updateProfile(String userId, UserDTO dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (dto.getFirstName() != null) user.setFirstName(dto.getFirstName());
        if (dto.getLastName() != null) user.setLastName(dto.getLastName());
        if (dto.getAvatar() != null && "SELLER".equals(user.getRole())) {
            user.setAvatar(dto.getAvatar());
        }

        User saved = userRepository.save(user);
        return toDTO(saved);
    }

    private UserDTO toDTO(User user) {
        return new UserDTO(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole(),
                user.getAvatar(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
```

- [ ] **Step 2: Create UserEventProducer**

Create `user-service/src/main/java/com/jurol/buy01/user/kafka/UserEventProducer.java`:

```java
package com.jurol.buy01.user.kafka;

import com.jurol.buy01.common.events.UserRegisteredEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class UserEventProducer {

    private static final Logger log = LoggerFactory.getLogger(UserEventProducer.class);
    private final KafkaTemplate<String, UserRegisteredEvent> kafkaTemplate;
    private final String topic;

    public UserEventProducer(KafkaTemplate<String, UserRegisteredEvent> kafkaTemplate,
                             @Value("${kafka.topics.user-events}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public void sendUserRegisteredEvent(UserRegisteredEvent event) {
        log.info("Sending UserRegistered event for user: {}", event.getUserId());
        kafkaTemplate.send(topic, event.getUserId(), event);
    }
}
```

- [ ] **Step 3: Verify build compiles**

Run: `mvn compile -pl user-service -am`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add user-service/
git commit -m "feat: add UserService and Kafka event producer"
```

---

### Task 9: User Service - Controllers

**Files:**
- Create: `user-service/src/main/java/com/jurol/buy01/user/controller/AuthController.java`
- Create: `user-service/src/main/java/com/jurol/buy01/user/controller/UserController.java`
- Create: `user-service/src/main/java/com/jurol/buy01/user/controller/GlobalExceptionHandler.java`

- [ ] **Step 1: Create AuthController**

Create `user-service/src/main/java/com/jurol/buy01/user/controller/AuthController.java`:

```java
package com.jurol.buy01.user.controller;

import com.jurol.buy01.common.dto.AuthRequest;
import com.jurol.buy01.common.dto.UserDTO;
import com.jurol.buy01.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserDTO> register(@Valid @RequestBody AuthRequest request) {
        UserDTO user = userService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@Valid @RequestBody AuthRequest request) {
        Map<String, String> response = userService.login(request);
        return ResponseEntity.ok(response);
    }
}
```

- [ ] **Step 2: Create UserController**

Create `user-service/src/main/java/com/jurol/buy01/user/controller/UserController.java`:

```java
package com.jurol.buy01.user.controller;

import com.jurol.buy01.common.dto.UserDTO;
import com.jurol.buy01.user.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserDTO> getProfile(Authentication authentication) {
        String userId = (String) authentication.getPrincipal();
        UserDTO user = userService.getProfile(userId);
        return ResponseEntity.ok(user);
    }

    @PutMapping("/me")
    public ResponseEntity<UserDTO> updateProfile(Authentication authentication, @RequestBody UserDTO dto) {
        String userId = (String) authentication.getPrincipal();
        UserDTO updated = userService.updateProfile(userId, dto);
        return ResponseEntity.ok(updated);
    }
}
```

- [ ] **Step 3: Create GlobalExceptionHandler**

Create `user-service/src/main/java/com/jurol/buy01/user/controller/GlobalExceptionHandler.java`:

```java
package com.jurol.buy01.user.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex) {
        return ResponseEntity.badRequest().body(Map.of(
                "error", "BAD_REQUEST",
                "message", ex.getMessage(),
                "timestamp", Instant.now().toString()
        ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(MethodArgumentNotValidException ex) {
        String errors = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining(", "));

        return ResponseEntity.badRequest().body(Map.of(
                "error", "VALIDATION_ERROR",
                "message", errors,
                "timestamp", Instant.now().toString()
        ));
    }
}
```

- [ ] **Step 4: Verify build compiles**

Run: `mvn compile -pl user-service -am`
Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add user-service/
git commit -m "feat: add Auth and User controllers with exception handling"
```

---

### Task 10: User Service - Tests

**Files:**
- Create: `user-service/src/test/java/com/jurol/buy01/user/UserServiceApplicationTests.java`
- Create: `user-service/src/test/java/com/jurol/buy01/user/service/UserServiceTest.java`
- Create: `user-service/src/test/java/com/jurol/buy01/user/controller/AuthControllerTest.java`

- [ ] **Step 1: Create UserServiceTest**

Create `user-service/src/test/java/com/jurol/buy01/user/service/UserServiceTest.java`:

```java
package com.jurol.buy01.user.service;

import com.jurol.buy01.common.dto.AuthRequest;
import com.jurol.buy01.common.dto.UserDTO;
import com.jurol.buy01.common.events.UserRegisteredEvent;
import com.jurol.buy01.common.security.JwtUtil;
import com.jurol.buy01.user.kafka.UserEventProducer;
import com.jurol.buy01.user.model.User;
import com.jurol.buy01.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private UserEventProducer eventProducer;

    @InjectMocks
    private UserService userService;

    private User user;
    private AuthRequest registerRequest;

    @BeforeEach
    void setUp() {
        user = new User("test@example.com", "encodedPassword", "John", "Doe", "CLIENT");
        user.setId("user123");

        registerRequest = new AuthRequest("test@example.com", "password123");
        registerRequest.setFirstName("John");
        registerRequest.setLastName("Doe");
        registerRequest.setRole("CLIENT");
    }

    @Test
    void register_shouldCreateUser() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);
        doNothing().when(eventProducer).sendUserRegisteredEvent(any(UserRegisteredEvent.class));

        UserDTO result = userService.register(registerRequest);

        assertNotNull(result);
        assertEquals("test@example.com", result.getEmail());
        assertEquals("CLIENT", result.getRole());
        verify(eventProducer).sendUserRegisteredEvent(any(UserRegisteredEvent.class));
    }

    @Test
    void register_shouldThrowForDuplicateEmail() {
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        assertThrows(RuntimeException.class, () -> userService.register(registerRequest));
    }

    @Test
    void login_shouldReturnToken() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);
        when(jwtUtil.generateToken("user123", "test@example.com", "CLIENT")).thenReturn("jwt-token");

        var result = userService.login(new AuthRequest("test@example.com", "password123"));

        assertEquals("jwt-token", result.get("token"));
    }

    @Test
    void login_shouldThrowForInvalidCredentials() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongpassword", "encodedPassword")).thenReturn(false);

        assertThrows(RuntimeException.class, () ->
                userService.login(new AuthRequest("test@example.com", "wrongpassword")));
    }
}
```

- [ ] **Step 2: Create AuthControllerTest**

Create `user-service/src/test/java/com/jurol/buy01/user/controller/AuthControllerTest.java`:

```java
package com.jurol.buy01.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jurol.buy01.common.dto.AuthRequest;
import com.jurol.buy01.common.dto.UserDTO;
import com.jurol.buy01.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.bean.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void register_shouldReturnCreatedUser() throws Exception {
        AuthRequest request = new AuthRequest("test@example.com", "password123");
        request.setFirstName("John");
        request.setLastName("Doe");
        request.setRole("CLIENT");

        UserDTO userDTO = new UserDTO("user123", "test@example.com", "John", "Doe", "CLIENT", null, Instant.now(), Instant.now());

        when(userService.register(any(AuthRequest.class))).thenReturn(userDTO);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.role").value("CLIENT"));
    }

    @Test
    void login_shouldReturnToken() throws Exception {
        AuthRequest request = new AuthRequest("test@example.com", "password123");

        when(userService.login(any(AuthRequest.class))).thenReturn(Map.of("token", "jwt-token", "role", "CLIENT"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"));
    }
}
```

- [ ] **Step 3: Create UserServiceApplicationTests**

Create `user-service/src/test/java/com/jurol/buy01/user/UserServiceApplicationTests.java`:

```java
package com.jurol.buy01.user;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class UserServiceApplicationTests {

    @Test
    void contextLoads() {
    }
}
```

- [ ] **Step 4: Run tests**

Run: `mvn test -pl user-service -am`
Expected: All tests pass

- [ ] **Step 5: Commit**

```bash
git add user-service/
git commit -m "test: add user service unit and controller tests"
```

---

### Task 11: Product Service - Model & Repository

**Files:**
- Create: `product-service/src/main/java/com/jurol/buy01/product/ProductServiceApplication.java`
- Create: `product-service/src/main/java/com/jurol/buy01/product/model/Product.java`
- Create: `product-service/src/main/java/com/jurol/buy01/product/repository/ProductRepository.java`
- Create: `product-service/src/main/resources/application.yml`

- [ ] **Step 1: Create ProductServiceApplication**

Create `product-service/src/main/java/com/jurol/buy01/product/ProductServiceApplication.java`:

```java
package com.jurol.buy01.product;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.jurol.buy01.product", "com.jurol.buy01.common"})
public class ProductServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ProductServiceApplication.class, args);
    }
}
```

- [ ] **Step 2: Create Product model**

Create `product-service/src/main/java/com/jurol/buy01/product/model/Product.java`:

```java
package com.jurol.buy01.product.model;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "products")
public class Product {

    @Id
    private String id;
    private String name;
    private String description;
    private BigDecimal price;
    private String sellerId;
    private List<String> mediaIds = new ArrayList<>();

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    public Product() {}

    public Product(String name, String description, BigDecimal price, String sellerId) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.sellerId = sellerId;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public String getSellerId() { return sellerId; }
    public void setSellerId(String sellerId) { this.sellerId = sellerId; }
    public List<String> getMediaIds() { return mediaIds; }
    public void setMediaIds(List<String> mediaIds) { this.mediaIds = mediaIds; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
```

- [ ] **Step 3: Create ProductRepository**

Create `product-service/src/main/java/com/jurol/buy01/product/repository/ProductRepository.java`:

```java
package com.jurol.buy01.product.repository;

import com.jurol.buy01.product.model.Product;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends MongoRepository<Product, String> {
    List<Product> findBySellerId(String sellerId);
}
```

- [ ] **Step 4: Create application.yml**

Create `product-service/src/main/resources/application.yml`:

```yaml
server:
  port: 8082

spring:
  application:
    name: product-service
  data:
    mongodb:
      uri: mongodb://localhost:27017/products_db
  kafka:
    bootstrap-servers: localhost:9092

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/

jwt:
  secret: mySecretKeyForJWTTokenGenerationThatIsAtLeast32Bytes!!
  expiration-ms: 86400000

kafka:
  topics:
    product-events: product-events
    user-events: user-events
  consumer:
    group-id: product-service-group
```

- [ ] **Step 5: Verify build compiles**

Run: `mvn compile -pl product-service -am`
Expected: BUILD SUCCESS

- [ ] **Step 6: Commit**

```bash
git add product-service/
git commit -m "feat: add Product model and repository"
```

---

### Task 12: Product Service - Security & Service Layer

**Files:**
- Create: `product-service/src/main/java/com/jurol/buy01/product/security/SecurityConfig.java`
- Create: `product-service/src/main/java/com/jurol/buy01/product/security/JwtAuthenticationFilter.java`
- Create: `product-service/src/main/java/com/jurol/buy01/product/service/ProductService.java`
- Create: `product-service/src/main/java/com/jurol/buy01/product/kafka/ProductEventProducer.java`
- Create: `product-service/src/main/java/com/jurol/buy01/product/kafka/UserEventConsumer.java`

- [ ] **Step 1: Create JwtAuthenticationFilter**

Create `product-service/src/main/java/com/jurol/buy01/product/security/JwtAuthenticationFilter.java`:

```java
package com.jurol.buy01.product.security;

import com.jurol.buy01.common.security.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                if (jwtUtil.validateToken(token)) {
                    String userId = jwtUtil.getUserId(token);
                    String email = jwtUtil.getEmail(token);
                    String role = jwtUtil.getRole(token);

                    var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
                    var auth = new UsernamePasswordAuthenticationToken(userId, null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } catch (Exception e) {
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }
}
```

- [ ] **Step 2: Create SecurityConfig**

Create `product-service/src/main/java/com/jurol/buy01/product/security/SecurityConfig.java`:

```java
package com.jurol.buy01.product.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.GET, "/api/products/**").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
```

- [ ] **Step 3: Create ProductService**

Create `product-service/src/main/java/com/jurol/buy01/product/service/ProductService.java`:

```java
package com.jurol.buy01.product.service;

import com.jurol.buy01.common.dto.ProductDTO;
import com.jurol.buy01.common.events.ProductCreatedEvent;
import com.jurol.buy01.common.events.ProductDeletedEvent;
import com.jurol.buy01.product.kafka.ProductEventProducer;
import com.jurol.buy01.product.model.Product;
import com.jurol.buy01.product.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductEventProducer eventProducer;

    public ProductService(ProductRepository productRepository, ProductEventProducer eventProducer) {
        this.productRepository = productRepository;
        this.eventProducer = eventProducer;
    }

    public List<ProductDTO> getAllProducts() {
        return productRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public ProductDTO getProductById(String id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        return toDTO(product);
    }

    public ProductDTO createProduct(ProductDTO dto, String sellerId) {
        Product product = new Product(dto.getName(), dto.getDescription(), dto.getPrice(), sellerId);
        Product saved = productRepository.save(product);

        eventProducer.sendProductCreatedEvent(new ProductCreatedEvent(saved.getId(), sellerId));

        return toDTO(saved);
    }

    public ProductDTO updateProduct(String id, ProductDTO dto, String sellerId) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        if (!product.getSellerId().equals(sellerId)) {
            throw new RuntimeException("Not authorized to update this product");
        }

        if (dto.getName() != null) product.setName(dto.getName());
        if (dto.getDescription() != null) product.setDescription(dto.getDescription());
        if (dto.getPrice() != null) product.setPrice(dto.getPrice());

        Product saved = productRepository.save(product);
        return toDTO(saved);
    }

    public void deleteProduct(String id, String sellerId) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        if (!product.getSellerId().equals(sellerId)) {
            throw new RuntimeException("Not authorized to delete this product");
        }

        productRepository.deleteById(id);
        eventProducer.sendProductDeletedEvent(new ProductDeletedEvent(id, sellerId));
    }

    private ProductDTO toDTO(Product product) {
        return new ProductDTO(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getSellerId(),
                product.getMediaIds(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }
}
```

- [ ] **Step 4: Create ProductEventProducer**

Create `product-service/src/main/java/com/jurol/buy01/product/kafka/ProductEventProducer.java`:

```java
package com.jurol.buy01.product.kafka;

import com.jurol.buy01.common.events.ProductCreatedEvent;
import com.jurol.buy01.common.events.ProductDeletedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class ProductEventProducer {

    private static final Logger log = LoggerFactory.getLogger(ProductEventProducer.class);
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String topic;

    public ProductEventProducer(KafkaTemplate<String, Object> kafkaTemplate,
                                @Value("${kafka.topics.product-events}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public void sendProductCreatedEvent(ProductCreatedEvent event) {
        log.info("Sending ProductCreated event for product: {}", event.getProductId());
        kafkaTemplate.send(topic, event.getProductId(), event);
    }

    public void sendProductDeletedEvent(ProductDeletedEvent event) {
        log.info("Sending ProductDeleted event for product: {}", event.getProductId());
        kafkaTemplate.send(topic, event.getProductId(), event);
    }
}
```

- [ ] **Step 5: Create UserEventConsumer**

Create `product-service/src/main/java/com/jurol/buy01/product/kafka/UserEventConsumer.java`:

```java
package com.jurol.buy01.product.kafka;

import com.jurol.buy01.common.events.UserRegisteredEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class UserEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(UserEventConsumer.class);

    @KafkaListener(topics = "user-events", groupId = "product-service-group")
    public void handleUserRegistered(UserRegisteredEvent event) {
        log.info("Received UserRegistered event for user: {} with role: {}", event.getUserId(), event.getRole());
    }
}
```

- [ ] **Step 6: Verify build compiles**

Run: `mvn compile -pl product-service -am`
Expected: BUILD SUCCESS

- [ ] **Step 7: Commit**

```bash
git add product-service/
git commit -m "feat: add ProductService, security, and Kafka integration"
```

---

### Task 13: Product Service - Controller & Tests

**Files:**
- Create: `product-service/src/main/java/com/jurol/buy01/product/controller/ProductController.java`
- Create: `product-service/src/main/java/com/jurol/buy01/product/controller/GlobalExceptionHandler.java`
- Create: `product-service/src/test/java/com/jurol/buy01/product/service/ProductServiceTest.java`

- [ ] **Step 1: Create ProductController**

Create `product-service/src/main/java/com/jurol/buy01/product/controller/ProductController.java`:

```java
package com.jurol.buy01.product.controller;

import com.jurol.buy01.common.dto.ProductDTO;
import com.jurol.buy01.product.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public ResponseEntity<List<ProductDTO>> getAllProducts() {
        return ResponseEntity.ok(productService.getAllProducts());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductDTO> getProductById(@PathVariable String id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @PostMapping
    public ResponseEntity<ProductDTO> createProduct(Authentication authentication, @Valid @RequestBody ProductDTO dto) {
        String sellerId = (String) authentication.getPrincipal();
        ProductDTO created = productService.createProduct(dto, sellerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductDTO> updateProduct(Authentication authentication, @PathVariable String id, @RequestBody ProductDTO dto) {
        String sellerId = (String) authentication.getPrincipal();
        ProductDTO updated = productService.updateProduct(id, dto, sellerId);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(Authentication authentication, @PathVariable String id) {
        String sellerId = (String) authentication.getPrincipal();
        productService.deleteProduct(id, sellerId);
        return ResponseEntity.noContent().build();
    }
}
```

- [ ] **Step 2: Create GlobalExceptionHandler**

Create `product-service/src/main/java/com/jurol/buy01/product/controller/GlobalExceptionHandler.java`:

```java
package com.jurol.buy01.product.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex) {
        return ResponseEntity.badRequest().body(Map.of(
                "error", "BAD_REQUEST",
                "message", ex.getMessage(),
                "timestamp", Instant.now().toString()
        ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(MethodArgumentNotValidException ex) {
        String errors = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining(", "));

        return ResponseEntity.badRequest().body(Map.of(
                "error", "VALIDATION_ERROR",
                "message", errors,
                "timestamp", Instant.now().toString()
        ));
    }
}
```

- [ ] **Step 3: Create ProductServiceTest**

Create `product-service/src/test/java/com/jurol/buy01/product/service/ProductServiceTest.java`:

```java
package com.jurol.buy01.product.service;

import com.jurol.buy01.common.dto.ProductDTO;
import com.jurol.buy01.common.events.ProductCreatedEvent;
import com.jurol.buy01.common.events.ProductDeletedEvent;
import com.jurol.buy01.product.kafka.ProductEventProducer;
import com.jurol.buy01.product.model.Product;
import com.jurol.buy01.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductEventProducer eventProducer;

    @InjectMocks
    private ProductService productService;

    private Product product;
    private ProductDTO productDTO;

    @BeforeEach
    void setUp() {
        product = new Product("Test Product", "Description", new BigDecimal("29.99"), "seller123");
        product.setId("product123");

        productDTO = new ProductDTO("product123", "Test Product", "Description", new BigDecimal("29.99"), "seller123", null, null, null);
    }

    @Test
    void createProduct_shouldCreateAndPublishEvent() {
        when(productRepository.save(any(Product.class))).thenReturn(product);
        doNothing().when(eventProducer).sendProductCreatedEvent(any(ProductCreatedEvent.class));

        ProductDTO result = productService.createProduct(productDTO, "seller123");

        assertNotNull(result);
        assertEquals("Test Product", result.getName());
        verify(eventProducer).sendProductCreatedEvent(any(ProductCreatedEvent.class));
    }

    @Test
    void updateProduct_shouldUpdateIfOwner() {
        when(productRepository.findById("product123")).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        ProductDTO updateDto = new ProductDTO();
        updateDto.setName("Updated Product");

        ProductDTO result = productService.updateProduct("product123", updateDto, "seller123");

        assertEquals("Updated Product", result.getName());
    }

    @Test
    void updateProduct_shouldThrowIfNotOwner() {
        when(productRepository.findById("product123")).thenReturn(Optional.of(product));

        ProductDTO updateDto = new ProductDTO();
        updateDto.setName("Hacked Product");

        assertThrows(RuntimeException.class, () ->
                productService.updateProduct("product123", updateDto, "other-seller"));
    }

    @Test
    void deleteProduct_shouldDeleteAndPublishEvent() {
        when(productRepository.findById("product123")).thenReturn(Optional.of(product));
        doNothing().when(productRepository).deleteById("product123");
        doNothing().when(eventProducer).sendProductDeletedEvent(any(ProductDeletedEvent.class));

        productService.deleteProduct("product123", "seller123");

        verify(productRepository).deleteById("product123");
        verify(eventProducer).sendProductDeletedEvent(any(ProductDeletedEvent.class));
    }

    @Test
    void deleteProduct_shouldThrowIfNotOwner() {
        when(productRepository.findById("product123")).thenReturn(Optional.of(product));

        assertThrows(RuntimeException.class, () ->
                productService.deleteProduct("product123", "other-seller"));
    }
}
```

- [ ] **Step 4: Run tests**

Run: `mvn test -pl product-service -am`
Expected: All tests pass

- [ ] **Step 5: Commit**

```bash
git add product-service/
git commit -m "feat: add ProductController and tests"
```

---

### Task 14: Media Service - Model & Repository

**Files:**
- Create: `media-service/src/main/java/com/jurol/buy01/media/MediaServiceApplication.java`
- Create: `media-service/src/main/java/com/jurol/buy01/media/model/Media.java`
- Create: `media-service/src/main/java/com/jurol/buy01/media/repository/MediaRepository.java`
- Create: `media-service/src/main/resources/application.yml`

- [ ] **Step 1: Create MediaServiceApplication**

Create `media-service/src/main/java/com/jurol/buy01/media/MediaServiceApplication.java`:

```java
package com.jurol.buy01.media;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.jurol.buy01.media", "com.jurol.buy01.common"})
public class MediaServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(MediaServiceApplication.class, args);
    }
}
```

- [ ] **Step 2: Create Media model**

Create `media-service/src/main/java/com/jurol/buy01/media/model/Media.java`:

```java
package com.jurol.buy01.media.model;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "media")
public class Media {

    @Id
    private String id;
    private String filename;
    private String originalName;
    private String contentType;
    private Long size;
    private String path;
    private String productId;
    private String sellerId;

    @CreatedDate
    private Instant createdAt;

    public Media() {}

    public Media(String filename, String originalName, String contentType, Long size, String path, String productId, String sellerId) {
        this.filename = filename;
        this.originalName = originalName;
        this.contentType = contentType;
        this.size = size;
        this.path = path;
        this.productId = productId;
        this.sellerId = sellerId;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }
    public String getOriginalName() { return originalName; }
    public void setOriginalName(String originalName) { this.originalName = originalName; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public Long getSize() { return size; }
    public void setSize(Long size) { this.size = size; }
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
    public String getSellerId() { return sellerId; }
    public void setSellerId(String sellerId) { this.sellerId = sellerId; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
```

- [ ] **Step 3: Create MediaRepository**

Create `media-service/src/main/java/com/jurol/buy01/media/repository/MediaRepository.java`:

```java
package com.jurol.buy01.media.repository;

import com.jurol.buy01.media.model.Media;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MediaRepository extends MongoRepository<Media, String> {
    List<Media> findByProductId(String productId);
    List<Media> findBySellerId(String sellerId);
    void deleteByProductId(String productId);
}
```

- [ ] **Step 4: Create application.yml**

Create `media-service/src/main/resources/application.yml`:

```yaml
server:
  port: 8083

spring:
  application:
    name: media-service
  data:
    mongodb:
      uri: mongodb://localhost:27017/media_db
  kafka:
    bootstrap-servers: localhost:9092
  servlet:
    multipart:
      max-file-size: 2MB
      max-request-size: 2MB

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/

jwt:
  secret: mySecretKeyForJWTTokenGenerationThatIsAtLeast32Bytes!!
  expiration-ms: 86400000

kafka:
  topics:
    product-events: product-events
  consumer:
    group-id: media-service-group

media:
  storage:
    path: ./media-storage
  allowed-types: image/jpeg,image/png,image/gif,image/webp
  max-size: 2097152
```

- [ ] **Step 5: Verify build compiles**

Run: `mvn compile -pl media-service -am`
Expected: BUILD SUCCESS

- [ ] **Step 6: Commit**

```bash
git add media-service/
git commit -m "feat: add Media model and repository"
```

---

### Task 15: Media Service - File Validation

**Files:**
- Create: `media-service/src/main/java/com/jurol/buy01/media/validation/FileValidator.java`

- [ ] **Step 1: Create FileValidator**

Create `media-service/src/main/java/com/jurol/buy01/media/validation/FileValidator.java`:

```java
package com.jurol.buy01.media.validation;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;

@Component
public class FileValidator {

    private static final List<String> ALLOWED_MIME_TYPES = List.of(
            "image/jpeg", "image/png", "image/gif", "image/webp"
    );

    private static final byte[] JPEG_MAGIC = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] PNG_MAGIC = {(byte) 0x89, (byte) 0x50, (byte) 0x4E, (byte) 0x47};
    private static final byte[] GIF_MAGIC_87 = "GIF87a".getBytes();
    private static final byte[] GIF_MAGIC_89 = "GIF89a".getBytes();
    private static final byte[] WEBP_MAGIC_RIFF = "RIFF".getBytes();
    private static final byte[] WEBP_MAGIC_WEBP = "WEBP".getBytes();

    private final long maxSize;
    private final List<String> allowedTypes;

    public FileValidator(
            @Value("${media.allowed-types:image/jpeg,image/png,image/gif,image/webp}") String allowedTypesStr,
            @Value("${media.max-size:2097152}") long maxSize) {
        this.maxSize = maxSize;
        this.allowedTypes = Arrays.asList(allowedTypesStr.split(","));
    }

    public void validate(MultipartFile file) throws IOException, ValidationException {
        if (file.isEmpty()) {
            throw new ValidationException("FILE_EMPTY", "Uploaded file is empty");
        }

        if (file.getSize() > maxSize) {
            throw new ValidationException("FILE_TOO_LARGE",
                    String.format("File exceeds %d bytes limit", maxSize));
        }

        String contentType = file.getContentType();
        if (contentType == null || !allowedTypes.contains(contentType.toLowerCase())) {
            throw new ValidationException("INVALID_FILE_TYPE",
                    "Only JPEG, PNG, GIF, WebP images allowed. Got: " + contentType);
        }

        byte[] header = file.getBytes();
        if (header.length < 12) {
            throw new ValidationException("INVALID_FILE", "File is too small to be a valid image");
        }

        if (!matchesMagicBytes(header, contentType)) {
            throw new ValidationException("INVALID_FILE",
                    "File content does not match declared type: " + contentType);
        }
    }

    private boolean matchesMagicBytes(byte[] header, String contentType) {
        return switch (contentType.toLowerCase()) {
            case "image/jpeg" -> startsWith(header, JPEG_MAGIC);
            case "image/png" -> startsWith(header, PNG_MAGIC);
            case "image/gif" -> startsWith(header, GIF_MAGIC_87) || startsWith(header, GIF_MAGIC_89);
            case "image/webp" -> startsWith(header, WEBP_MAGIC_RIFF) && startsWith(header, 8, WEBP_MAGIC_WEBP);
            default -> false;
        };
    }

    private boolean startsWith(byte[] data, byte[] prefix) {
        return startsWith(data, 0, prefix);
    }

    private boolean startsWith(byte[] data, int offset, byte[] prefix) {
        if (data.length - offset < prefix.length) return false;
        for (int i = 0; i < prefix.length; i++) {
            if (data[offset + i] != prefix[i]) return false;
        }
        return true;
    }

    public static class ValidationException extends Exception {
        private final String errorCode;

        public ValidationException(String errorCode, String message) {
            super(message);
            this.errorCode = errorCode;
        }

        public String getErrorCode() { return errorCode; }
    }
}
```

- [ ] **Step 2: Verify build compiles**

Run: `mvn compile -pl media-service -am`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add media-service/
git commit -m "feat: add FileValidator with magic bytes check"
```

---

### Task 16: Media Service - Security & Service Layer

**Files:**
- Create: `media-service/src/main/java/com/jurol/buy01/media/security/SecurityConfig.java`
- Create: `media-service/src/main/java/com/jurol/buy01/media/security/JwtAuthenticationFilter.java`
- Create: `media-service/src/main/java/com/jurol/buy01/media/service/MediaService.java`
- Create: `media-service/src/main/java/com/jurol/buy01/media/kafka/ProductEventConsumer.java`

- [ ] **Step 1: Create JwtAuthenticationFilter**

Create `media-service/src/main/java/com/jurol/buy01/media/security/JwtAuthenticationFilter.java`:

```java
package com.jurol.buy01.media.security;

import com.jurol.buy01.common.security.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                if (jwtUtil.validateToken(token)) {
                    String userId = jwtUtil.getUserId(token);
                    String email = jwtUtil.getEmail(token);
                    String role = jwtUtil.getRole(token);

                    var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
                    var auth = new UsernamePasswordAuthenticationToken(userId, null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } catch (Exception e) {
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }
}
```

- [ ] **Step 2: Create SecurityConfig**

Create `media-service/src/main/java/com/jurol/buy01/media/security/SecurityConfig.java`:

```java
package com.jurol.buy01.media.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.GET, "/api/media/**").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
```

- [ ] **Step 3: Create MediaService**

Create `media-service/src/main/java/com/jurol/buy01/media/service/MediaService.java`:

```java
package com.jurol.buy01.media.service;

import com.jurol.buy01.common.dto.MediaDTO;
import com.jurol.buy01.media.model.Media;
import com.jurol.buy01.media.repository.MediaRepository;
import com.jurol.buy01.media.validation.FileValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class MediaService {

    private final MediaRepository mediaRepository;
    private final FileValidator fileValidator;
    private final String storagePath;

    public MediaService(MediaRepository mediaRepository, FileValidator fileValidator,
                        @Value("${media.storage.path:./media-storage}") String storagePath) {
        this.mediaRepository = mediaRepository;
        this.fileValidator = fileValidator;
        this.storagePath = storagePath;
    }

    public MediaDTO uploadFile(MultipartFile file, String productId, String sellerId) throws IOException, FileValidator.ValidationException {
        fileValidator.validate(file);

        String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path sellerDir = Paths.get(storagePath, sellerId);
        Files.createDirectories(sellerDir);
        Path filePath = sellerDir.resolve(filename);
        file.transferTo(filePath.toFile());

        Media media = new Media(
                filename,
                file.getOriginalFilename(),
                file.getContentType(),
                file.getSize(),
                filePath.toString(),
                productId,
                sellerId
        );

        Media saved = mediaRepository.save(media);
        return toDTO(saved);
    }

    public MediaDTO getMediaById(String id) {
        Media media = mediaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Media not found"));
        return toDTO(media);
    }

    public List<MediaDTO> getMediaByProductId(String productId) {
        return mediaRepository.findByProductId(productId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public void deleteMedia(String id, String sellerId) {
        Media media = mediaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Media not found"));

        if (!media.getSellerId().equals(sellerId)) {
            throw new RuntimeException("Not authorized to delete this media");
        }

        try {
            Path filePath = Paths.get(media.getPath());
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            // Log but don't fail - file might already be deleted
        }

        mediaRepository.deleteById(id);
    }

    public void deleteMediaByProductId(String productId) {
        List<Media> mediaList = mediaRepository.findByProductId(productId);
        for (Media media : mediaList) {
            try {
                Path filePath = Paths.get(media.getPath());
                Files.deleteIfExists(filePath);
            } catch (IOException e) {
                // Log but continue
            }
        }
        mediaRepository.deleteByProductId(productId);
    }

    private MediaDTO toDTO(Media media) {
        return new MediaDTO(
                media.getId(),
                media.getFilename(),
                media.getOriginalName(),
                media.getContentType(),
                media.getSize(),
                media.getProductId(),
                media.getSellerId(),
                media.getCreatedAt()
        );
    }
}
```

- [ ] **Step 4: Create ProductEventConsumer**

Create `media-service/src/main/java/com/jurol/buy01/media/kafka/ProductEventConsumer.java`:

```java
package com.jurol.buy01.media.kafka;

import com.jurol.buy01.common.events.ProductCreatedEvent;
import com.jurol.buy01.common.events.ProductDeletedEvent;
import com.jurol.buy01.media.service.MediaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class ProductEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(ProductEventConsumer.class);
    private final MediaService mediaService;

    public ProductEventConsumer(MediaService mediaService) {
        this.mediaService = mediaService;
    }

    @KafkaListener(topics = "product-events", groupId = "media-service-group")
    public void handleProductEvent(Object event) {
        if (event instanceof ProductCreatedEvent created) {
            log.info("Received ProductCreated event for product: {}", created.getProductId());
        } else if (event instanceof ProductDeletedEvent deleted) {
            log.info("Received ProductDeleted event for product: {}", deleted.getProductId());
            mediaService.deleteMediaByProductId(deleted.getProductId());
        }
    }
}
```

- [ ] **Step 5: Verify build compiles**

Run: `mvn compile -pl media-service -am`
Expected: BUILD SUCCESS

- [ ] **Step 6: Commit**

```bash
git add media-service/
git commit -m "feat: add MediaService, security, and Kafka consumer"
```

---

### Task 17: Media Service - Controller & Tests

**Files:**
- Create: `media-service/src/main/java/com/jurol/buy01/media/controller/MediaController.java`
- Create: `media-service/src/main/java/com/jurol/buy01/media/controller/GlobalExceptionHandler.java`
- Create: `media-service/src/test/java/com/jurol/buy01/media/validation/FileValidatorTest.java`
- Create: `media-service/src/test/java/com/jurol/buy01/media/service/MediaServiceTest.java`

- [ ] **Step 1: Create MediaController**

Create `media-service/src/main/java/com/jurol/buy01/media/controller/MediaController.java`:

```java
package com.jurol.buy01.media.controller;

import com.jurol.buy01.common.dto.MediaDTO;
import com.jurol.buy01.media.service.MediaService;
import com.jurol.buy01.media.validation.FileValidator;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/media")
public class MediaController {

    private final MediaService mediaService;

    public MediaController(MediaService mediaService) {
        this.mediaService = mediaService;
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(
            Authentication authentication,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "productId", required = false) String productId) {
        String sellerId = (String) authentication.getPrincipal();
        try {
            MediaDTO media = mediaService.uploadFile(file, productId, sellerId);
            return ResponseEntity.status(HttpStatus.CREATED).body(media);
        } catch (FileValidator.ValidationException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getErrorCode(),
                    "message", e.getMessage(),
                    "timestamp", Instant.now().toString()
            ));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "error", "UPLOAD_FAILED",
                    "message", "Failed to upload file: " + e.getMessage(),
                    "timestamp", Instant.now().toString()
            ));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<MediaDTO> getMediaById(@PathVariable String id) {
        return ResponseEntity.ok(mediaService.getMediaById(id));
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<List<MediaDTO>> getMediaByProductId(@PathVariable String productId) {
        return ResponseEntity.ok(mediaService.getMediaByProductId(productId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMedia(Authentication authentication, @PathVariable String id) {
        String sellerId = (String) authentication.getPrincipal();
        mediaService.deleteMedia(id, sellerId);
        return ResponseEntity.noContent().build();
    }
}
```

- [ ] **Step 2: Create GlobalExceptionHandler**

Create `media-service/src/main/java/com/jurol/buy01/media/controller/GlobalExceptionHandler.java`:

```java
package com.jurol.buy01.media.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex) {
        return ResponseEntity.badRequest().body(Map.of(
                "error", "BAD_REQUEST",
                "message", ex.getMessage(),
                "timestamp", Instant.now().toString()
        ));
    }
}
```

- [ ] **Step 3: Create FileValidatorTest**

Create `media-service/src/test/java/com/jurol/buy01/media/validation/FileValidatorTest.java`:

```java
package com.jurol.buy01.media.validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class FileValidatorTest {

    private FileValidator fileValidator;

    @BeforeEach
    void setUp() {
        fileValidator = new FileValidator("image/jpeg,image/png,image/gif,image/webp", 2097152);
    }

    @Test
    void validate_shouldAcceptValidJpeg() throws Exception {
        byte[] jpegContent = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", jpegContent);

        assertDoesNotThrow(() -> fileValidator.validate(file));
    }

    @Test
    void validate_shouldAcceptValidPng() throws Exception {
        byte[] pngContent = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00, 0x00, 0x00};
        MockMultipartFile file = new MockMultipartFile("file", "test.png", "image/png", pngContent);

        assertDoesNotThrow(() -> fileValidator.validate(file));
    }

    @Test
    void validate_shouldRejectEmptyFile() {
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", new byte[0]);

        assertThrows(FileValidator.ValidationException.class, () -> fileValidator.validate(file));
    }

    @Test
    void validate_shouldRejectOversizedFile() {
        byte[] largeContent = new byte[2097153]; // 2MB + 1 byte
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", largeContent);

        assertThrows(FileValidator.ValidationException.class, () -> fileValidator.validate(file));
    }

    @Test
    void validate_shouldRejectInvalidContentType() {
        MockMultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf", new byte[]{0x25, 0x50, 0x44, 0x46});

        assertThrows(FileValidator.ValidationException.class, () -> fileValidator.validate(file));
    }

    @Test
    void validate_shouldRejectMismatchedMagicBytes() {
        byte[] content = new byte[]{0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", content);

        assertThrows(FileValidator.ValidationException.class, () -> fileValidator.validate(file));
    }
}
```

- [ ] **Step 4: Create MediaServiceTest**

Create `media-service/src/test/java/com/jurol/buy01/media/service/MediaServiceTest.java`:

```java
package com.jurol.buy01.media.service;

import com.jurol.buy01.common.dto.MediaDTO;
import com.jurol.buy01.media.model.Media;
import com.jurol.buy01.media.repository.MediaRepository;
import com.jurol.buy01.media.validation.FileValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MediaServiceTest {

    @Mock
    private MediaRepository mediaRepository;

    @Mock
    private FileValidator fileValidator;

    @InjectMocks
    private MediaService mediaService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(mediaService, "storagePath", "./test-media-storage");
    }

    @Test
    void getMediaById_shouldReturnMedia() {
        Media media = new Media("test.jpg", "test.jpg", "image/jpeg", 1024L, "/path/test.jpg", "product123", "seller123");
        media.setId("media123");

        when(mediaRepository.findById("media123")).thenReturn(Optional.of(media));

        MediaDTO result = mediaService.getMediaById("media123");

        assertNotNull(result);
        assertEquals("test.jpg", result.getFilename());
    }

    @Test
    void getMediaById_shouldThrowIfNotFound() {
        when(mediaRepository.findById("nonexistent")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> mediaService.getMediaById("nonexistent"));
    }

    @Test
    void deleteMedia_shouldDeleteIfOwner() {
        Media media = new Media("test.jpg", "test.jpg", "image/jpeg", 1024L, "/path/test.jpg", "product123", "seller123");
        media.setId("media123");

        when(mediaRepository.findById("media123")).thenReturn(Optional.of(media));
        doNothing().when(mediaRepository).deleteById("media123");

        mediaService.deleteMedia("media123", "seller123");

        verify(mediaRepository).deleteById("media123");
    }

    @Test
    void deleteMedia_shouldThrowIfNotOwner() {
        Media media = new Media("test.jpg", "test.jpg", "image/jpeg", 1024L, "/path/test.jpg", "product123", "seller123");
        media.setId("media123");

        when(mediaRepository.findById("media123")).thenReturn(Optional.of(media));

        assertThrows(RuntimeException.class, () -> mediaService.deleteMedia("media123", "other-seller"));
    }
}
```

- [ ] **Step 5: Run tests**

Run: `mvn test -pl media-service -am`
Expected: All tests pass

- [ ] **Step 6: Commit**

```bash
git add media-service/
git commit -m "feat: add MediaController, FileValidator, and tests"
```

---

### Task 18: API Gateway

**Files:**
- Create: `api-gateway/src/main/java/com/jurol/buy01/gateway/ApiGatewayApplication.java`
- Create: `api-gateway/src/main/java/com/jurol/buy01/gateway/JwtGatewayFilter.java`
- Create: `api-gateway/src/main/java/com/jurol/buy01/gateway/RouteConfig.java`
- Create: `api-gateway/src/main/resources/application.yml`

- [ ] **Step 1: Create ApiGatewayApplication**

Create `api-gateway/src/main/java/com/jurol/buy01/gateway/ApiGatewayApplication.java`:

```java
package com.jurol.buy01.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.List;

@SpringBootApplication
public class ApiGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:4200"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsWebFilter(source);
    }
}
```

- [ ] **Step 2: Create JwtGatewayFilter**

Create `api-gateway/src/main/java/com/jurol/buy01/gateway/JwtGatewayFilter.java`:

```java
package com.jurol.buy01.gateway;

import com.jurol.buy01.common.security.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class JwtGatewayFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(JwtGatewayFilter.class);
    private final JwtUtil jwtUtil;

    public JwtGatewayFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        List<String> publicPaths = List.of("/api/auth/", "/api/products", "/api/media/");
        String path = request.getURI().getPath();

        boolean isPublic = publicPaths.stream().anyMatch(path::startsWith)
                || (path.startsWith("/api/products/") && request.getMethod().name().equals("GET"))
                || (path.startsWith("/api/media/") && request.getMethod().name().equals("GET"));

        if (isPublic) {
            return chain.filter(exchange);
        }

        String header = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) {
            return unauthorized(exchange);
        }

        String token = header.substring(7);
        try {
            if (!jwtUtil.validateToken(token)) {
                return unauthorized(exchange);
            }

            String userId = jwtUtil.getUserId(token);
            String email = jwtUtil.getEmail(token);
            String role = jwtUtil.getRole(token);

            ServerHttpRequest modifiedRequest = request.mutate()
                    .header("X-User-Id", userId)
                    .header("X-User-Email", email)
                    .header("X-User-Role", role)
                    .build();

            return chain.filter(exchange.mutate().request(modifiedRequest).build());
        } catch (Exception e) {
            log.error("JWT validation failed", e);
            return unauthorized(exchange);
        }
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        return response.setComplete();
    }

    @Override
    public int getOrder() {
        return -1; // Run before other filters
    }
}
```

- [ ] **Step 3: Create RouteConfig**

Create `api-gateway/src/main/java/com/jurol/buy01/gateway/RouteConfig.java`:

```java
package com.jurol.buy01.gateway;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RouteConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("user-service", r -> r
                        .path("/api/auth/**", "/api/users/**")
                        .filters(f -> f.stripPrefix(0))
                        .uri("lb://user-service"))
                .route("product-service", r -> r
                        .path("/api/products/**")
                        .filters(f -> f.stripPrefix(0))
                        .uri("lb://product-service"))
                .route("media-service", r -> r
                        .path("/api/media/**")
                        .filters(f -> f.stripPrefix(0))
                        .uri("lb://media-service"))
                .build();
    }
}
```

- [ ] **Step 4: Create application.yml**

Create `api-gateway/src/main/resources/application.yml`:

```yaml
server:
  port: 9000

spring:
  application:
    name: api-gateway
  cloud:
    gateway:
      default-filters:
        - DedupeResponseHeader=Access-Control-Allow-Credentials Access-Control-Allow-Origin

  kafka:
    bootstrap-servers: localhost:9092

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/

jwt:
  secret: mySecretKeyForJWTTokenGenerationThatIsAtLeast32Bytes!!
  expiration-ms: 86400000

management:
  endpoints:
    web:
      exposure:
        include: health,info
```

- [ ] **Step 5: Verify build compiles**

Run: `mvn compile -pl api-gateway -am`
Expected: BUILD SUCCESS

- [ ] **Step 6: Commit**

```bash
git add api-gateway/
git commit -m "feat: add API Gateway with JWT validation and routing"
```

---

### Task 19: Integration Verification

**Files:**
- Create: `api-gateway/src/test/java/com/jurol/buy01/gateway/ApiGatewayApplicationTests.java`
- Create: `eureka-server/src/test/java/com/jurol/buy01/eureka/EurekaServerApplicationTests.java`
- Create: `user-service/src/test/java/com/jurol/buy01/user/UserServiceApplicationTests.java`
- Create: `product-service/src/test/java/com/jurol/buy01/product/ProductServiceApplicationTests.java`
- Create: `media-service/src/test/java/com/jurol/buy01/media/MediaServiceApplicationTests.java`

- [ ] **Step 1: Create ApiGatewayApplicationTests**

Create `api-gateway/src/test/java/com/jurol/buy01/gateway/ApiGatewayApplicationTests.java`:

```java
package com.jurol.buy01.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ApiGatewayApplicationTests {

    @Test
    void contextLoads() {
    }
}
```

- [ ] **Step 2: Create ProductServiceApplicationTests**

Create `product-service/src/test/java/com/jurol/buy01/product/ProductServiceApplicationTests.java`:

```java
package com.jurol.buy01.product;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ProductServiceApplicationTests {

    @Test
    void contextLoads() {
    }
}
```

- [ ] **Step 3: Create MediaServiceApplicationTests**

Create `media-service/src/test/java/com/jurol/buy01/media/MediaServiceApplicationTests.java`:

```java
package com.jurol.buy01.media;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class MediaServiceApplicationTests {

    @Test
    void contextLoads() {
    }
}
```

- [ ] **Step 4: Run all tests**

Run: `mvn test`
Expected: All tests pass

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "test: add integration tests for all services"
```

---

### Task 20: Final Build Verification

- [ ] **Step 1: Clean and build entire project**

Run: `mvn clean install`
Expected: BUILD SUCCESS for all modules

- [ ] **Step 2: Verify all modules are built**

Run: `ls -la */target/*.jar`
Expected: JAR files for each service module

- [ ] **Step 3: Final commit**

```bash
git add -A
git commit -m "chore: complete backend microservices setup"
```
