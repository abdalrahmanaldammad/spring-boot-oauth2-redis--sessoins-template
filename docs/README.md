# Spring Security Redis Session Demo

A comprehensive Spring Boot REST API backend optimized for **React frontend integration**, demonstrating Spring Security authentication and authorization with Redis session management and H2 database integration.

## 🚀 **Optimized for React Frontend**

This backend is specifically configured for React/SPA integration:
- **Pure REST API** (no server-side rendering)
- **CORS configured** for React development (port 3000)
- **Session-based authentication** with automatic cookie handling
- **JSON-only responses** for all endpoints
- **React-friendly authentication status** endpoint (`/auth/status`)

👉 **See [REACT_INTEGRATION.md](REACT_INTEGRATION.md) for complete React setup guide**

## Features

- **Spring Security Authentication**: Form-based login with custom UserDetailsService
- **Redis Session Storage**: Distributed session management using Spring Session Data Redis
- **Role-based Authorization**: RBAC with different user roles (ADMIN, MANAGER, MODERATOR, USER)
- **H2 Database Integration**: In-memory database for user and role persistence
- **Session Management**: Session monitoring, invalidation, and concurrent session control
- **RESTful APIs**: Comprehensive REST endpoints for authentication and user management
- **Security Best Practices**: Password encoding, CSRF protection, proper session handling

## Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Client        │    │  Spring Boot    │    │     Redis       │
│   (Browser/     │◄──►│   Application   │◄──►│   (Session      │
│    API Client)  │    │                 │    │    Storage)     │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                                │
                                ▼
                       ┌─────────────────┐
                       │   H2 Database   │
                       │  (User/Role     │
                       │   Storage)      │
                       └─────────────────┘
```

## Prerequisites

- Java 17 or higher
- Maven 3.6 or higher
- Redis server running on localhost:6379
- Docker (optional, for Redis)

## Quick Start

### 1. Start Redis Server

Using Docker:
```bash
docker run -d -p 6379:6379 --name redis redis:latest
```

Or install Redis locally and start it:
```bash
redis-server
```

### 2. Clone and Build

```bash
git clone <repository-url>
cd spring-security-redis-session
mvn clean install
```

### 3. Run the Application

```bash
mvn spring-boot:run
```

The application will start on `http://localhost:8080/api`

### 4. Test the Setup

Visit `http://localhost:8080/api/public/health` to verify the application is running.

## Configuration

### Application Properties

Key configuration options in `application.yml`:

```yaml
spring:
  # Redis Configuration
  data:
    redis:
      host: localhost
      port: 6379
      
  # Session Configuration
  session:
    store-type: redis
    timeout: 30m
    
  # H2 Database
  datasource:
    url: jdbc:h2:mem:testdb
    
server:
  port: 8080
  servlet:
    context-path: /api
```

## Default Users

The application comes with pre-configured test users:

| Username  | Password    | Roles                    |
|-----------|-------------|--------------------------|
| admin     | admin123    | ROLE_ADMIN               |
| manager   | manager123  | ROLE_MANAGER, ROLE_USER  |
| moderator | moderator123| ROLE_MODERATOR, ROLE_USER|
| alice     | alice123    | ROLE_USER                |
| bob       | bob123      | ROLE_USER                |
| charlie   | charlie123  | ROLE_USER                |
| diana     | diana123    | ROLE_USER                |
| eve       | eve123      | ROLE_USER                |

## API Documentation

### Authentication Endpoints

#### Login
```bash
POST /api/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "admin123"
}
```

Response:
```json
{
  "success": true,
  "message": "Login successful",
  "sessionId": "12345-67890-ABCDE",
  "user": {
    "id": 1,
    "username": "admin",
    "email": "admin@example.com",
    "firstName": "System",
    "lastName": "Administrator",
    "enabled": true,
    "roles": ["ROLE_ADMIN"]
  }
}
```

#### Register
```bash
POST /api/auth/register
Content-Type: application/json

{
  "username": "newuser",
  "email": "newuser@example.com",
  "password": "password123",
  "firstName": "New",
  "lastName": "User"
}
```

#### Get Current User
```bash
GET /api/auth/me
Cookie: JSESSIONID=<session-id>
```

#### Logout
```bash
POST /api/auth/logout
Cookie: JSESSIONID=<session-id>
```

### User Endpoints (ROLE_USER required)

#### Get Profile
```bash
GET /api/user/profile
Cookie: JSESSIONID=<session-id>
```

#### Update Profile
```bash
PUT /api/user/profile
Content-Type: application/json
Cookie: JSESSIONID=<session-id>

{
  "firstName": "Updated",
  "lastName": "Name",
  "email": "updated@example.com"
}
```

#### Change Password
```bash
POST /api/user/change-password
Content-Type: application/json
Cookie: JSESSIONID=<session-id>

{
  "currentPassword": "oldpassword",
  "newPassword": "newpassword123"
}
```

### Admin Endpoints (ROLE_ADMIN required)

#### Get All Users
```bash
GET /api/admin/users
Cookie: JSESSIONID=<session-id>
```

#### Enable/Disable User
```bash
PUT /api/admin/users/{userId}/enable
PUT /api/admin/users/{userId}/disable
Cookie: JSESSIONID=<session-id>
```

#### Get System Statistics
```bash
GET /api/admin/stats
Cookie: JSESSIONID=<session-id>
```

#### Get All Active Sessions
```bash
GET /api/admin/sessions/all
Cookie: JSESSIONID=<session-id>
```

### Session Management

#### Get Session Info
```bash
GET /api/auth/session/info
Cookie: JSESSIONID=<session-id>
```

#### Get Active Sessions (Current User)
```bash
GET /api/auth/sessions/active
Cookie: JSESSIONID=<session-id>
```

#### Invalidate Specific Session
```bash
POST /api/auth/sessions/{sessionId}/invalidate
Cookie: JSESSIONID=<session-id>
```

### Public Endpoints (No authentication required)

#### Health Check
```bash
GET /api/public/health
```

#### Application Info
```bash
GET /api/public/info
```

## Testing Examples

### 1. Test Authentication Flow

```bash
# 1. Login as admin
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' \
  -c cookies.txt

# 2. Access protected endpoint
curl -X GET http://localhost:8080/api/admin/users \
  -b cookies.txt

# 3. Logout
curl -X POST http://localhost:8080/api/auth/logout \
  -b cookies.txt
```

### 2. Test Role-based Access

```bash
# Login as regular user
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","password":"alice123"}' \
  -c user-cookies.txt

# Try to access admin endpoint (should fail)
curl -X GET http://localhost:8080/api/admin/users \
  -b user-cookies.txt

# Access user endpoint (should succeed)
curl -X GET http://localhost:8080/api/user/profile \
  -b user-cookies.txt
```

### 3. Test Session Management

```bash
# Get session info
curl -X GET http://localhost:8080/api/auth/session/info \
  -b cookies.txt

# Get active sessions
curl -X GET http://localhost:8080/api/auth/sessions/active \
  -b cookies.txt
```

## Database Access

### H2 Console

Access the H2 database console at: `http://localhost:8080/api/h2-console`

Connection details:
- JDBC URL: `jdbc:h2:mem:testdb`
- Username: `sa`
- Password: (leave blank)

### Sample Queries

```sql
-- View all users
SELECT * FROM users;

-- View all roles
SELECT * FROM roles;

-- View user-role relationships
SELECT u.username, r.name 
FROM users u 
JOIN user_roles ur ON u.id = ur.user_id 
JOIN roles r ON ur.role_id = r.id;
```

## Redis Session Inspection

Connect to Redis and inspect sessions:

```bash
redis-cli

# List all keys
KEYS *

# View session data
GET "spring:session:sessions:<session-id>"

# List all sessions
KEYS "spring:session:sessions:*"
```

## Security Features

### 1. Password Security
- BCrypt password encoding with strength 12
- Password validation requirements
- Secure password change functionality

### 2. Session Security
- Redis-based distributed session storage
- Session fixation protection
- Concurrent session control (max 1 session per user)
- Session timeout (30 minutes)
- Secure session cookies (HttpOnly, SameSite)

### 3. Access Control
- Role-based authorization
- Method-level security annotations
- Resource-based access control
- Admin protection (can't delete own account)

### 4. CORS Configuration
- Configured for development (localhost origins)
- Credentials support enabled
- Configurable for production environments

## Development

### Project Structure

```
src/
├── main/
│   ├── java/com/example/security/
│   │   ├── config/          # Security and Redis configuration
│   │   ├── controller/      # REST controllers
│   │   ├── dto/            # Data transfer objects
│   │   ├── entity/         # JPA entities
│   │   ├── repository/     # Data repositories
│   │   ├── service/        # Business logic and utilities
│   │   └── SpringSecurityRedisSessionApplication.java
│   └── resources/
│       ├── application.yml  # Application configuration
│       └── data.sql        # Initial data script
└── test/                   # Test classes
```

### Adding New Roles

1. Update the `RoleName` enum in `Role.java`
2. Add the role to `DataInitializationService.java`
3. Update security configuration in `SecurityConfig.java`
4. Add appropriate controller endpoints with `@PreAuthorize` annotations

### Custom Endpoints

To add new endpoints:

1. Create controller class in `controller` package
2. Add appropriate security annotations
3. Update the security configuration if needed
4. Add tests for the new endpoints

## Monitoring and Observability

### Actuator Endpoints

The application includes Spring Boot Actuator for monitoring:

- `/api/actuator/health` - Health information
- `/api/actuator/info` - Application information  
- `/api/actuator/metrics` - Application metrics
- `/api/actuator/sessions` - Session information (admin only)

### Logging

Configure logging levels in `application.yml`:

```yaml
logging:
  level:
    org.springframework.security: DEBUG
    org.springframework.session: DEBUG
    com.example: DEBUG
```

## Production Considerations

### 1. Security Hardening

```yaml
server:
  servlet:
    session:
      cookie:
        secure: true  # Enable for HTTPS
        same-site: strict
        
spring:
  security:
    headers:
      frame-options: deny
      content-type-options: nosniff
```

### 2. Redis Configuration

For production, consider:
- Redis password protection
- Redis clustering
- SSL/TLS encryption
- Connection pooling optimization

### 3. Database Configuration

Replace H2 with a production database:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/myapp
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: validate
```

### 4. Environment Variables

Use environment variables for sensitive configuration:

```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}
```

## Troubleshooting

### Common Issues

1. **Redis Connection Error**
   - Ensure Redis is running on localhost:6379
   - Check Redis logs for connection issues

2. **Session Not Persisting**
   - Verify Redis configuration
   - Check session timeout settings
   - Ensure cookies are enabled in client

3. **Access Denied Errors**
   - Verify user roles and permissions
   - Check security configuration
   - Review authentication state

### Debug Tips

1. Enable debug logging for security components
2. Monitor Redis keys and session data
3. Use H2 console to inspect user data
4. Check browser developer tools for cookie/session issues

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make changes with tests
4. Submit a pull request

## License

This project is licensed under the MIT License - see the LICENSE file for details.
