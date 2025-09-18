# Quick Start Guide

🚀 **This backend is optimized for React frontend integration!**

## Prerequisites

1. **Java 17+** installed
2. **Maven 3.6+** installed  
3. **Redis server** running on localhost:6379
4. **React app** (optional - for frontend integration)

## Starting Redis (Using Docker)

```bash
# Start Redis container
docker run -d --name redis -p 6379:6379 redis:latest

# Verify Redis is running
docker ps | grep redis
```

## Running the Application

1. **Clone/Navigate to the project directory:**
   ```bash
   cd /path/to/spring-security-redis-session
   ```

2. **Build the application:**
   ```bash
   mvn clean compile
   ```

3. **Run the application:**
   ```bash
   mvn spring-boot:run
   ```

4. **Verify the application is running:**
   ```bash
   curl http://localhost:8080/api/public/health
   ```

## Test Authentication

### 1. Login as Admin
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' \
  -c cookies.txt
```

### 2. Access Admin Endpoint
```bash
curl -X GET http://localhost:8080/api/admin/users \
  -b cookies.txt
```

### 3. Get Session Info
```bash
curl -X GET http://localhost:8080/api/auth/session/info \
  -b cookies.txt
```

### 4. Logout
```bash
curl -X POST http://localhost:8080/api/auth/logout \
  -b cookies.txt
```

## Available Test Accounts

| Username  | Password    | Roles                    |
|-----------|-------------|--------------------------|
| admin     | admin123    | ROLE_ADMIN               |
| manager   | manager123  | ROLE_MANAGER, ROLE_USER  |
| moderator | moderator123| ROLE_MODERATOR, ROLE_USER|
| alice     | alice123    | ROLE_USER                |
| bob       | bob123      | ROLE_USER                |

## Key Endpoints

- **Application Base:** `http://localhost:8080/api`
- **Health Check:** `http://localhost:8080/api/public/health`
- **H2 Console:** `http://localhost:8080/api/h2-console`
- **Login:** `POST /api/auth/login`
- **Register:** `POST /api/auth/register`
- **User Profile:** `GET /api/user/profile`
- **Admin Users:** `GET /api/admin/users`

## React Frontend Integration

### Quick React Setup

```bash
# In a new terminal, create React app
npx create-react-app my-frontend
cd my-frontend
npm install axios
npm start  # Starts on http://localhost:3000
```

### Test React-Backend Communication

```javascript
// Test authentication status (React-friendly endpoint)
fetch('http://localhost:8080/api/auth/status', {
  credentials: 'include'
})
.then(response => response.json())
.then(data => console.log(data));
```

👉 **For complete React integration guide, see [REACT_INTEGRATION.md](REACT_INTEGRATION.md)**

## Next Steps

1. **React Integration:** Follow the [REACT_INTEGRATION.md](REACT_INTEGRATION.md) guide
2. **Explore the API:** Check the full README.md for comprehensive API documentation
3. **Monitor Sessions:** Use Redis CLI to inspect session data
4. **Database Access:** Use H2 console to view user/role data
5. **Customize:** Modify the code to fit your specific requirements

## Troubleshooting

- **Redis Connection Issues:** Ensure Redis is running on port 6379
- **Session Problems:** Check Redis keys with `redis-cli KEYS "*"`
- **Database Issues:** Access H2 console at `/api/h2-console`
- **Port Conflicts:** Change server port in `application.yml`

For detailed documentation, see the full [README.md](README.md) file.
