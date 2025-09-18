package com.example.app.common.exception.security;

import org.springframework.http.HttpStatus;

/**
 * Authorization related exceptions.
 * This is the new, non-deprecated version in the proper package structure.
 */
public class AuthorizationException extends SecurityException {
    
    public AuthorizationException(String message, String errorCode) {
        super(message, errorCode, HttpStatus.FORBIDDEN);
    }
    
    public AuthorizationException(String message, String errorCode, Throwable cause) {
        super(message, errorCode, HttpStatus.FORBIDDEN, cause);
    }
    
    // Common authorization exceptions as static inner classes
    
    /**
     * Exception thrown when access is denied to a resource.
     */
    public static class AccessDeniedException extends AuthorizationException {
        public AccessDeniedException(String message) {
            super(message, "AUTHZ_ACCESS_DENIED");
        }
        
        public AccessDeniedException(String resource, String action) {
            super(String.format("Access denied: Cannot %s %s", action, resource), "AUTHZ_ACCESS_DENIED");
        }
        
        public AccessDeniedException() {
            super("Access denied to the requested resource", "AUTHZ_ACCESS_DENIED");
        }
    }
    
    /**
     * Exception thrown when user has insufficient role.
     */
    public static class InsufficientRoleException extends AuthorizationException {
        public InsufficientRoleException(String requiredRole) {
            super(String.format("Insufficient privileges. Required role: %s", requiredRole), 
                  "AUTHZ_INSUFFICIENT_ROLE");
        }
        
        public InsufficientRoleException(String currentRole, String requiredRole) {
            super(String.format("Insufficient privileges. Current role: %s, Required: %s", currentRole, requiredRole), 
                  "AUTHZ_INSUFFICIENT_ROLE");
        }
    }
    
    /**
     * Exception thrown when user has insufficient permissions.
     */
    public static class InsufficientPermissionException extends AuthorizationException {
        public InsufficientPermissionException(String message) {
            super(message, "AUTHZ_INSUFFICIENT_PERMISSION");
        }
        
        public InsufficientPermissionException(String permission, String resource) {
            super(String.format("Insufficient permission '%s' for resource '%s'", permission, resource), 
                  "AUTHZ_INSUFFICIENT_PERMISSION");
        }
        
        public InsufficientPermissionException() {
            super("Insufficient permissions to perform this action", "AUTHZ_INSUFFICIENT_PERMISSION");
        }
    }
    
    /**
     * Exception thrown when resource ownership is required.
     */
    public static class ResourceOwnershipException extends AuthorizationException {
        public ResourceOwnershipException(String message) {
            super(message, "AUTHZ_OWNERSHIP_REQUIRED");
        }
        
        public ResourceOwnershipException(String resourceType, Object resourceId) {
            super(String.format("You don't have ownership of %s with ID: %s", resourceType, resourceId), 
                  "AUTHZ_OWNERSHIP_REQUIRED");
        }
        
        public ResourceOwnershipException() {
            super("Resource ownership required for this operation", "AUTHZ_OWNERSHIP_REQUIRED");
        }
    }
    
    /**
     * Exception thrown when admin action is required.
     */
    public static class AdminActionRequiredException extends AuthorizationException {
        public AdminActionRequiredException(String message) {
            super(message, "AUTHZ_ADMIN_REQUIRED");
        }
        
        public AdminActionRequiredException() {
            super("This action requires administrative privileges", "AUTHZ_ADMIN_REQUIRED");
        }
        
        public AdminActionRequiredException(String action, boolean isAction) {
            super(String.format("Administrative privileges required to %s", action), "AUTHZ_ADMIN_REQUIRED");
        }
    }
    
    /**
     * Exception thrown when API key is invalid or missing.
     */
    public static class InvalidApiKeyException extends AuthorizationException {
        public InvalidApiKeyException() {
            super("Invalid or missing API key", "AUTHZ_INVALID_API_KEY");
        }
        
        public InvalidApiKeyException(String message) {
            super(message, "AUTHZ_INVALID_API_KEY");
        }
    }
    
    /**
     * Exception thrown when rate limit is exceeded.
     */
    public static class RateLimitExceededException extends AuthorizationException {
        public RateLimitExceededException(int limit, String period) {
            super(String.format("Rate limit exceeded: %d requests per %s", limit, period), 
                  "AUTHZ_RATE_LIMIT_EXCEEDED");
        }
        
        public RateLimitExceededException(String message) {
            super(message, "AUTHZ_RATE_LIMIT_EXCEEDED");
        }
    }
    
    /**
     * Exception thrown when IP is blocked or not whitelisted.
     */
    public static class IpBlockedException extends AuthorizationException {
        public IpBlockedException(String ipAddress) {
            super(String.format("Access denied from IP address: %s", ipAddress), "AUTHZ_IP_BLOCKED");
        }
        
        public IpBlockedException(String ipAddress, String reason) {
            super(String.format("Access denied from IP address %s: %s", ipAddress, reason), "AUTHZ_IP_BLOCKED");
        }
    }
}
