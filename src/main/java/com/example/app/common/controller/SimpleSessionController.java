package com.example.app.common.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

/**
 * Simple session controller that works without complex Redis session registry
 * This is the approach most real-world projects use
 */
@RestController
@RequestMapping("/auth")
public class SimpleSessionController {

    @GetMapping("/session/current")
    public ResponseEntity<?> getCurrentSession(
            HttpServletRequest request, 
            Authentication authentication) {
        
        Map<String, Object> response = new HashMap<>();
        
        HttpSession session = request.getSession(false);
        if (session == null) {
            response.put("success", false);
            response.put("message", "No active session");
            return ResponseEntity.status(401).body(response);
        }

        // Basic session info - always available
        Map<String, Object> sessionInfo = new HashMap<>();
        sessionInfo.put("sessionId", session.getId());
        sessionInfo.put("creationTime", LocalDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(session.getCreationTime()),
            ZoneId.systemDefault()));
        sessionInfo.put("lastAccessTime", LocalDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(session.getLastAccessedTime()),
            ZoneId.systemDefault()));
        sessionInfo.put("maxInactiveInterval", session.getMaxInactiveInterval());
        sessionInfo.put("isNew", session.isNew());

        response.put("success", true);
        response.put("session", sessionInfo);
        response.put("authenticated", authentication != null && authentication.isAuthenticated());
        
        if (authentication != null && authentication.isAuthenticated()) {
            response.put("user", authentication.getPrincipal());
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping("/session/stats")
    public ResponseEntity<?> getSessionStats(HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        
        HttpSession session = request.getSession(false);
        if (session == null) {
            response.put("success", false);
            response.put("message", "No active session");
            return ResponseEntity.status(401).body(response);
        }

        // Calculate session duration
        long creationTime = session.getCreationTime();
        long lastAccessTime = session.getLastAccessedTime();
        long currentTime = System.currentTimeMillis();
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("sessionDuration", (currentTime - creationTime) / 1000); // seconds
        stats.put("idleTime", (currentTime - lastAccessTime) / 1000); // seconds
        stats.put("timeUntilExpiry", session.getMaxInactiveInterval() - 
                                   ((currentTime - lastAccessTime) / 1000));
        
        response.put("success", true);
        response.put("stats", stats);
        
        return ResponseEntity.ok(response);
    }
}
