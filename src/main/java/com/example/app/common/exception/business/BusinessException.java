package com.example.app.common.exception.business;

import com.example.app.common.exception.base.ApplicationException;
import org.springframework.http.HttpStatus;

/**
 * Base exception class for business logic related exceptions.
 * This is the new, non-deprecated version in the proper package structure.
 */
public class BusinessException extends ApplicationException {
    
    public BusinessException(String message, String errorCode, HttpStatus httpStatus) {
        super(message, errorCode, httpStatus);
    }
    
    public BusinessException(String message, String errorCode, HttpStatus httpStatus, Throwable cause) {
        super(message, errorCode, httpStatus, cause);
    }
    
    // Common business exceptions as static inner classes
    
    /**
     * Exception thrown when a resource is not found.
     */
    public static class ResourceNotFoundException extends BusinessException {
        public ResourceNotFoundException(String message) {
            super(message, "RESOURCE_NOT_FOUND", HttpStatus.NOT_FOUND);
        }
        
        public ResourceNotFoundException(String resourceType, Object id) {
            super(String.format("%s with ID '%s' not found", resourceType, id), 
                  "RESOURCE_NOT_FOUND", HttpStatus.NOT_FOUND);
        }
    }
    
    /**
     * Exception thrown when a resource already exists.
     */
    public static class ResourceAlreadyExistsException extends BusinessException {
        public ResourceAlreadyExistsException(String message) {
            super(message, "RESOURCE_ALREADY_EXISTS", HttpStatus.CONFLICT);
        }
        
        public ResourceAlreadyExistsException(String resourceType, String field, Object value) {
            super(String.format("%s with %s '%s' already exists", resourceType, field, value), 
                  "RESOURCE_ALREADY_EXISTS", HttpStatus.CONFLICT);
        }
    }
    
    /**
     * Exception thrown when an operation is invalid.
     */
    public static class InvalidOperationException extends BusinessException {
        public InvalidOperationException(String message) {
            super(message, "INVALID_OPERATION", HttpStatus.BAD_REQUEST);
        }
        
        public InvalidOperationException(String operation, String reason) {
            super(String.format("Invalid operation '%s': %s", operation, reason),
                  "INVALID_OPERATION", HttpStatus.BAD_REQUEST);
        }
    }
    
    /**
     * Exception thrown when validation fails.
     */
    public static class ValidationException extends BusinessException {
        public ValidationException(String message) {
            super(message, "VALIDATION_ERROR", HttpStatus.BAD_REQUEST);
        }
        
        public ValidationException(String field, String value, String requirement) {
            super(String.format("Validation failed for field '%s' with value '%s': %s", 
                               field, value, requirement), 
                  "VALIDATION_ERROR", HttpStatus.BAD_REQUEST);
        }
    }
    
    /**
     * Exception thrown when a service is unavailable.
     */
    public static class ServiceUnavailableException extends BusinessException {
        public ServiceUnavailableException(String serviceName) {
            super(String.format("Service '%s' is currently unavailable", serviceName), 
                  "SERVICE_UNAVAILABLE", HttpStatus.SERVICE_UNAVAILABLE);
        }
        
        public ServiceUnavailableException(String serviceName, String reason) {
            super(String.format("Service '%s' is unavailable: %s", serviceName, reason), 
                  "SERVICE_UNAVAILABLE", HttpStatus.SERVICE_UNAVAILABLE);
        }
    }
    
    /**
     * Exception thrown when there's a configuration error.
     */
    public static class ConfigurationException extends BusinessException {
        public ConfigurationException(String message) {
            super(message, "CONFIGURATION_ERROR", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        
        public ConfigurationException(String message, Throwable cause) {
            super(message, "CONFIGURATION_ERROR", HttpStatus.INTERNAL_SERVER_ERROR, cause);
        }
    }
    
    /**
     * Exception thrown when business rules are violated.
     */
    public static class BusinessRuleViolationException extends BusinessException {
        public BusinessRuleViolationException(String rule, String violation) {
            super(String.format("Business rule '%s' violated: %s", rule, violation),
                  "BUSINESS_RULE_VIOLATION", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        
        public BusinessRuleViolationException(String message) {
            super(message, "BUSINESS_RULE_VIOLATION", HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }
    
    /**
     * Exception thrown when state transition is invalid.
     */
    public static class InvalidStateTransitionException extends BusinessException {
        public InvalidStateTransitionException(String fromState, String toState, String entity) {
            super(String.format("Invalid state transition from '%s' to '%s' for %s", 
                               fromState, toState, entity),
                  "INVALID_STATE_TRANSITION", HttpStatus.CONFLICT);
        }
        
        public InvalidStateTransitionException(String message) {
            super(message, "INVALID_STATE_TRANSITION", HttpStatus.CONFLICT);
        }
    }
    
    /**
     * Exception thrown when an operation times out.
     */
    public static class OperationTimeoutException extends BusinessException {
        public OperationTimeoutException(String operation, long timeoutMs) {
            super(String.format("Operation '%s' timed out after %d ms", operation, timeoutMs),
                  "OPERATION_TIMEOUT", HttpStatus.REQUEST_TIMEOUT);
        }
        
        public OperationTimeoutException(String message) {
            super(message, "OPERATION_TIMEOUT", HttpStatus.REQUEST_TIMEOUT);
        }
    }
}
