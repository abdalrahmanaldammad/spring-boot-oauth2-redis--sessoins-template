package com.example.app.user.controller;

import com.example.app.common.exception.domain.user.UserException;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/** Controller for user-level operations */
@RestController
@RequestMapping("/user")
@PreAuthorize("hasRole('USER')")
public class UserController {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  @Autowired
  public UserController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
  }

  @GetMapping("/profile")
  public ResponseEntity<?> getProfile(Authentication authentication) {
    UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

    User user =
        userRepository
            .findById(userPrincipal.getId())
            .orElseThrow(() -> new UserException.UserNotFoundException(userPrincipal.getId()));

    Map<String, Object> response = new HashMap<>();
    response.put("success", true);
    response.put("user", createUserResponse(user));

    return ResponseEntity.ok(response);
  }

  @PutMapping("/profile")
  public ResponseEntity<?> updateProfile(
      @RequestBody Map<String, String> updateRequest, Authentication authentication) {
    UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

    User user =
        userRepository
            .findById(userPrincipal.getId())
            .orElseThrow(() -> new RuntimeException("User not found"));

    Map<String, Object> response = new HashMap<>();

    // Update allowed fields
    if (updateRequest.containsKey("firstName") && updateRequest.get("firstName") != null) {
      user.setFirstName(updateRequest.get("firstName"));
    }

    if (updateRequest.containsKey("lastName") && updateRequest.get("lastName") != null) {
      user.setLastName(updateRequest.get("lastName"));
    }

    if (updateRequest.containsKey("email") && updateRequest.get("email") != null) {
      String newEmail = updateRequest.get("email");
      // Check if email is already taken by another user
      if (userRepository.existsByEmail(newEmail) && !user.getEmail().equals(newEmail)) {
        response.put("success", false);
        response.put("message", "Email is already in use");
        return ResponseEntity.badRequest().body(response);
      }
      user.setEmail(newEmail);
    }

    User savedUser = userRepository.save(user);

    response.put("success", true);
    response.put("message", "Profile updated successfully");
    response.put("user", createUserResponse(savedUser));

    return ResponseEntity.ok(response);
  }

  @PostMapping("/change-password")
  public ResponseEntity<?> changePassword(
      @RequestBody Map<String, String> passwordRequest, Authentication authentication) {
    UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

    User user =
        userRepository
            .findById(userPrincipal.getId())
            .orElseThrow(() -> new RuntimeException("User not found"));

    Map<String, Object> response = new HashMap<>();

    String currentPassword = passwordRequest.get("currentPassword");
    String newPassword = passwordRequest.get("newPassword");

    if (currentPassword == null || newPassword == null) {
      response.put("success", false);
      response.put("message", "Current password and new password are required");
      return ResponseEntity.badRequest().body(response);
    }

    // Verify current password
    if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
      response.put("success", false);
      response.put("message", "Current password is incorrect");
      return ResponseEntity.badRequest().body(response);
    }

    // Validate new password
    if (newPassword.length() < 6) {
      response.put("success", false);
      response.put("message", "New password must be at least 6 characters long");
      return ResponseEntity.badRequest().body(response);
    }

    // Update password
    user.setPassword(passwordEncoder.encode(newPassword));
    userRepository.save(user);

    response.put("success", true);
    response.put("message", "Password changed successfully");

    return ResponseEntity.ok(response);
  }

  @GetMapping("/dashboard")
  public ResponseEntity<?> getDashboard(Authentication authentication) {
    UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

    Map<String, Object> dashboardData = new HashMap<>();
    dashboardData.put(
        "welcomeMessage", "Welcome to your dashboard, " + userPrincipal.getFullName() + "!");
    dashboardData.put(
        "lastLogin", userPrincipal.toString()); // This would typically show last login time
    dashboardData.put("accountStatus", "Active");
    dashboardData.put(
        "roles",
        userPrincipal.getAuthorities().stream()
            .map(authority -> authority.getAuthority())
            .collect(Collectors.toList()));

    Map<String, Object> response = new HashMap<>();
    response.put("success", true);
    response.put("dashboard", dashboardData);

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
        user.getRoles().stream().map(role -> role.getName()).collect(Collectors.toSet()));
  }
}
