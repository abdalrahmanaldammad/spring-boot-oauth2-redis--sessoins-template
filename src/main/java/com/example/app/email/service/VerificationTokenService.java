package com.example.app.email.service;

import com.example.app.email.entity.TokenType;

import com.example.app.user.entity.User;
import com.example.app.email.entity.VerificationToken;
import com.example.app.user.repository.UserRepository;
import com.example.app.email.repository.VerificationTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

/** Service for managing verification tokens */
@Slf4j
@Service
@RequiredArgsConstructor
public class VerificationTokenService {

  private final VerificationTokenRepository tokenRepository;
  private final UserRepository userRepository;
  private final EmailService emailService;

  private static final SecureRandom secureRandom = new SecureRandom();
  private static final int TOKEN_LENGTH = 32;

  @Value("${app.email.verification-expiry}")
  private int verificationExpiryHours;

  @Value("${app.email.password-reset-expiry}")
  private int passwordResetExpiryHours;

  @Value("${app.email.email-change-expiry}")
  private int emailChangeExpiryHours;

  @Value("${app.email.rate-limit.max-emails-per-hour}")
  private int maxEmailsPerHour;

  @Value("${app.email.rate-limit.max-emails-per-day}")
  private int maxEmailsPerDay;

  /** Generate and send email verification token */
  @Transactional
  public boolean generateEmailVerificationToken(User user) {
    if (user.getEmailVerified()) {
      log.warn(
          "Attempted to send verification email to already verified user: {}", user.getEmail());
      return false;
    }

    if (!isWithinRateLimit(user, TokenType.EMAIL_VERIFICATION)) {
      log.warn("Rate limit exceeded for email verification: {}", user.getEmail());
      return false;
    }

    // Invalidate existing tokens
    tokenRepository.invalidateTokensForUser(user, TokenType.EMAIL_VERIFICATION);

    // Create new token
    VerificationToken token =
        createToken(user, user.getEmail(), TokenType.EMAIL_VERIFICATION, verificationExpiryHours);
    tokenRepository.save(token);

    // Update user's last verification sent time
    user.setEmailVerificationSentAt(LocalDateTime.now());
    userRepository.save(user);

    // Send email asynchronously
    emailService.sendEmailVerification(user, token);

    log.info("Email verification token generated for user: {}", user.getEmail());
    return true;
  }

  /** Generate and send password reset token */
  @Transactional
  public boolean generatePasswordResetToken(String email) {
    Optional<User> userOptional = userRepository.findByEmail(email);
    if (userOptional.isEmpty()) {
      log.warn("Password reset requested for non-existent email: {}", email);
      // Don't reveal that the email doesn't exist
      return true;
    }

    User user = userOptional.get();

    if (!isWithinRateLimit(user, TokenType.PASSWORD_RESET)) {
      log.warn("Rate limit exceeded for password reset: {}", email);
      return false;
    }

    // Invalidate existing tokens
    tokenRepository.invalidateTokensForUser(user, TokenType.PASSWORD_RESET);

    // Create new token
    VerificationToken token =
        createToken(user, user.getEmail(), TokenType.PASSWORD_RESET, passwordResetExpiryHours);
    tokenRepository.save(token);

    // Send email asynchronously
    emailService.sendPasswordReset(user, token);

    log.info("Password reset token generated for user: {}", user.getEmail());
    return true;
  }

  /** Generate and send email change verification token */
  @Transactional
  public boolean generateEmailChangeToken(User user, String newEmail) {
    if (user.getEmail().equals(newEmail)) {
      log.warn("Attempted to change email to same email: {}", newEmail);
      return false;
    }

    if (userRepository.existsByEmail(newEmail)) {
      log.warn("Attempted to change email to existing email: {}", newEmail);
      return false;
    }

    if (!isWithinRateLimit(user, TokenType.EMAIL_CHANGE)) {
      log.warn("Rate limit exceeded for email change: {}", user.getEmail());
      return false;
    }

    // Invalidate existing tokens
    tokenRepository.invalidateTokensForUser(user, TokenType.EMAIL_CHANGE);

    // Create new token
    VerificationToken token =
        createToken(user, newEmail, TokenType.EMAIL_CHANGE, emailChangeExpiryHours);
    tokenRepository.save(token);

    // Send email to new address asynchronously
    emailService.sendEmailChangeVerification(user, token);

    log.info("Email change token generated for user: {} -> {}", user.getEmail(), newEmail);
    return true;
  }

  /** Verify email verification token */
  @Transactional
  public VerificationResult verifyEmailToken(String tokenString) {
    Optional<VerificationToken> tokenOpt =
        tokenRepository.findValidToken(tokenString, LocalDateTime.now());

    if (tokenOpt.isEmpty()) {
      return new VerificationResult(false, "Invalid or expired token");
    }

    VerificationToken token = tokenOpt.get();

    if (token.getTokenType() != TokenType.EMAIL_VERIFICATION) {
      return new VerificationResult(false, "Invalid token type");
    }

    User user = token.getUser();

    // Mark token as used
    token.confirm();
    tokenRepository.save(token);

    // Mark user's email as verified
    user.setEmailVerified(true);
    userRepository.save(user);

    // Send welcome email
    emailService.sendWelcomeEmail(user);

    log.info("Email verification successful for user: {}", user.getEmail());
    return new VerificationResult(true, "Email verified successfully");
  }

  /** Verify password reset token */
  @Transactional
  public VerificationResult verifyPasswordResetToken(String tokenString, String newPassword) {
    Optional<VerificationToken> tokenOpt =
        tokenRepository.findValidToken(tokenString, LocalDateTime.now());

    if (tokenOpt.isEmpty()) {
      return new VerificationResult(false, "Invalid or expired token");
    }

    VerificationToken token = tokenOpt.get();

    if (token.getTokenType() != TokenType.PASSWORD_RESET) {
      return new VerificationResult(false, "Invalid token type");
    }

    User user = token.getUser();

    // Mark token as used
    token.confirm();
    tokenRepository.save(token);

    log.info("Password reset token verification successful for user: {}", user.getEmail());
    return new VerificationResult(true, "Token verified successfully", user);
  }

  /** Verify email change token */
  @Transactional
  public VerificationResult verifyEmailChangeToken(String tokenString) {
    Optional<VerificationToken> tokenOpt =
        tokenRepository.findValidToken(tokenString, LocalDateTime.now());

    if (tokenOpt.isEmpty()) {
      return new VerificationResult(false, "Invalid or expired token");
    }

    VerificationToken token = tokenOpt.get();

    if (token.getTokenType() != TokenType.EMAIL_CHANGE) {
      return new VerificationResult(false, "Invalid token type");
    }

    User user = token.getUser();
    String oldEmail = user.getEmail();
    String newEmail = token.getEmail();

    // Check if new email is still available
    if (userRepository.existsByEmail(newEmail)) {
      return new VerificationResult(false, "Email address is no longer available");
    }

    // Mark token as used
    token.confirm();
    tokenRepository.save(token);

    // Update user's email
    user.setEmail(newEmail);
    user.setEmailVerified(true); // New email is verified
    userRepository.save(user);

    log.info("Email change successful for user: {} -> {}", oldEmail, newEmail);
    return new VerificationResult(true, "Email changed successfully");
  }

  /** Check if user is within rate limit */
  private boolean isWithinRateLimit(User user, TokenType tokenType) {
    LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
    LocalDateTime oneDayAgo = LocalDateTime.now().minusDays(1);

    long hourlyCount = tokenRepository.countTokensCreatedSince(user, tokenType, oneHourAgo);
    long dailyCount = tokenRepository.countTokensCreatedSince(user, tokenType, oneDayAgo);

    return hourlyCount < maxEmailsPerHour && dailyCount < maxEmailsPerDay;
  }

  /** Create verification token */
  private VerificationToken createToken(
      User user, String email, TokenType tokenType, int expiryHours) {
    String tokenString = generateSecureToken();
    LocalDateTime expiresAt = LocalDateTime.now().plusHours(expiryHours);

    return VerificationToken.builder()
        .token(tokenString)
        .tokenType(tokenType)
        .user(user)
        .email(email)
        .expiresAt(expiresAt)
        .used(false)
        .build();
  }

  /** Generate secure random token */
  private String generateSecureToken() {
    byte[] randomBytes = new byte[TOKEN_LENGTH];
    secureRandom.nextBytes(randomBytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
  }

  /** Cleanup expired tokens - runs every hour */
  @Scheduled(fixedRate = 3600000) // 1 hour in milliseconds
  @Transactional
  public void cleanupExpiredTokens() {
    int deletedCount = tokenRepository.deleteExpiredTokens(LocalDateTime.now());
    if (deletedCount > 0) {
      log.info("Cleaned up {} expired verification tokens", deletedCount);
    }
  }

  /** Cleanup old used tokens - runs daily at midnight */
  @Scheduled(cron = "0 0 0 * * *")
  @Transactional
  public void cleanupOldUsedTokens() {
    // Delete used tokens older than 30 days
    LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30);
    int deletedCount = tokenRepository.deleteUsedTokensOlderThan(cutoffDate);
    if (deletedCount > 0) {
      log.info("Cleaned up {} old used verification tokens", deletedCount);
    }
  }

  /** Result class for verification operations */
  public static class VerificationResult {
    private final boolean success;
    private final String message;
    private final User user;

    public VerificationResult(boolean success, String message) {
      this(success, message, null);
    }

    public VerificationResult(boolean success, String message, User user) {
      this.success = success;
      this.message = message;
      this.user = user;
    }

    public boolean isSuccess() {
      return success;
    }

    public String getMessage() {
      return message;
    }

    public User getUser() {
      return user;
    }
  }
}
