# Microservices E-Commerce Platform - Backend Design

## Overview

End-to-end e-commerce platform with Spring Boot microservices. Phase 1 focuses on backend services; Angular frontend comes in Phase 2.

## Architecture

### Services

| Service | Port | Database | Purpose |
|---------|------|----------|---------|
| Eureka Server | 8761 | - | Service discovery registry |
| API Gateway | 9000 | - | Single entry point, JWT validation, routing |
| User Service | 8081 | `users_db` | Registration, authentication, profiles |
| Product Service | 8082 | `products_db` | Product CRUD operations |
| Media Service | 8083 | `media_db` | File uploads, validation, storage |

### Communication

- **Synchronous**: Client → API Gateway → Service (REST)
- **Asynchronous**: Service ↔ Service (Kafka events)
- **Discovery**: All services register with Eureka on startup

### Project Structure

Monorepo with multi-module Maven:

```
buy-01/
├── pom.xml (parent)
├── common/              (shared DTOs, security utils)
├── eureka-server/       (service discovery)
├── api-gateway/         (single entry point)
├── user-service/        (auth, profiles)
├── product-service/     (CRUD, images)
└── media-service/       (file uploads)
```

## Data Models

### User Service (`users` collection)

```json
{
  "_id": "ObjectId",
  "email": "string (unique)",
  "password": "string (BCrypt hashed)",
  "firstName": "string",
  "lastName": "string",
  "role": "CLIENT | SELLER",
  "avatar": "string (URL, sellers only)",
  "createdAt": "ISODate",
  "updatedAt": "ISODate"
}
```

### Product Service (`products` collection)

```json
{
  "_id": "ObjectId",
  "name": "string",
  "description": "string",
  "price": "decimal",
  "sellerId": "string (references User._id)",
  "mediaIds": ["string (references Media._id)"],
  "createdAt": "ISODate",
  "updatedAt": "ISODate"
}
```

### Media Service (`media` collection)

```json
{
  "_id": "ObjectId",
  "filename": "string",
  "originalName": "string",
  "contentType": "string",
  "size": "number (bytes)",
  "path": "string (filesystem path)",
  "productId": "string (references Product._id)",
  "sellerId": "string (references User._id)",
  "createdAt": "ISODate"
}
```

### Relationships

- `Product.sellerId` → `User._id` (ownership)
- `Product.mediaIds` → `Media._id[]` (product images)
- `Media.productId` → `Product._id` (image belongs to product)

## Authentication & Security

### JWT Flow

1. User registers via `POST /api/auth/register` → User Service creates account, returns JWT
2. User logs in via `POST /api/auth/login` → User Service validates credentials, returns JWT
3. JWT contains: `userId`, `email`, `role` (CLIENT/SELLER), `exp`
4. API Gateway validates JWT on every request, forwards user info in headers

### Role-Based Access

- `CLIENT`: Can browse products, view media
- `SELLER`: Can CRUD their own products, upload media for their products
- Ownership check: Seller can only modify/delete products they created

### Password Security

- BCrypt hashing with salt via Spring Security
- Passwords never exposed in API responses (`@JsonIgnore`)

### API Gateway Security

- Validates JWT signature using shared secret
- Extracts user info, adds headers (`X-User-Id`, `X-User-Role`)
- Routes to appropriate service

## Kafka Events

### Topics

| Topic | Event | Producer | Consumer | Purpose |
|-------|-------|----------|----------|---------|
| `user-events` | `UserRegistered` | User Service | Product, Media | Notify services of new user |
| `product-events` | `ProductCreated` | Product Service | Media | Link media to product |
| `product-events` | `ProductDeleted` | Product Service | Media | Clean up orphaned media |

### Event Payloads

```json
// UserRegistered
{
  "eventType": "USER_REGISTERED",
  "userId": "string",
  "email": "string",
  "role": "CLIENT | SELLER",
  "timestamp": "ISODate"
}

// ProductCreated
{
  "eventType": "PRODUCT_CREATED",
  "productId": "string",
  "sellerId": "string",
  "timestamp": "ISODate"
}

// ProductDeleted
{
  "eventType": "PRODUCT_DELETED",
  "productId": "string",
  "sellerId": "string",
  "timestamp": "ISODate"
}
```

### Configuration

- Single Kafka broker (localhost:9092) for development
- Each service has its own consumer group
- JSON serialization via `JsonSerializer`/`JsonDeserializer`

## Media Service Details

### Endpoints

- `POST /api/media/upload` - Upload image (seller only)
- `GET /api/media/{id}` - Get media metadata
- `GET /api/media/product/{productId}` - Get all media for a product
- `DELETE /api/media/{id}` - Delete media (owner only)

### Validation Rules

- Max file size: 2MB
- Allowed types: `image/jpeg`, `image/png`, `image/gif`, `image/webp`
- File type validation via `Content-Type` header AND magic bytes check
- Storage path: `./media-storage/{sellerId}/{filename}`

### Error Responses

```json
{
  "error": "FILE_TOO_LARGE",
  "message": "File exceeds 2MB limit",
  "maxSize": "2MB"
}

{
  "error": "INVALID_FILE_TYPE",
  "message": "Only JPEG, PNG, GIF, WebP images allowed",
  "allowedTypes": ["image/jpeg", "image/png", "image/gif", "image/webp"]
}
```

## API Endpoints

### User Service

| Method | Path | Auth | Role | Description |
|--------|------|------|------|-------------|
| POST | `/api/auth/register` | No | - | Register new user |
| POST | `/api/auth/login` | No | - | Login, get JWT |
| GET | `/api/users/me` | Yes | Any | Get current user profile |
| PUT | `/api/users/me` | Yes | Any | Update profile |
| PUT | `/api/users/me/avatar` | Yes | SELLER | Upload/update avatar |

### Product Service

| Method | Path | Auth | Role | Description |
|--------|------|------|------|-------------|
| GET | `/api/products` | No | - | List all products |
| GET | `/api/products/{id}` | No | - | Get product by ID |
| POST | `/api/products` | Yes | SELLER | Create product |
| PUT | `/api/products/{id}` | Yes | SELLER | Update product (owner only) |
| DELETE | `/api/products/{id}` | Yes | SELLER | Delete product (owner only) |

### Media Service

| Method | Path | Auth | Role | Description |
|--------|------|------|------|-------------|
| POST | `/api/media/upload` | Yes | SELLER | Upload image |
| GET | `/api/media/{id}` | No | - | Get media metadata |
| GET | `/api/media/product/{productId}` | No | - | Get product images |
| DELETE | `/api/media/{id}` | Yes | SELLER | Delete media (owner only) |

## Testing Strategy

### Unit Tests

- Service layer logic (business rules, validation)
- Media file validation (size, type checking)
- JWT token generation/validation

### Integration Tests

- API endpoint testing with MockMvc
- MongoDB repository tests (Testcontainers)
- Kafka event publishing/consuming

### Security Tests

- Role-based access (client can't create products)
- Ownership enforcement (seller can't modify another's products)
- JWT validation (expired tokens, invalid signatures)
- File upload constraints (size, type)

### Test Tools

- JUnit 5 + Mockito for unit tests
- Spring Boot Test + MockMvc for integration tests
- Testcontainers for MongoDB and Kafka

## Dependencies

### Spring Boot Starters

- `spring-boot-starter-web` - REST APIs
- `spring-boot-starter-data-mongodb` - MongoDB access
- `spring-boot-starter-security` - Authentication/authorization
- `spring-boot-starter-validation` - Bean validation
- `spring-cloud-starter-netflix-eureka-client` - Service discovery
- `spring-cloud-starter-gateway` - API Gateway
- `spring-kafka` - Kafka integration

### Testing Dependencies

- `spring-boot-starter-test` - Testing framework
- `spring-security-test` - Security testing
- `testcontainers-mongodb` - MongoDB in tests
- `testcontainers-kafka` - Kafka in tests
