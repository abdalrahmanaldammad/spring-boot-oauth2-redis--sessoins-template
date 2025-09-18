# React Integration Guide

This guide shows how to integrate the Spring Boot backend with a React frontend application.

## Overview

The backend is optimized for React integration with:
- **REST API only** (no server-side rendering)
- **CORS configured** for React development (port 3000)
- **Session-based authentication** with cookies
- **JSON responses** for all endpoints
- **React-friendly authentication status** endpoint

## Frontend Setup

### 1. Create React App

```bash
npx create-react-app my-app
cd my-app
npm install axios  # For API calls
```

### 2. Configure Axios for Session Cookies

```javascript
// src/api/client.js
import axios from 'axios';

const apiClient = axios.create({
  baseURL: 'http://localhost:8080/api',
  withCredentials: true, // Important: This ensures cookies are sent
  headers: {
    'Content-Type': 'application/json',
  },
});

export default apiClient;
```

## Authentication Implementation

### 1. Authentication Context

```javascript
// src/context/AuthContext.js
import React, { createContext, useContext, useState, useEffect } from 'react';
import apiClient from '../api/client';

const AuthContext = createContext();

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [authenticated, setAuthenticated] = useState(false);

  // Check authentication status on app load
  useEffect(() => {
    checkAuthStatus();
  }, []);

  const checkAuthStatus = async () => {
    try {
      const response = await apiClient.get('/auth/status');
      setAuthenticated(response.data.authenticated);
      setUser(response.data.user);
    } catch (error) {
      setAuthenticated(false);
      setUser(null);
    } finally {
      setLoading(false);
    }
  };

  const login = async (username, password) => {
    try {
      const response = await apiClient.post('/auth/login', {
        username,
        password
      });
      
      if (response.data.success) {
        setAuthenticated(true);
        setUser(response.data.user);
        return { success: true };
      }
    } catch (error) {
      const message = error.response?.data?.message || 'Login failed';
      return { success: false, message };
    }
  };

  const register = async (userData) => {
    try {
      const response = await apiClient.post('/auth/register', userData);
      return { success: true, message: 'Registration successful' };
    } catch (error) {
      const message = error.response?.data?.message || 'Registration failed';
      return { success: false, message };
    }
  };

  const logout = async () => {
    try {
      await apiClient.post('/auth/logout');
      setAuthenticated(false);
      setUser(null);
    } catch (error) {
      console.error('Logout error:', error);
    }
  };

  const value = {
    user,
    authenticated,
    loading,
    login,
    register,
    logout,
    checkAuthStatus
  };

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
};
```

### 2. Protected Route Component

```javascript
// src/components/ProtectedRoute.js
import React from 'react';
import { useAuth } from '../context/AuthContext';

const ProtectedRoute = ({ children, roles = [] }) => {
  const { authenticated, user, loading } = useAuth();

  if (loading) {
    return <div>Loading...</div>;
  }

  if (!authenticated) {
    return <div>Please log in to access this page.</div>;
  }

  // Check role-based access
  if (roles.length > 0 && user?.roles) {
    const hasRequiredRole = roles.some(role => 
      user.roles.includes(role)
    );
    
    if (!hasRequiredRole) {
      return <div>Access denied. Insufficient permissions.</div>;
    }
  }

  return children;
};

export default ProtectedRoute;
```

### 3. Login Component

```javascript
// src/components/Login.js
import React, { useState } from 'react';
import { useAuth } from '../context/AuthContext';

const Login = () => {
  const [credentials, setCredentials] = useState({
    username: '',
    password: ''
  });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  
  const { login } = useAuth();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError('');

    const result = await login(credentials.username, credentials.password);
    
    if (!result.success) {
      setError(result.message);
    }
    
    setLoading(false);
  };

  const handleChange = (e) => {
    setCredentials({
      ...credentials,
      [e.target.name]: e.target.value
    });
  };

  return (
    <form onSubmit={handleSubmit}>
      <h2>Login</h2>
      
      {error && <div style={{ color: 'red' }}>{error}</div>}
      
      <div>
        <input
          type="text"
          name="username"
          placeholder="Username"
          value={credentials.username}
          onChange={handleChange}
          required
        />
      </div>
      
      <div>
        <input
          type="password"
          name="password"
          placeholder="Password"
          value={credentials.password}
          onChange={handleChange}
          required
        />
      </div>
      
      <button type="submit" disabled={loading}>
        {loading ? 'Logging in...' : 'Login'}
      </button>
    </form>
  );
};

export default Login;
```

### 4. Main App Component

```javascript
// src/App.js
import React from 'react';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';
import ProtectedRoute from './components/ProtectedRoute';
import Login from './components/Login';

const Dashboard = () => {
  const { user, logout } = useAuth();
  
  return (
    <div>
      <h2>Dashboard</h2>
      <p>Welcome, {user?.firstName} {user?.lastName}!</p>
      <p>Username: {user?.username}</p>
      <p>Roles: {user?.roles?.join(', ')}</p>
      <button onClick={logout}>Logout</button>
    </div>
  );
};

const AdminPanel = () => {
  const { user } = useAuth();
  
  return (
    <div>
      <h2>Admin Panel</h2>
      <p>Only admins can see this!</p>
      <p>Your roles: {user?.roles?.join(', ')}</p>
    </div>
  );
};

const AppContent = () => {
  const { authenticated, loading } = useAuth();
  
  if (loading) {
    return <div>Loading...</div>;
  }

  return (
    <Router>
      <div className="App">
        <Routes>
          <Route 
            path="/login" 
            element={authenticated ? <Dashboard /> : <Login />} 
          />
          
          <Route 
            path="/dashboard" 
            element={
              <ProtectedRoute>
                <Dashboard />
              </ProtectedRoute>
            } 
          />
          
          <Route 
            path="/admin" 
            element={
              <ProtectedRoute roles={['ROLE_ADMIN']}>
                <AdminPanel />
              </ProtectedRoute>
            } 
          />
          
          <Route 
            path="/" 
            element={authenticated ? <Dashboard /> : <Login />} 
          />
        </Routes>
      </div>
    </Router>
  );
};

function App() {
  return (
    <AuthProvider>
      <AppContent />
    </AuthProvider>
  );
}

export default App;
```

## API Service Examples

### 1. User Service

```javascript
// src/services/userService.js
import apiClient from '../api/client';

export const userService = {
  // Get current user profile
  getProfile: async () => {
    const response = await apiClient.get('/user/profile');
    return response.data;
  },

  // Update user profile
  updateProfile: async (userData) => {
    const response = await apiClient.put('/user/profile', userData);
    return response.data;
  },

  // Change password
  changePassword: async (passwordData) => {
    const response = await apiClient.post('/user/change-password', passwordData);
    return response.data;
  }
};
```

### 2. Admin Service

```javascript
// src/services/adminService.js
import apiClient from '../api/client';

export const adminService = {
  // Get all users (admin only)
  getAllUsers: async () => {
    const response = await apiClient.get('/admin/users');
    return response.data;
  },

  // Enable user
  enableUser: async (userId) => {
    const response = await apiClient.put(`/admin/users/${userId}/enable`);
    return response.data;
  },

  // Disable user
  disableUser: async (userId) => {
    const response = await apiClient.put(`/admin/users/${userId}/disable`);
    return response.data;
  },

  // Get system statistics
  getStats: async () => {
    const response = await apiClient.get('/admin/stats');
    return response.data;
  }
};
```

## Error Handling

### Global Error Handler

```javascript
// src/api/client.js (updated)
import axios from 'axios';

const apiClient = axios.create({
  baseURL: 'http://localhost:8080/api',
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Response interceptor for error handling
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      // Handle unauthorized access
      window.location.href = '/login';
    }
    
    if (error.response?.status === 403) {
      // Handle forbidden access
      alert('Access denied. You do not have permission to perform this action.');
    }
    
    return Promise.reject(error);
  }
);

export default apiClient;
```

## Development Workflow

### 1. Start Backend (Spring Boot)

```bash
# Terminal 1 - Start Redis
docker run -d --name redis -p 6379:6379 redis:latest

# Terminal 2 - Start Spring Boot
cd /path/to/spring-boot
mvn spring-boot:run
```

### 2. Start Frontend (React)

```bash
# Terminal 3 - Start React
cd /path/to/react-app
npm start  # Starts on http://localhost:3000
```

### 3. Test Integration

1. **Login Test:**
   ```bash
   curl -X POST http://localhost:8080/api/auth/login \
     -H "Content-Type: application/json" \
     -d '{"username":"admin","password":"admin123"}' \
     -c cookies.txt
   ```

2. **Status Check:**
   ```bash
   curl -X GET http://localhost:8080/api/auth/status \
     -b cookies.txt
   ```

## Key API Endpoints for React

| Endpoint | Method | Purpose | Authentication |
|----------|--------|---------|---------------|
| `/auth/login` | POST | User login | No |
| `/auth/register` | POST | User registration | No |
| `/auth/logout` | POST | User logout | Yes |
| `/auth/status` | GET | Check auth status (React-friendly) | No |
| `/auth/me` | GET | Get current user | Yes |
| `/user/profile` | GET | Get user profile | Yes (USER+) |
| `/admin/users` | GET | Get all users | Yes (ADMIN) |
| `/public/health` | GET | Health check | No |

## Best Practices

### 1. Session Management
- Use `withCredentials: true` in axios configuration
- Let the backend handle session cookies automatically
- Check authentication status on app initialization

### 2. Error Handling
- Implement global error interceptors
- Handle 401/403 responses appropriately
- Show user-friendly error messages

### 3. Role-Based Access
- Check roles on both frontend and backend
- Use protected route components
- Implement role-based UI rendering

### 4. Development
- Use environment variables for API URLs
- Implement loading states
- Add proper TypeScript types (if using TypeScript)

### 5. Production
- Enable HTTPS and update CORS settings
- Set secure cookie flags
- Implement proper error logging

## Testing the Integration

### Sample Test Users

| Username | Password | Roles | Purpose |
|----------|----------|-------|---------|
| admin | admin123 | ROLE_ADMIN | Test admin features |
| manager | manager123 | ROLE_MANAGER, ROLE_USER | Test manager features |
| alice | alice123 | ROLE_USER | Test regular user features |

### Testing Checklist

- [ ] React app starts on port 3000
- [ ] Login form works with test users
- [ ] Authentication state persists on page reload
- [ ] Protected routes work correctly
- [ ] Role-based access control functions
- [ ] Logout clears session
- [ ] API calls include session cookies
- [ ] Error handling works for 401/403 responses

This integration provides a solid foundation for building a React application with session-based authentication using your Spring Boot backend!
