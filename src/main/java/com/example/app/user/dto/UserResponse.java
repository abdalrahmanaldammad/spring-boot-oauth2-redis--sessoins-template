package com.example.app.user.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * DTO for user response (excluding sensitive information)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {
    
    private Long id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private boolean enabled;
    private boolean emailVerified;
    private LocalDateTime createdAt;
    private LocalDateTime lastLogin;
    private Set<String> roles;
    
    // Custom method for full name
    public String getFullName() {
        return firstName + " " + lastName;
    }
}
