package com.example.app.common.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Controller for public endpoints that don't require authentication
 */
@RestController
@RequestMapping("/public")
public class PublicController {
    
    @GetMapping("/health")
    public ResponseEntity<?> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("timestamp", LocalDateTime.now());
        response.put("service", "Spring Security Redis Session Demo");
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/info")
    public ResponseEntity<?> info() {
        Map<String, Object> response = new HashMap<>();
        response.put("application", "Spring Security Redis Session Demo");
        response.put("version", "1.0.0");
        response.put("description", "Demo application showcasing Spring Security with Redis session management");
        response.put("features", new String[]{
            "Spring Security Authentication",
            "Redis Session Storage",
            "Role-based Authorization",
            "H2 Database Integration",
            "Session Management",
            "RESTful APIs"
        });
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/welcome")
    public ResponseEntity<?> welcome() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Welcome to Spring Security Redis Session Demo!");
        response.put("instructions", "Please register or login to access protected resources");
        response.put("endpoints", Map.of(
            "registration", "/api/auth/register",
            "login", "/api/auth/login",
            "public_info", "/api/public/info"
        ));
        
        return ResponseEntity.ok(response);
    }
}
