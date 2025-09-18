# 📧 Production-Grade Email Verification System

## Overview

Your Spring Boot application now features a complete, production-ready email verification system that includes:

- ✅ **Email verification** for new registrations
- 🔐 **Password reset** via email
- 📧 **Email change verification**
- 🚫 **Rate limiting** to prevent spam
- 🎨 **Professional HTML email templates**
- 🔄 **Token expiration and cleanup**
- 🔁 **Resend verification** functionality
- 🛡️ **Security best practices**

## 🔧 Configuration Required

### 1. Email Provider Setup (Gmail Example)

#### Step 1: Enable 2-Factor Authentication
1. Go to your [Google Account settings](https://myaccount.google.com/)
2. Navigate to **Security** → **2-Step Verification**
3. Enable 2-factor authentication

#### Step 2: Generate App Password
1. Go to **Security** → **App passwords**
2. Select **Mail** and your device
3. Copy the generated 16-character password

#### Step 3: Set Environment Variables
```bash
# Gmail SMTP Configuration
EMAIL_HOST=smtp.gmail.com
EMAIL_PORT=587
EMAIL_USERNAME=your-email@gmail.com
EMAIL_PASSWORD=your-16-character-app-password

# Email Settings
EMAIL_FROM=noreply@yourapp.com
EMAIL_FROM_NAME=Your App Name
APP_BASE_URL=http://localhost:8080/api
FRONTEND_URL=http://localhost:3000

# Token Expiration (in hours)
EMAIL_VERIFICATION_EXPIRY=24
PASSWORD_RESET_EXPIRY=2
EMAIL_CHANGE_EXPIRY=24

# Rate Limiting
EMAIL_RATE_LIMIT=5
EMAIL_RATE_LIMIT_DAILY=20
```

### 2. Alternative Email Providers

#### AWS SES
```yaml
spring:
  mail:
    host: email-smtp.us-east-1.amazonaws.com
    port: 587
    username: ${AWS_SES_USERNAME}
    password: ${AWS_SES_PASSWORD}
```

#### SendGrid
```yaml
spring:
  mail:
    host: smtp.sendgrid.net
    port: 587
    username: apikey
    password: ${SENDGRID_API_KEY}
```

#### Mailgun
```yaml
spring:
  mail:
    host: smtp.mailgun.org
    port: 587
    username: ${MAILGUN_USERNAME}
    password: ${MAILGUN_PASSWORD}
```

## 📊 Database Schema Changes

The system adds these new fields to your database:

### User Table
```sql
-- Email verification fields
email_verified BOOLEAN DEFAULT FALSE,
email_verification_sent_at TIMESTAMP,

-- OAuth fields (existing)
provider VARCHAR(50) DEFAULT 'LOCAL',
provider_id VARCHAR(255),
image_url VARCHAR(500),

-- Make password nullable for OAuth users
password VARCHAR(100) NULL
```

### Verification Tokens Table (New)
```sql
CREATE TABLE verification_tokens (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    token VARCHAR(255) UNIQUE NOT NULL,
    token_type VARCHAR(50) NOT NULL,
    user_id BIGINT NOT NULL,
    email VARCHAR(120) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    confirmed_at TIMESTAMP NULL,
    used BOOLEAN DEFAULT FALSE,
    
    FOREIGN KEY (user_id) REFERENCES users(id),
    INDEX idx_token (token),
    INDEX idx_user_type (user_id, token_type),
    INDEX idx_expires_at (expires_at)
);
```

## 🛠️ API Endpoints

### Email Verification

#### Register User (Updated)
**POST** `/api/auth/register`
```json
{
  "username": "johndoe",
  "email": "john@example.com",
  "password": "password123",
  "firstName": "John",
  "lastName": "Doe"
}
```

**Response:**
```json
{
  "success": true,
  "message": "User registered successfully. Please check your email to verify your account.",
  "emailSent": true,
  "user": {
    "id": 1,
    "username": "johndoe",
    "email": "john@example.com",
    "firstName": "John",
    "lastName": "Doe",
    "emailVerified": false,
    "enabled": true,
    "roles": ["ROLE_USER"]
  }
}
```

#### Verify Email
**POST** `/api/auth/verify-email?token=<token>`

**Response:**
```json
{
  "success": true,
  "message": "Email verified successfully"
}
```

#### Resend Verification Email
**POST** `/api/auth/resend-verification`
```json
{
  "success": true,
  "message": "Verification email sent successfully"
}
```

### Password Reset

#### Request Password Reset
**POST** `/api/auth/forgot-password?email=user@example.com`

**Response:**
```json
{
  "success": true,
  "message": "If the email exists, a password reset link has been sent"
}
```

#### Reset Password
**POST** `/api/auth/reset-password?token=<token>&password=newpassword123`

**Response:**
```json
{
  "success": true,
  "message": "Password reset successfully"
}
```

### Email Change

#### Request Email Change
**POST** `/api/auth/change-email?newEmail=newemail@example.com`

**Response:**
```json
{
  "success": true,
  "message": "Email change verification sent to new email address"
}
```

#### Verify Email Change
**POST** `/api/auth/verify-email-change?token=<token>`

**Response:**
```json
{
  "success": true,
  "message": "Email changed successfully"
}
```

## 🎨 Email Templates

The system includes beautiful, responsive HTML email templates:

### Email Verification Template
- **File**: `src/main/resources/templates/email-verification.html`
- **Features**: Modern design, mobile-responsive, security tips
- **Colors**: Blue gradient theme

### Password Reset Template
- **File**: `src/main/resources/templates/password-reset.html`
- **Features**: Security warnings, clear CTA buttons
- **Colors**: Pink/red gradient theme

### Welcome Email Template
- **File**: `src/main/resources/templates/welcome.html`
- **Features**: Feature highlights, action buttons
- **Colors**: Blue/teal gradient theme

### Email Change Template
- **File**: `src/main/resources/templates/email-change-verification.html`
- **Features**: Email address display, security notes
- **Colors**: Pink/yellow gradient theme

## 🔒 Security Features

### Token Security
- **Secure Random Generation**: 32-byte secure random tokens
- **URL-Safe Encoding**: Base64 URL encoding without padding
- **One-Time Use**: Tokens are invalidated after use
- **Expiration**: Configurable expiration times
- **Type Validation**: Tokens are type-specific

### Rate Limiting
- **Per User**: Maximum emails per hour/day
- **Per Email**: Prevents abuse of email addresses
- **Configurable Limits**: Customizable via environment variables

### Email Enumeration Protection
- **Password Reset**: Always returns success regardless of email existence
- **Consistent Responses**: Same response time for existing/non-existing emails

## 🚀 Frontend Integration

### React Example

```jsx
// Email verification component
import React, { useState } from 'react';

const EmailVerification = ({ user }) => {
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState('');

  const resendVerification = async () => {
    setLoading(true);
    try {
      const response = await fetch('/api/auth/resend-verification', {
        method: 'POST',
        credentials: 'include'
      });
      const data = await response.json();
      setMessage(data.message);
    } catch (error) {
      setMessage('Failed to resend email');
    } finally {
      setLoading(false);
    }
  };

  if (user.emailVerified) {
    return (
      <div className="alert alert-success">
        ✅ Your email is verified
      </div>
    );
  }

  return (
    <div className="alert alert-warning">
      <p>⚠️ Please verify your email address</p>
      <p>Check your inbox for a verification email.</p>
      <button 
        onClick={resendVerification} 
        disabled={loading}
        className="btn btn-primary"
      >
        {loading ? 'Sending...' : 'Resend Email'}
      </button>
      {message && <p className="mt-2">{message}</p>}
    </div>
  );
};

// Email verification handler
import React, { useEffect, useState } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';

const VerifyEmailPage = () => {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const [status, setStatus] = useState('verifying');
  const [message, setMessage] = useState('');

  useEffect(() => {
    const token = searchParams.get('token');
    if (!token) {
      setStatus('error');
      setMessage('Invalid verification link');
      return;
    }

    verifyEmail(token);
  }, [searchParams]);

  const verifyEmail = async (token) => {
    try {
      const response = await fetch(`/api/auth/verify-email?token=${token}`, {
        method: 'POST'
      });
      const data = await response.json();
      
      if (data.success) {
        setStatus('success');
        setMessage(data.message);
        setTimeout(() => navigate('/dashboard'), 3000);
      } else {
        setStatus('error');
        setMessage(data.message);
      }
    } catch (error) {
      setStatus('error');
      setMessage('Verification failed. Please try again.');
    }
  };

  return (
    <div className="verify-email-page">
      {status === 'verifying' && (
        <div className="text-center">
          <div className="spinner"></div>
          <p>Verifying your email...</p>
        </div>
      )}

      {status === 'success' && (
        <div className="alert alert-success text-center">
          <h2>✅ Email Verified!</h2>
          <p>{message}</p>
          <p>Redirecting to dashboard...</p>
        </div>
      )}

      {status === 'error' && (
        <div className="alert alert-danger text-center">
          <h2>❌ Verification Failed</h2>
          <p>{message}</p>
          <button 
            onClick={() => navigate('/login')} 
            className="btn btn-primary"
          >
            Go to Login
          </button>
        </div>
      )}
    </div>
  );
};
```

## 🧪 Testing

### 1. Test Email Configuration
```bash
# Test email sending
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "your-email@gmail.com",
    "password": "password123",
    "firstName": "Test",
    "lastName": "User"
  }'
```

### 2. Test Email Verification
```bash
# Verify email (replace TOKEN with actual token from email)
curl -X POST "http://localhost:8080/api/auth/verify-email?token=TOKEN"
```

### 3. Test Password Reset
```bash
# Request password reset
curl -X POST "http://localhost:8080/api/auth/forgot-password?email=your-email@gmail.com"

# Reset password (replace TOKEN)
curl -X POST "http://localhost:8080/api/auth/reset-password?token=TOKEN&password=newpassword123"
```

## 📋 Production Deployment Checklist

### Email Configuration
- [ ] Set up production email service (AWS SES, SendGrid, etc.)
- [ ] Configure DNS records (SPF, DKIM, DMARC)
- [ ] Set up email delivery monitoring
- [ ] Test email deliverability

### Security
- [ ] Use HTTPS in production
- [ ] Set secure email credentials
- [ ] Configure proper CORS settings
- [ ] Set up rate limiting at load balancer level
- [ ] Monitor for email abuse

### Performance
- [ ] Set up email queue for high volume
- [ ] Configure database indexes
- [ ] Set up token cleanup jobs
- [ ] Monitor email sending performance

### Monitoring
- [ ] Set up email delivery alerts
- [ ] Monitor bounce rates
- [ ] Track verification rates
- [ ] Set up error alerting

## 🐛 Troubleshooting

### Common Issues

#### Emails Not Sending
1. **Check SMTP Credentials**
   - Verify username/password
   - Ensure app password for Gmail
   - Check network connectivity

2. **Authentication Issues**
   - Enable "Less secure apps" for some providers
   - Use OAuth2 for Gmail (recommended)
   - Check 2FA settings

#### Emails Going to Spam
1. **DNS Configuration**
   - Set up SPF record: `v=spf1 include:_spf.google.com ~all`
   - Configure DKIM signing
   - Set up DMARC policy

2. **Email Content**
   - Avoid spam trigger words
   - Include unsubscribe link
   - Use professional sender address

#### High Bounce Rate
1. **Email Validation**
   - Implement client-side email validation
   - Use email verification services
   - Monitor bounce reports

### Debug Logging
Enable debug logging for email issues:
```yaml
logging:
  level:
    org.springframework.mail: DEBUG
    org.springframework.scheduling: DEBUG
    com.example.security.service: DEBUG
```

## 🔄 Future Enhancements

### Planned Features
- [ ] Email templates customization UI
- [ ] Bulk email operations
- [ ] Email analytics dashboard
- [ ] SMS verification fallback
- [ ] Social login integration
- [ ] Multi-language email templates

### Advanced Security
- [ ] Device-based verification
- [ ] IP-based rate limiting
- [ ] Suspicious activity detection
- [ ] Account recovery questions

## 📞 Support

For issues with the email verification system:

1. Check the logs for error messages
2. Verify email provider settings
3. Test with different email addresses
4. Check spam folders
5. Review rate limiting settings

The system is designed to be production-ready with proper error handling, logging, and security measures in place.
