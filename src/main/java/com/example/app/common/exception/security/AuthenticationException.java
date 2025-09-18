package com.example.app.common.exception.security;

import org.springframework.http.HttpStatus;

/**
 * Authentication related exceptions.
 * This is the new, non-deprecated version in the proper package structure.
 */
public class AuthenticationException extends SecurityException {
    
    public AuthenticationException(String message, String errorCode) {
        super(message, errorCode, HttpStatus.UNAUTHORIZED);
    }
    
    public AuthenticationException(String message, String errorCode, Throwable cause) {
        super(message, errorCode, HttpStatus.UNAUTHORIZED, cause);
    }
    
    // Common authentication exceptions as static inner classes
    
    /**
     * Exception thrown when credentials are invalid.
     */
    public static class InvalidCredentialsException extends AuthenticationException {
        public InvalidCredentialsException(String message) {
            super(message, "AUTH_INVALID_CREDENTIALS");
        }
        
        public InvalidCredentialsException() {
            super("Invalid username or password", "AUTH_INVALID_CREDENTIALS");
        }
    }
    
    /**
     * Exception thrown when account is locked.
     */
    public static class AccountLockedException extends AuthenticationException {
        public AccountLockedException(String username, int remainingMinutes) {
            super(String.format("Account '%s' is locked. Try again in %d minutes", username, remainingMinutes), 
                  "AUTH_ACCOUNT_LOCKED");
        }
        
        public AccountLockedException(String message) {
            super(message, "AUTH_ACCOUNT_LOCKED");
        }
    }
    
    /**
     * Exception thrown when account is disabled.
     */
    public static class AccountDisabledException extends AuthenticationException {
        public AccountDisabledException(String username) {
            super(String.format("Account '%s' has been disabled", username), "AUTH_ACCOUNT_DISABLED");
        }
        
        public AccountDisabledException(String username, String reason) {
            super(String.format("Account '%s' has been disabled: %s", username, reason), 
                  "AUTH_ACCOUNT_DISABLED");
        }
    }
    
    /**
     * Exception thrown when account has expired.
     */
    public static class AccountExpiredException extends AuthenticationException {
        public AccountExpiredException(String username) {
            super(String.format("Account '%s' has expired", username), "AUTH_ACCOUNT_EXPIRED");
        }
    }
    
    /**
     * Exception thrown when credentials have expired.
     */
    public static class CredentialsExpiredException extends AuthenticationException {
        public CredentialsExpiredException(String username) {
            super(String.format("Credentials for account '%s' have expired. Please reset your password", username), 
                  "AUTH_CREDENTIALS_EXPIRED");
        }
    }
    
    /**
     * Exception thrown when user is not found.
     */
    public static class UserNotFoundException extends AuthenticationException {
        public UserNotFoundException(String identifier) {
            super(String.format("User '%s' not found", identifier), "AUTH_USER_NOT_FOUND");
        }
    }
    
    /**
     * Exception thrown when token is invalid.
     */
    public static class InvalidTokenException extends AuthenticationException {
        public InvalidTokenException(String tokenType) {
            super(String.format("Invalid %s token", tokenType), "AUTH_INVALID_TOKEN");
        }
        
        public InvalidTokenException() {
            super("Invalid or malformed token", "AUTH_INVALID_TOKEN");
        }
    }
    
    /**
     * Exception thrown when token has expired.
     */
    public static class TokenExpiredException extends AuthenticationException {
        public TokenExpiredException(String tokenType) {
            super(String.format("%s token has expired", tokenType), "AUTH_TOKEN_EXPIRED");
        }
        
        public TokenExpiredException() {
            super("Token has expired", "AUTH_TOKEN_EXPIRED");
        }
    }
    
    /**
     * Exception thrown when session has expired.
     */
    public static class SessionExpiredException extends AuthenticationException {
        public SessionExpiredException() {
            super("Session has expired. Please login again", "AUTH_SESSION_EXPIRED");
        }
        
        public SessionExpiredException(String sessionId) {
            super(String.format("Session '%s' has expired", sessionId), "AUTH_SESSION_EXPIRED");
        }
    }
    
    /**
     * Exception thrown when too many login attempts are made.
     */
    public static class TooManyLoginAttemptsException extends AuthenticationException {
        public TooManyLoginAttemptsException(int attempts, int maxAttempts) {
            super(String.format("Too many login attempts (%d/%d). Account temporarily locked", attempts, maxAttempts), 
                  "AUTH_TOO_MANY_ATTEMPTS");
        }
        
        public TooManyLoginAttemptsException(String message) {
            super(message, "AUTH_TOO_MANY_ATTEMPTS");
        }
    }
    
    /**
     * Exception thrown when two-factor authentication fails.
     */
    public static class TwoFactorAuthenticationException extends AuthenticationException {
        public TwoFactorAuthenticationException(String message) {
            super(message, "AUTH_2FA_FAILED");
        }
        
        public TwoFactorAuthenticationException() {
            super("Two-factor authentication failed", "AUTH_2FA_FAILED");
        }
    }
}
