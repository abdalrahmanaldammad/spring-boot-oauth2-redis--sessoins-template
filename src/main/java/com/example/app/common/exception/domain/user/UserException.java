package com.example.app.common.exception.domain.user;

import com.example.app.common.exception.domain.DomainException;
import org.springframework.http.HttpStatus;

/**
 * Base exception class for user domain-related exceptions.
 * All user-specific exceptions should extend from this class.
 */
public class UserException extends DomainException {
    
    public UserException(String message, String errorCode, HttpStatus httpStatus) {
        super(message, errorCode, httpStatus);
    }
    
    public UserException(String message, String errorCode, HttpStatus httpStatus, Throwable cause) {
        super(message, errorCode, httpStatus, cause);
    }
    
    // Common user exceptions as static inner classes
    
    /**
     * Exception thrown when a user is not found.
     */
    public static class UserNotFoundException extends UserException {
        public UserNotFoundException(Long userId) {
            super(String.format("User with ID %d not found", userId), 
                  "USER_NOT_FOUND", HttpStatus.NOT_FOUND);
        }
        
        public UserNotFoundException(String username) {
            super(String.format("User with username '%s' not found", username), 
                  "USER_NOT_FOUND", HttpStatus.NOT_FOUND);
        }
    }
    
    /**
     * Exception thrown when attempting to create a user that already exists.
     */
    public static class UserAlreadyExistsException extends UserException {
        public UserAlreadyExistsException(String field, String value) {
            super(String.format("User with %s '%s' already exists", field, value),
                  "USER_ALREADY_EXISTS", HttpStatus.CONFLICT);
        }
    }
    
    /**
     * Exception thrown when profile update fails.
     */
    public static class ProfileUpdateException extends UserException {
        public ProfileUpdateException(String field, String reason) {
            super(String.format("Failed to update profile field '%s': %s", field, reason),
                  "PROFILE_UPDATE_FAILED", HttpStatus.BAD_REQUEST);
        }
    }
    
    /**
     * Exception thrown when account is suspended.
     */
    public static class AccountSuspendedException extends UserException {
        public AccountSuspendedException(String username, String reason) {
            super(String.format("Account '%s' is suspended: %s", username, reason),
                  "ACCOUNT_SUSPENDED", HttpStatus.FORBIDDEN);
        }
    }
    
    /**
     * Exception thrown when password requirements are not met.
     */
    public static class WeakPasswordException extends UserException {
        public WeakPasswordException(String requirements) {
            super(String.format("Password does not meet requirements: %s", requirements),
                  "WEAK_PASSWORD", HttpStatus.BAD_REQUEST);
        }
    }
}
