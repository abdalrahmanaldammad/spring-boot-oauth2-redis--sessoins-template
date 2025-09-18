package com.example.app.auth.controller;

import com.example.app.auth.dto.ForgotPasswordDto;
import com.example.app.auth.dto.ResetPasswordDto;

import com.example.app.auth.dto.LoginRequest;
import com.example.app.auth.dto.RegisterRequest;
import com.example.app.user.dto.UserResponse;
import com.example.app.common.entity.Role;
import com.example.app.user.entity.User;
import com.example.app.common.repository.RoleRepository;
import com.example.app.user.repository.UserRepository;
import com.example.app.auth.service.UserPrincipal;
import com.example.app.email.service.VerificationTokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** REST controller for authentication operations */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthenticationManager authenticationManager;
  private final UserRepository userRepository;
  private final RoleRepository roleRepository;
  private final PasswordEncoder passwordEncoder;
  private final SessionRegistry sessionRegistry;
  private final ClientRegistrationRepository clientRegistrationRepository;
  private final VerificationTokenService verificationTokenService;
  private final FindByIndexNameSessionRepository<? extends Session> sessionRepository;

  @Value("${server.servlet.context-path:/}")
  private String contextPath;

  @PostMapping("/login")
  public ResponseEntity<?> authenticateUser(
      @Valid @RequestBody LoginRequest loginRequest, HttpServletRequest request) {
    try {
      Authentication authentication =
          authenticationManager.authenticate(
              new UsernamePasswordAuthenticationToken(
                  loginRequest.getUsername(), loginRequest.getPassword()));

      SecurityContextHolder.getContext().setAuthentication(authentication);

      // Create session
      HttpSession session = request.getSession(true);
      session.setAttribute(
          HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
          SecurityContextHolder.getContext());

      UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

      Map<String, Object> response = new HashMap<>();
      response.put("success", true);
      response.put("message", "Login successful");
      response.put("sessionId", session.getId());
      response.put("user", createUserResponse(userPrincipal));

      return ResponseEntity.ok(response);

    } catch (AuthenticationException e) {
      Map<String, Object> response = new HashMap<>();
      response.put("success", false);
      response.put("message", "Invalid username or password");
      return ResponseEntity.status(401).body(response);
    }
  }

  @PostMapping("/register")
  public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest registerRequest) {
    Map<String, Object> response = new HashMap<>();

    // Check if username exists
    if (userRepository.existsByUsername(registerRequest.getUsername())) {
      response.put("success", false);
      response.put("message", "Username is already taken");
      return ResponseEntity.badRequest().body(response);
    }

    // Check if email exists
    if (userRepository.existsByEmail(registerRequest.getEmail())) {
      response.put("success", false);
      response.put("message", "Email is already in use");
      return ResponseEntity.badRequest().body(response);
    }

    // Create new user (email unverified by default)
    User user =
        new User(
            registerRequest.getUsername(),
            registerRequest.getEmail(),
            passwordEncoder.encode(registerRequest.getPassword()),
            registerRequest.getFirstName(),
            registerRequest.getLastName());

    // Set email as unverified for local registration
    user.setEmailVerified(false);

    // Assign default role
    Role userRole =
        roleRepository
            .findByName("ROLE_USER")
            .orElseThrow(() -> new RuntimeException("Default role not found"));
    user.getRoles().add(userRole);

    User savedUser = userRepository.save(user);

    // Send verification email
    boolean emailSent = verificationTokenService.generateEmailVerificationToken(savedUser);

    response.put("success", true);
    response.put(
        "message", "User registered successfully. Please check your email to verify your account.");
    response.put("emailSent", emailSent);
    response.put("user", createUserResponse(savedUser));

    return ResponseEntity.ok(response);
  }

  @PostMapping("/logout")
  public ResponseEntity<?> logoutUser(HttpServletRequest request) {
    HttpSession session = request.getSession(false);
    if (session != null) {
      session.invalidate();
    }

    SecurityContextHolder.clearContext();

    Map<String, Object> response = new HashMap<>();
    response.put("success", true);
    response.put("message", "Logout successful");

    return ResponseEntity.ok(response);
  }

  @GetMapping("/me")
  public ResponseEntity<?> getCurrentUser(Authentication authentication) {
    if (authentication == null || !authentication.isAuthenticated()) {
      Map<String, Object> response = new HashMap<>();
      response.put("success", false);
      response.put("message", "User not authenticated");
      response.put("authenticated", false);
      return ResponseEntity.status(401).body(response);
    }

    UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

    Map<String, Object> response = new HashMap<>();
    response.put("success", true);
    response.put("authenticated", true);
    response.put("user", createUserResponse(userPrincipal));

    return ResponseEntity.ok(response);
  }

  /**
   * React-friendly authentication status check Returns authentication status without throwing
   * errors
   */
  @GetMapping("/status")
  public ResponseEntity<?> getAuthStatus(Authentication authentication) {
    Map<String, Object> response = new HashMap<>();

    if (authentication == null
        || !authentication.isAuthenticated()
        || "anonymousUser".equals(authentication.getPrincipal())) {
      response.put("authenticated", false);
      response.put("user", null);
    } else {
      UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
      response.put("authenticated", true);
      response.put("user", createUserResponse(userPrincipal));
    }

    return ResponseEntity.ok(response);
  }

  @GetMapping("/session/info")
  public ResponseEntity<?> getSessionInfo(
      HttpServletRequest request, Authentication authentication) {
    Map<String, Object> response = new HashMap<>();

    HttpSession session = request.getSession(false);
    if (session == null) {
      response.put("success", false);
      response.put("message", "No active session");
      return ResponseEntity.status(401).body(response);
    }

    response.put("success", true);
    response.put("sessionId", session.getId());
    response.put(
        "creationTime",
        LocalDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(session.getCreationTime()),
            java.time.ZoneId.systemDefault()));
    response.put(
        "lastAccessedTime",
        LocalDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(session.getLastAccessedTime()),
            java.time.ZoneId.systemDefault()));
    response.put("maxInactiveInterval", session.getMaxInactiveInterval());

    if (authentication != null && authentication.isAuthenticated()) {
      UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
      response.put("user", createUserResponse(userPrincipal));
    }

    return ResponseEntity.ok(response);
  }

  @GetMapping("/sessions/active")
  public ResponseEntity<?> getActiveSessions(Authentication authentication) {
    if (authentication == null || !authentication.isAuthenticated()) {
      Map<String, Object> response = new HashMap<>();
      response.put("success", false);
      response.put("message", "User not authenticated");
      return ResponseEntity.status(401).body(response);
    }

    UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
    List<SessionInformation> sessions = sessionRegistry.getAllSessions(userPrincipal, false);

    Map<String, Object> response = new HashMap<>();
    response.put("success", true);
    response.put("activeSessions", sessions.size());
    response.put(
        "sessions",
        sessions.stream()
            .map(
                session -> {
                  Map<String, Object> sessionInfo = new HashMap<>();
                  sessionInfo.put("sessionId", session.getSessionId());
                  sessionInfo.put("lastRequest", session.getLastRequest());
                  sessionInfo.put("expired", session.isExpired());
                  return sessionInfo;
                })
            .collect(Collectors.toList()));

    return ResponseEntity.ok(response);
  }

  @PostMapping("/sessions/{sessionId}/invalidate")
  public ResponseEntity<?> invalidateSession(
      @PathVariable String sessionId, Authentication authentication) {
    if (authentication == null || !authentication.isAuthenticated()) {
      Map<String, Object> response = new HashMap<>();
      response.put("success", false);
      response.put("message", "User not authenticated");
      return ResponseEntity.status(401).body(response);
    }

    UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
    List<SessionInformation> sessions = sessionRegistry.getAllSessions(userPrincipal, false);

    SessionInformation targetSession =
        sessions.stream()
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

  /** Get OAuth2 login URLs for Google and GitHub */
  @GetMapping("/oauth2/authorization-urls")
  public ResponseEntity<?> getOAuth2AuthorizationUrls(HttpServletRequest request) {
    Map<String, Object> response = new HashMap<>();
    Map<String, String> urls = new HashMap<>();

    try {
      // Get Google OAuth2 URL
      ClientRegistration googleClient = clientRegistrationRepository.findByRegistrationId("google");
      if (googleClient != null) {
        String googleAuthUrl = contextPath + "/oauth2/authorization/google";
        urls.put("google", googleAuthUrl);
      }

      // Get GitHub OAuth2 URL
      ClientRegistration githubClient = clientRegistrationRepository.findByRegistrationId("github");
      if (githubClient != null) {
        String githubAuthUrl = contextPath + "/oauth2/authorization/github";
        urls.put("github", githubAuthUrl);
      }

      response.put("success", true);
      response.put("urls", urls);

    } catch (Exception e) {
      response.put("success", false);
      response.put("message", "Error retrieving OAuth2 URLs: " + e.getMessage());
    }

    return ResponseEntity.ok(response);
  }

  /** Get available OAuth2 providers */
  @GetMapping("/oauth2/providers")
  public ResponseEntity<?> getOAuth2Providers() {
    Map<String, Object> response = new HashMap<>();

    try {
      List<Map<String, Object>> providers = new ArrayList<>();

      // Google provider info
      ClientRegistration googleClient = clientRegistrationRepository.findByRegistrationId("google");
      if (googleClient != null) {
        Map<String, Object> googleProvider = new HashMap<>();
        googleProvider.put("name", "google");
        googleProvider.put("displayName", "Google");
        googleProvider.put("available", true);
        providers.add(googleProvider);
      }

      // GitHub provider info
      ClientRegistration githubClient = clientRegistrationRepository.findByRegistrationId("github");
      if (githubClient != null) {
        Map<String, Object> githubProvider = new HashMap<>();
        githubProvider.put("name", "github");
        githubProvider.put("displayName", "GitHub");
        githubProvider.put("available", true);
        providers.add(githubProvider);
      }

      response.put("success", true);
      response.put("providers", providers);

    } catch (Exception e) {
      response.put("success", false);
      response.put("message", "Error retrieving OAuth2 providers: " + e.getMessage());
    }

    return ResponseEntity.ok(response);
  }

  /** Verify email with token */
  @GetMapping("/verify-email")
  public ResponseEntity<?> verifyEmail(@RequestParam("token") String token) {
    Map<String, Object> response = new HashMap<>();

    VerificationTokenService.VerificationResult result =
        verificationTokenService.verifyEmailToken(token);

    response.put("success", result.isSuccess());
    response.put("message", result.getMessage());

    if (result.isSuccess()) {
      return ResponseEntity.ok(response);
    } else {
      return ResponseEntity.badRequest().body(response);
    }
  }

  /** Resend email verification */
  @PostMapping("/resend-verification")
  public ResponseEntity<?> resendEmailVerification(Authentication authentication) {
    Map<String, Object> response = new HashMap<>();

    if (authentication == null || !authentication.isAuthenticated()) {
      response.put("success", false);
      response.put("message", "User not authenticated");
      return ResponseEntity.status(401).body(response);
    }

    UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
    User user =
        userRepository
            .findById(userPrincipal.getId())
            .orElseThrow(() -> new RuntimeException("User not found"));

    if (user.getEmailVerified()) {
      response.put("success", false);
      response.put("message", "Email is already verified");
      return ResponseEntity.badRequest().body(response);
    }

    boolean emailSent = verificationTokenService.generateEmailVerificationToken(user);

    response.put("success", emailSent);
    response.put(
        "message",
        emailSent
            ? "Verification email sent successfully"
            : "Failed to send verification email or rate limit exceeded");

    return ResponseEntity.ok(response);
  }

  /** Request password reset */
  @PostMapping("/forgot-password")
  public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordDto forgotPasswordDto) {
    Map<String, Object> response = new HashMap<>();

    boolean result =
        verificationTokenService.generatePasswordResetToken(forgotPasswordDto.getEmail());

    // Always return success to prevent email enumeration attacks
    response.put("success", true);
    response.put("message", "If the email exists, a password reset link has been sent");

    return ResponseEntity.ok(response);
  }

  /** Reset password with token */
  @PostMapping("/reset-password")
  public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordDto resetPasswordDto) {
    Map<String, Object> response = new HashMap<>();

    if (resetPasswordDto.getNewPassword() == null
        || resetPasswordDto.getNewPassword().length() < 8) {
      response.put("success", false);
      response.put("message", "Password must be at least 8 characters long");
      return ResponseEntity.badRequest().body(response);
    }

    VerificationTokenService.VerificationResult result =
        verificationTokenService.verifyPasswordResetToken(
            resetPasswordDto.getToken(), resetPasswordDto.getNewPassword());

    if (result.isSuccess()) {
      User user = result.getUser();
      user.setPassword(passwordEncoder.encode(resetPasswordDto.getNewPassword()));
      userRepository.save(user);

      response.put("success", true);
      response.put("message", "Password reset successfully");
      return ResponseEntity.ok(response);
    } else {
      response.put("success", false);
      response.put("message", result.getMessage());
      return ResponseEntity.badRequest().body(response);
    }
  }

  /** Change email address */
  @PostMapping("/change-email")
  public ResponseEntity<?> changeEmail(
      @RequestParam("newEmail") String newEmail, Authentication authentication) {
    Map<String, Object> response = new HashMap<>();

    if (authentication == null || !authentication.isAuthenticated()) {
      response.put("success", false);
      response.put("message", "User not authenticated");
      return ResponseEntity.status(401).body(response);
    }

    UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
    User user =
        userRepository
            .findById(userPrincipal.getId())
            .orElseThrow(() -> new RuntimeException("User not found"));

    boolean result = verificationTokenService.generateEmailChangeToken(user, newEmail);

    response.put("success", result);
    response.put(
        "message",
        result
            ? "Email change verification sent to new email address"
            : "Failed to send email change verification or rate limit exceeded");

    return ResponseEntity.ok(response);
  }

  /** Verify email change with token */
  @PostMapping("/verify-email-change")
  public ResponseEntity<?> verifyEmailChange(@RequestParam("token") String token) {
    Map<String, Object> response = new HashMap<>();

    VerificationTokenService.VerificationResult result =
        verificationTokenService.verifyEmailChangeToken(token);

    response.put("success", result.isSuccess());
    response.put("message", result.getMessage());

    if (result.isSuccess()) {
      return ResponseEntity.ok(response);
    } else {
      return ResponseEntity.badRequest().body(response);
    }
  }

  private UserResponse createUserResponse(UserPrincipal userPrincipal) {
    Set<String> roles =
        userPrincipal.getAuthorities().stream()
            .map(authority -> authority.getAuthority())
            .collect(Collectors.toSet());

    // For UserPrincipal, we need to fetch the user to get emailVerified status
    User user = userRepository.findById(userPrincipal.getId()).orElse(null);
    boolean emailVerified = user != null ? user.getEmailVerified() : false;

    return new UserResponse(
        userPrincipal.getId(),
        userPrincipal.getUsername(),
        userPrincipal.getEmail(),
        userPrincipal.getFirstName(),
        userPrincipal.getLastName(),
        userPrincipal.isEnabled(),
        emailVerified,
        null, // We don't have createdAt in UserPrincipal
        null, // We don't have lastLogin in UserPrincipal
        roles);
  }

  private UserResponse createUserResponse(User user) {
    Set<String> roles = user.getRoles().stream().map(Role::getName).collect(Collectors.toSet());

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
        roles);
  }
}
