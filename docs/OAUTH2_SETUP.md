# OAuth2 Integration Guide

## Overview

Your Spring Boot application now supports OAuth2 authentication with Google and GitHub, while maintaining your existing Redis session-based authentication system. Users can:

- Register/login with username/password (existing functionality)
- Login with Google OAuth2
- Login with GitHub OAuth2
- Link multiple authentication methods to the same account
- Maintain consistent session management across all authentication types

## Prerequisites

1. **Google OAuth2 Setup**
2. **GitHub OAuth2 Setup**
3. **Environment Variables Configuration**

## Google OAuth2 Setup

### Step 1: Create Google Cloud Project

1. Go to [Google Cloud Console](https://console.cloud.google.com)
2. Create a new project or select existing one
3. Enable the Google+ API or Google Identity API

### Step 2: Create OAuth2 Credentials

1. Go to **APIs & Services** → **Credentials**
2. Click **Create Credentials** → **OAuth 2.0 Client IDs**
3. Configure consent screen if needed
4. Choose **Web application**
5. Add authorized redirect URIs:
   - `http://localhost:8080/api/login/oauth2/code/google` (for development)
   - `https://yourdomain.com/api/login/oauth2/code/google` (for production)

### Step 3: Get Credentials

- Copy the **Client ID** and **Client Secret**

## GitHub OAuth2 Setup

### Step 1: Create GitHub OAuth App

1. Go to GitHub → **Settings** → **Developer settings** → **OAuth Apps**
2. Click **New OAuth App**
3. Fill in the details:
   - **Application name**: Your app name
   - **Homepage URL**: `http://localhost:3000` (or your frontend URL)
   - **Authorization callback URL**: `http://localhost:8080/api/login/oauth2/code/github`

### Step 2: Get Credentials

- Copy the **Client ID** and **Client Secret**

## Environment Variables

Create a `.env` file in your project root or set environment variables:

```bash
# Google OAuth2
GOOGLE_CLIENT_ID=your-google-client-id-here
GOOGLE_CLIENT_SECRET=your-google-client-secret-here

# GitHub OAuth2
GITHUB_CLIENT_ID=your-github-client-id-here
GITHUB_CLIENT_SECRET=your-github-client-secret-here

# OAuth2 Redirect URI (where users go after successful auth)
OAUTH2_REDIRECT_URI=http://localhost:3000/auth/oauth2/redirect
```

## API Endpoints

### OAuth2 Login URLs

**GET** `/api/auth/oauth2/authorization-urls`
```json
{
  "success": true,
  "urls": {
    "google": "/api/oauth2/authorization/google",
    "github": "/api/oauth2/authorization/github"
  }
}
```

### Available Providers

**GET** `/api/auth/oauth2/providers`
```json
{
  "success": true,
  "providers": [
    {
      "name": "google",
      "displayName": "Google",
      "available": true
    },
    {
      "name": "github",
      "displayName": "GitHub", 
      "available": true
    }
  ]
}
```

### Direct OAuth2 Login

To initiate OAuth2 login, redirect users to:
- **Google**: `http://localhost:8080/api/oauth2/authorization/google`
- **GitHub**: `http://localhost:8080/api/oauth2/authorization/github`

## Frontend Integration

### React Example

```jsx
// OAuth2 Login Component
import React, { useState, useEffect } from 'react';

const OAuth2Login = () => {
  const [providers, setProviders] = useState([]);

  useEffect(() => {
    fetch('/api/auth/oauth2/providers')
      .then(res => res.json())
      .then(data => {
        if (data.success) {
          setProviders(data.providers);
        }
      });
  }, []);

  const handleOAuth2Login = (provider) => {
    window.location.href = `/api/oauth2/authorization/${provider}`;
  };

  return (
    <div>
      <h3>Login with:</h3>
      {providers.map(provider => (
        <button 
          key={provider.name}
          onClick={() => handleOAuth2Login(provider.name)}
          className="oauth-button"
        >
          Login with {provider.displayName}
        </button>
      ))}
    </div>
  );
};
```

### OAuth2 Redirect Handler

Create a redirect handler page at `/auth/oauth2/redirect`:

```jsx
// OAuth2RedirectHandler.js
import React, { useEffect } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';

const OAuth2RedirectHandler = () => {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();

  useEffect(() => {
    const success = searchParams.get('success');
    const error = searchParams.get('error');

    if (success === 'true') {
      // Successfully authenticated
      const sessionId = searchParams.get('sessionId');
      const userId = searchParams.get('userId');
      const username = searchParams.get('username');
      
      console.log('OAuth2 login successful:', { sessionId, userId, username });
      
      // Redirect to dashboard or main app
      navigate('/dashboard');
    } else if (error) {
      // Authentication failed
      console.error('OAuth2 login failed:', error);
      navigate('/login?error=' + encodeURIComponent(error));
    } else {
      // Unknown state
      navigate('/login');
    }
  }, [searchParams, navigate]);

  return (
    <div>
      <h3>Processing authentication...</h3>
      <p>Please wait while we complete your login.</p>
    </div>
  );
};

export default OAuth2RedirectHandler;
```

## Authentication Flow

### 1. Traditional Login
1. User submits username/password
2. Spring Security authenticates
3. Session created in Redis
4. User authenticated

### 2. OAuth2 Login
1. User clicks "Login with Google/GitHub"
2. Frontend redirects to `/api/oauth2/authorization/{provider}`
3. User is redirected to OAuth2 provider
4. User authorizes the application
5. Provider redirects back to `/api/login/oauth2/code/{provider}`
6. Spring Security processes the OAuth2 response
7. `CustomOAuth2UserService` handles user creation/update
8. `OAuth2AuthenticationSuccessHandler` creates session
9. User is redirected to frontend with session info

## Database Schema Changes

The User entity now includes:
- `provider` (ENUM): LOCAL, GOOGLE, GITHUB
- `provider_id` (VARCHAR): OAuth2 provider's user ID
- `image_url` (VARCHAR): User's profile image URL
- `password` is now nullable for OAuth2-only users

## Session Management

- **Same Redis sessions** for all authentication methods
- **Session timeout**: 30 minutes (configurable)
- **Maximum sessions per user**: 1 (configurable)
- **Session sharing** between traditional and OAuth2 login

## Security Features

- **CSRF protection** disabled for REST API
- **CORS configured** for React frontend
- **Provider validation** prevents account linking issues
- **Automatic username generation** for OAuth2 users
- **Role assignment** (default: ROLE_USER)

## Testing the Integration

### 1. Start the Application
```bash
mvn spring-boot:run
```

### 2. Test Traditional Login
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"password123"}' \
  -c cookies.txt
```

### 3. Test OAuth2 Endpoints
```bash
# Get OAuth2 providers
curl http://localhost:8080/api/auth/oauth2/providers

# Get OAuth2 URLs  
curl http://localhost:8080/api/auth/oauth2/authorization-urls
```

### 4. Test OAuth2 Login
Open browser and navigate to:
- `http://localhost:8080/api/oauth2/authorization/google`
- `http://localhost:8080/api/oauth2/authorization/github`

## Troubleshooting

### Common Issues

1. **OAuth2 URLs not working**
   - Check environment variables are set
   - Verify redirect URIs in OAuth2 provider settings

2. **CORS errors**
   - Update CORS configuration in `SecurityConfig.java`
   - Check frontend URL is in allowed origins

3. **Session not created**
   - Verify Redis is running
   - Check session configuration in `application.yml`

4. **User creation fails**
   - Ensure ROLE_USER exists in database
   - Check OAuth2 provider returns required fields (email)

### Logs to Check

Enable debug logging for OAuth2:
```yaml
logging:
  level:
    org.springframework.security.oauth2: DEBUG
    org.springframework.security.web.authentication: DEBUG
```

## Production Considerations

1. **Use HTTPS** in production
2. **Set secure cookie flags** (`secure: true`)
3. **Update redirect URIs** to production URLs
4. **Use strong secrets** for OAuth2 client secrets
5. **Configure proper CORS** for production domains
6. **Monitor session usage** and adjust timeout as needed

## Next Steps

- Add more OAuth2 providers (Facebook, Twitter, etc.)
- Implement account linking for existing users
- Add user profile management
- Implement remember-me functionality
- Add audit logging for authentication events
