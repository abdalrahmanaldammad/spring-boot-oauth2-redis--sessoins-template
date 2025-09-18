package com.example.app.email.service;

import com.example.app.user.entity.User;
import com.example.app.email.entity.VerificationToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/** Service for sending emails with templates */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

  private final JavaMailSender mailSender;
  private final TemplateEngine templateEngine;

  @Value("${app.email.from-address}")
  private String fromAddress;

  @Value("${app.email.from-name}")
  private String fromName;

  @Value("${app.email.base-url}")
  private String baseUrl;

  @Value("${app.email.frontend-url}")
  private String frontendUrl;

  /** Send email verification email */
  @Async
  public CompletableFuture<Boolean> sendEmailVerification(User user, VerificationToken token) {
    try {
      String subject = "Verify Your Email Address";

      Map<String, Object> templateModel = new HashMap<>();
      templateModel.put("user", user);
      templateModel.put(
          "verificationUrl", frontendUrl + "/auth/verify-email?token=" + token.getToken());
      templateModel.put("appName", fromName);
      templateModel.put("expirationHours", "24");

      String htmlContent = processTemplate("email-verification", templateModel);
      String textContent = createTextVerificationContent(user, token);

      sendHtmlEmail(user.getEmail(), subject, htmlContent, textContent);

      log.info("Email verification sent to: {}", user.getEmail());
      return CompletableFuture.completedFuture(true);

    } catch (Exception e) {
      log.error("Failed to send email verification to: {}", user.getEmail(), e);
      return CompletableFuture.completedFuture(false);
    }
  }

  /** Send password reset email */
  @Async
  public CompletableFuture<Boolean> sendPasswordReset(User user, VerificationToken token) {
    try {
      String subject = "Reset Your Password";

      Map<String, Object> templateModel = new HashMap<>();
      templateModel.put("user", user);
      templateModel.put("resetUrl", frontendUrl + "/auth/reset-password?token=" + token.getToken());
      templateModel.put("appName", fromName);
      templateModel.put("expirationHours", "2");

      String htmlContent = processTemplate("password-reset", templateModel);
      String textContent = createTextPasswordResetContent(user, token);

      sendHtmlEmail(user.getEmail(), subject, htmlContent, textContent);

      log.info("Password reset email sent to: {}", user.getEmail());
      return CompletableFuture.completedFuture(true);

    } catch (Exception e) {
      log.error("Failed to send password reset email to: {}", user.getEmail(), e);
      return CompletableFuture.completedFuture(false);
    }
  }

  /** Send email change verification email */
  @Async
  public CompletableFuture<Boolean> sendEmailChangeVerification(
      User user, VerificationToken token) {
    try {
      String subject = "Verify Your New Email Address";

      Map<String, Object> templateModel = new HashMap<>();
      templateModel.put("user", user);
      templateModel.put("newEmail", token.getEmail());
      templateModel.put(
          "verificationUrl", frontendUrl + "/auth/verify-email-change?token=" + token.getToken());
      templateModel.put("appName", fromName);
      templateModel.put("expirationHours", "24");

      String htmlContent = processTemplate("email-change-verification", templateModel);
      String textContent = createTextEmailChangeContent(user, token);

      // Send to the NEW email address
      sendHtmlEmail(token.getEmail(), subject, htmlContent, textContent);

      log.info("Email change verification sent to: {}", token.getEmail());
      return CompletableFuture.completedFuture(true);

    } catch (Exception e) {
      log.error("Failed to send email change verification to: {}", token.getEmail(), e);
      return CompletableFuture.completedFuture(false);
    }
  }

  /** Send welcome email after successful verification */
  @Async
  public CompletableFuture<Boolean> sendWelcomeEmail(User user) {
    try {
      String subject = "Welcome to " + fromName + "!";

      Map<String, Object> templateModel = new HashMap<>();
      templateModel.put("user", user);
      templateModel.put("appName", fromName);
      templateModel.put("loginUrl", frontendUrl + "/auth/login");
      templateModel.put("dashboardUrl", frontendUrl + "/dashboard");

      String htmlContent = processTemplate("welcome", templateModel);
      String textContent = createTextWelcomeContent(user);

      sendHtmlEmail(user.getEmail(), subject, htmlContent, textContent);

      log.info("Welcome email sent to: {}", user.getEmail());
      return CompletableFuture.completedFuture(true);

    } catch (Exception e) {
      log.error("Failed to send welcome email to: {}", user.getEmail(), e);
      return CompletableFuture.completedFuture(false);
    }
  }

  /** Send HTML email with fallback to text */
  private void sendHtmlEmail(String to, String subject, String htmlContent, String textContent)
      throws MessagingException {
    try {
      MimeMessage message = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

      helper.setFrom(fromAddress, fromName);
      helper.setTo(to);
      helper.setSubject(subject);
      helper.setText(textContent, htmlContent);

      mailSender.send(message);

    } catch (Exception e) {
      log.warn("Failed to send HTML email, falling back to text email", e);
      sendSimpleEmail(to, subject, textContent);
    }
  }

  /** Send simple text email */
  private void sendSimpleEmail(String to, String subject, String content) throws MailException {
    SimpleMailMessage message = new SimpleMailMessage();
    message.setFrom(fromAddress);
    message.setTo(to);
    message.setSubject(subject);
    message.setText(content);

    mailSender.send(message);
  }

  /** Process Thymeleaf template */
  private String processTemplate(String templateName, Map<String, Object> variables) {
    try {
      Context context = new Context();
      context.setVariables(variables);
      return templateEngine.process(templateName, context);
    } catch (Exception e) {
      log.warn("Failed to process template: {}, using fallback", templateName, e);
      return createFallbackTemplate(templateName, variables);
    }
  }

  /** Create fallback template when Thymeleaf fails */
  private String createFallbackTemplate(String templateName, Map<String, Object> variables) {
    User user = (User) variables.get("user");
    String appName = (String) variables.get("appName");

    switch (templateName) {
      case "email-verification":
        String verificationUrl = (String) variables.get("verificationUrl");
        return String.format(
            """
                    <html>
                    <body>
                        <h2>Verify Your Email Address</h2>
                        <p>Hi %s,</p>
                        <p>Please click the link below to verify your email address:</p>
                        <p><a href="%s">Verify Email</a></p>
                        <p>This link will expire in 24 hours.</p>
                        <p>Best regards,<br>%s Team</p>
                    </body>
                    </html>
                    """,
            user.getFirstName(), verificationUrl, appName);

      case "password-reset":
        String resetUrl = (String) variables.get("resetUrl");
        return String.format(
            """
                    <html>
                    <body>
                        <h2>Reset Your Password</h2>
                        <p>Hi %s,</p>
                        <p>Click the link below to reset your password:</p>
                        <p><a href="%s">Reset Password</a></p>
                        <p>This link will expire in 2 hours.</p>
                        <p>Best regards,<br>%s Team</p>
                    </body>
                    </html>
                    """,
            user.getFirstName(), resetUrl, appName);

      default:
        return "<html><body><p>Email content not available.</p></body></html>";
    }
  }

  // Text content creation methods
  private String createTextVerificationContent(User user, VerificationToken token) {
    return String.format(
        """
            Hi %s,

            Please click the link below to verify your email address:
            %s/auth/verify-email?token=%s

            This link will expire in 24 hours.

            Best regards,
            %s Team
            """,
        user.getFirstName(), frontendUrl, token.getToken(), fromName);
  }

  private String createTextPasswordResetContent(User user, VerificationToken token) {
    return String.format(
        """
            Hi %s,

            Click the link below to reset your password:
            %s/auth/reset-password?token=%s

            This link will expire in 2 hours.

            If you didn't request this, please ignore this email.

            Best regards,
            %s Team
            """,
        user.getFirstName(), frontendUrl, token.getToken(), fromName);
  }

  private String createTextEmailChangeContent(User user, VerificationToken token) {
    return String.format(
        """
            Hi %s,

            Please click the link below to verify your new email address:
            %s/auth/verify-email-change?token=%s

            New email: %s
            This link will expire in 24 hours.

            Best regards,
            %s Team
            """,
        user.getFirstName(), frontendUrl, token.getToken(), token.getEmail(), fromName);
  }

  private String createTextWelcomeContent(User user) {
    return String.format(
        """
            Hi %s,

            Welcome to %s! Your email has been successfully verified.

            You can now access all features of your account.
            Login here: %s/auth/login

            Best regards,
            %s Team
            """,
        user.getFirstName(), fromName, frontendUrl, fromName);
  }
}
