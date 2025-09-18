package com.example.app.common.util;

import com.example.app.auth.service.UserPrincipal;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpSession;
import java.util.Optional;

/**
 * Utility class for common security operations
 */
@Component
public class SecurityUtils {
    
    /**
     * Get the currently authenticated user
     */
    public static Optional<UserPrincipal> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated() || 
            "anonymousUser".equals(authentication.getPrincipal())) {
            return Optional.empty();
        }
        
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserPrincipal) {
            return Optional.of((UserPrincipal) principal);
        }
        
        return Optional.empty();
    }
    
    /**
     * Get the current user's ID
     */
    public static Optional<Long> getCurrentUserId() {
        return getCurrentUser().map(UserPrincipal::getId);
    }
    
    /**
     * Get the current user's username
     */
    public static Optional<String> getCurrentUsername() {
        return getCurrentUser().map(UserPrincipal::getUsername);
    }
    
    /**
     * Check if the current user has a specific role
     */
    public static boolean hasRole(String role) {
        return getCurrentUser()
            .map(user -> user.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals(role)))
            .orElse(false);
    }
    
    /**
     * Check if the current user has any of the specified roles
     */
    public static boolean hasAnyRole(String... roles) {
        if (roles == null || roles.length == 0) {
            return false;
        }
        
        return getCurrentUser()
            .map(user -> {
                for (String role : roles) {
                    if (user.getAuthorities().stream()
                        .anyMatch(authority -> authority.getAuthority().equals(role))) {
                        return true;
                    }
                }
                return false;
            })
            .orElse(false);
    }
    
    /**
     * Check if current user is authenticated
     */
    public static boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated() && 
               !"anonymousUser".equals(authentication.getPrincipal());
    }
    
    /**
     * Check if current user is anonymous
     */
    public static boolean isAnonymous() {
        return !isAuthenticated();
    }
    
    /**
     * Get current authentication object
     */
    public static Optional<Authentication> getCurrentAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return Optional.ofNullable(authentication);
    }
    
    /**
     * Check if current user is the owner of a resource
     */
    public static boolean isOwner(Long resourceOwnerId) {
        return getCurrentUserId()
            .map(currentUserId -> currentUserId.equals(resourceOwnerId))
            .orElse(false);
    }
    
    /**
     * Check if current user is admin or owner of a resource
     */
    public static boolean isAdminOrOwner(Long resourceOwnerId) {
        return hasRole("ROLE_ADMIN") || isOwner(resourceOwnerId);
    }
    
    /**
     * Get session information from session object
     */
    public static class SessionInfo {
        private final String sessionId;
        private final long creationTime;
        private final long lastAccessedTime;
        private final int maxInactiveInterval;
        private final boolean isNew;
        
        public SessionInfo(HttpSession session) {
            this.sessionId = session.getId();
            this.creationTime = session.getCreationTime();
            this.lastAccessedTime = session.getLastAccessedTime();
            this.maxInactiveInterval = session.getMaxInactiveInterval();
            this.isNew = session.isNew();
        }
        
        public String getSessionId() {
            return sessionId;
        }
        
        public long getCreationTime() {
            return creationTime;
        }
        
        public long getLastAccessedTime() {
            return lastAccessedTime;
        }
        
        public int getMaxInactiveInterval() {
            return maxInactiveInterval;
        }
        
        public boolean isNew() {
            return isNew;
        }
        
        @Override
        public String toString() {
            return "SessionInfo{" +
                    "sessionId='" + sessionId + '\'' +
                    ", creationTime=" + creationTime +
                    ", lastAccessedTime=" + lastAccessedTime +
                    ", maxInactiveInterval=" + maxInactiveInterval +
                    ", isNew=" + isNew +
                    '}';
        }
    }
}
