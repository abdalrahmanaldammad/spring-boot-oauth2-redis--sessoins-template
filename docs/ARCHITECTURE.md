# Project Architecture

## Domain-Driven Package Structure

This Spring Boot application follows a domain-driven design approach where code is organized by business domains rather than technical layers.

### Package Structure

```
src/main/java/com/example/app/
├── SpringSecurityRedisSessionApplication.java  # Main application class
│
├── auth/                           # Authentication Domain
│   ├── controller/
│   │   └── AuthController.java     # Login, register, logout, session management
│   ├── service/
│   │   ├── CustomUserDetailsService.java
│   │   ├── CustomOAuth2UserService.java
│   │   ├── UserPrincipal.java
│   │   └── OAuth2UserInfo*.java
│   └── dto/
│       ├── LoginRequest.java
│       └── RegisterRequest.java
│
├── user/                           # User Management Domain
│   ├── entity/
│   │   └── User.java               # User entity with all user-related data
│   ├── controller/
│   │   └── UserController.java     # User profile operations
│   ├── service/
│   │   └── [User services go here]
│   ├── repository/
│   │   └── UserRepository.java     # User data access
│   └── dto/
│       └── UserResponse.java       # User response DTOs
│
├── email/                          # Email Domain
│   ├── entity/
│   │   └── VerificationToken.java  # Email verification tokens
│   ├── service/
│   │   ├── EmailService.java       # Email sending logic
│   │   └── VerificationTokenService.java
│   └── repository/
│       └── VerificationTokenRepository.java
│
├── admin/                          # Admin Domain
│   ├── controller/
│   │   └── AdminController.java    # Admin-only operations
│   └── service/
│       └── [Admin services go here]
│
└── common/                         # Shared Components
    ├── entity/
    │   ├── Role.java               # Shared entities
    │   └── AuthProvider.java
    ├── config/
    │   ├── SecurityConfig.java     # Security configuration
    │   ├── RedisConfig.java        # Redis configuration
    │   └── *Handler.java           # Auth handlers
    ├── exception/
    │   ├── GlobalExceptionHandler.java
    │   └── *Exception.java         # Custom exceptions
    ├── repository/
    │   └── RoleRepository.java     # Shared repositories
    ├── controller/
    │   ├── PublicController.java   # Public endpoints
    │   └── SimpleSessionController.java
    ├── dto/
    │   └── ErrorResponse.java      # Common DTOs
    └── util/
        ├── DataInitializationService.java
        └── SecurityUtils.java
```

## Benefits of This Structure

### 1. **Domain Separation**
- Each business domain is self-contained
- Easy to understand what each package does
- Reduces coupling between domains

### 2. **Scalability**
- Easy to add new domains (e.g., `product/`, `order/`, `payment/`)
- Each domain can evolve independently
- Clear boundaries for future microservices extraction

### 3. **Team Organization**
- Different teams can work on different domains
- Reduces merge conflicts
- Clear ownership of code

### 4. **Maintainability**
- Related code is grouped together
- Easy to find where specific functionality is implemented
- Natural place for new features

## How to Add a New Domain (e.g., Product)

1. **Create domain structure:**
```bash
mkdir -p src/main/java/com/example/app/product/{entity,controller,service,dto,repository}
```

2. **Create the entity:**
```java
// src/main/java/com/example/app/product/entity/Product.java
package com.example.app.product.entity;

@Entity
@Table(name = "products")
public class Product {
    // Product fields
}
```

3. **Create the repository:**
```java
// src/main/java/com/example/app/product/repository/ProductRepository.java
package com.example.app.product.repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    // Custom queries
}
```

4. **Create the service:**
```java
// src/main/java/com/example/app/product/service/ProductService.java
package com.example.app.product.service;

@Service
public class ProductService {
    // Business logic
}
```

5. **Create the controller:**
```java
// src/main/java/com/example/app/product/controller/ProductController.java
package com.example.app.product.controller;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    // REST endpoints
}
```

6. **Create DTOs:**
```java
// src/main/java/com/example/app/product/dto/ProductRequest.java
// src/main/java/com/example/app/product/dto/ProductResponse.java
```

## Package Dependencies

- `auth` → depends on `user`, `common`
- `user` → depends on `common`
- `email` → depends on `user`, `common`
- `admin` → depends on `user`, `common`
- `common` → no dependencies (base layer)

## Configuration Files

- `application.yml` - Main configuration
- `data.sql` - Initial data setup
- `templates/` - Email templates

## Development Guidelines

1. **Keep domains independent** - Avoid direct dependencies between business domains
2. **Use common for shared code** - Put shared entities, exceptions, and utilities in common
3. **Follow naming conventions** - Use consistent naming across domains
4. **Maintain package structure** - Don't mix different concerns in the same package
