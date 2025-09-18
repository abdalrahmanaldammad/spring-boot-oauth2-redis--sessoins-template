package com.example.app.admin.controller;
import com.example.app.email.entity.TokenType;

import com.example.app.user.dto.UserResponse;
import com.example.app.user.entity.User;
import com.example.app.email.entity.TokenType;
import com.example.app.user.repository.UserRepository;
import com.example.app.auth.service.UserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controller for admin-only operations
 */
@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    
    private final UserRepository userRepository;
    private final SessionRegistry sessionRegistry;
    
    @Autowired
    public AdminController(UserRepository userRepository, SessionRegistry sessionRegistry) {
        this.userRepository = userRepository;
        this.sessionRegistry = sessionRegistry;
    }
    
    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers() {
        List<User> users = userRepository.findAll();
        
        List<UserResponse> userResponses = users.stream()
            .map(this::createUserResponse)
            .collect(Collectors.toList());
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("users", userResponses);
        response.put("totalUsers", users.size());
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/users/{userId}")
    public ResponseEntity<?> getUserById(@PathVariable Long userId) {
        return userRepository.findById(userId)
            .map(user -> {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("user", createUserResponse(user));
                return ResponseEntity.ok(response);
            })
            .orElse(ResponseEntity.notFound().build());
    }
    
    @PutMapping("/users/{userId}/enable")
    public ResponseEntity<?> enableUser(@PathVariable Long userId) {
        return userRepository.findById(userId)
            .map(user -> {
                user.setEnabled(true);
                userRepository.save(user);
                
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "User enabled successfully");
                response.put("user", createUserResponse(user));
                return ResponseEntity.ok(response);
            })
            .orElse(ResponseEntity.notFound().build());
    }
    
    @PutMapping("/users/{userId}/disable")
    public ResponseEntity<?> disableUser(@PathVariable Long userId) {
        return userRepository.findById(userId)
            .map(user -> {
                user.setEnabled(false);
                userRepository.save(user);
                
                // Invalidate all sessions for this user
                List<Object> principals = sessionRegistry.getAllPrincipals();
                for (Object principal : principals) {
                    if (principal instanceof UserPrincipal) {
                        UserPrincipal userPrincipal = (UserPrincipal) principal;
                        if (userPrincipal.getId().equals(userId)) {
                            List<SessionInformation> sessions = sessionRegistry.getAllSessions(principal, false);
                            sessions.forEach(SessionInformation::expireNow);
                        }
                    }
                }
                
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "User disabled successfully");
                response.put("user", createUserResponse(user));
                return ResponseEntity.ok(response);
            })
            .orElse(ResponseEntity.notFound().build());
    }
    
    @PutMapping("/users/{userId}/lock")
    public ResponseEntity<?> lockUser(@PathVariable Long userId) {
        return userRepository.findById(userId)
            .map(user -> {
                user.setAccountNonLocked(false);
                userRepository.save(user);
                
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "User account locked successfully");
                response.put("user", createUserResponse(user));
                return ResponseEntity.ok(response);
            })
            .orElse(ResponseEntity.notFound().build());
    }
    
    @PutMapping("/users/{userId}/unlock")
    public ResponseEntity<?> unlockUser(@PathVariable Long userId) {
        return userRepository.findById(userId)
            .map(user -> {
                user.setAccountNonLocked(true);
                userRepository.save(user);
                
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "User account unlocked successfully");
                response.put("user", createUserResponse(user));
                return ResponseEntity.ok(response);
            })
            .orElse(ResponseEntity.notFound().build());
    }
    
    @DeleteMapping("/users/{userId}")
    public ResponseEntity<?> deleteUser(@PathVariable Long userId, Authentication authentication) {
        UserPrincipal currentUser = (UserPrincipal) authentication.getPrincipal();
        
        // Prevent admin from deleting themselves
        if (currentUser.getId().equals(userId)) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "You cannot delete your own account");
            return ResponseEntity.badRequest().body(response);
        }
        
        return userRepository.findById(userId)
            .map(user -> {
                userRepository.delete(user);
                
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "User deleted successfully");
                return ResponseEntity.ok(response);
            })
            .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/sessions/all")
    public ResponseEntity<?> getAllActiveSessions() {
        List<Object> principals = sessionRegistry.getAllPrincipals();
        
        List<Map<String, Object>> sessionInfos = principals.stream()
            .flatMap(principal -> sessionRegistry.getAllSessions(principal, false).stream())
            .map(session -> {
                Map<String, Object> sessionInfo = new HashMap<>();
                sessionInfo.put("sessionId", session.getSessionId());
                sessionInfo.put("lastRequest", session.getLastRequest());
                sessionInfo.put("expired", session.isExpired());
                
                Object principal = session.getPrincipal();
                if (principal instanceof UserPrincipal) {
                    UserPrincipal userPrincipal = (UserPrincipal) principal;
                    Map<String, Object> userInfo = new HashMap<>();
                    userInfo.put("id", userPrincipal.getId());
                    userInfo.put("username", userPrincipal.getUsername());
                    userInfo.put("email", userPrincipal.getEmail());
                    sessionInfo.put("user", userInfo);
                }
                
                return sessionInfo;
            })
            .collect(Collectors.toList());
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("totalSessions", sessionInfos.size());
        response.put("sessions", sessionInfos);
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/sessions/{sessionId}/invalidate")
    public ResponseEntity<?> invalidateUserSession(@PathVariable String sessionId) {
        List<Object> principals = sessionRegistry.getAllPrincipals();
        
        SessionInformation targetSession = principals.stream()
            .flatMap(principal -> sessionRegistry.getAllSessions(principal, false).stream())
            .filter(session -> session.getSessionId().equals(sessionId))
            .findFirst()
            .orElse(null);
        
        Map<String, Object> response = new HashMap<>();
        
        if (targetSession != null) {
            targetSession.expireNow();
            response.put("success", true);
            response.put("message", "Session invalidated successfully");
        } else {
            response.put("success", false);
            response.put("message", "Session not found");
        }
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/stats")
    public ResponseEntity<?> getSystemStats() {
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.countActiveUsers();
        long enabledUsers = userRepository.findByEnabledTrue().size();
        long disabledUsers = userRepository.findByEnabledFalse().size();
        
        List<Object> principals = sessionRegistry.getAllPrincipals();
        long activeSessions = principals.stream()
            .mapToLong(principal -> sessionRegistry.getAllSessions(principal, false).size())
            .sum();
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", totalUsers);
        stats.put("activeUsers", activeUsers);
        stats.put("enabledUsers", enabledUsers);
        stats.put("disabledUsers", disabledUsers);
        stats.put("activeSessions", activeSessions);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("stats", stats);
        
        return ResponseEntity.ok(response);
    }
    
    private UserResponse createUserResponse(User user) {
        return new UserResponse(
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            user.getFirstName(),
            user.getLastName(),
            user.getEnabled(),
            user.getEmailVerified(),
            user.getCreatedAt(),
            user.getLastLogin(),
            user.getRoles().stream().map(role -> role.getName()).collect(Collectors.toSet())
        );
    }
}
