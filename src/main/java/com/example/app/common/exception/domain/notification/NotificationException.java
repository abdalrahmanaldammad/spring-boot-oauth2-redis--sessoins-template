package com.example.app.common.exception.domain.notification;

import com.example.app.common.exception.domain.DomainException;
import org.springframework.http.HttpStatus;

/**
 * Base exception class for notification domain-related exceptions.
 * All notification-specific exceptions should extend from this class.
 */
public class NotificationException extends DomainException {
    
    public NotificationException(String message, String errorCode, HttpStatus httpStatus) {
        super(message, errorCode, httpStatus);
    }
    
    public NotificationException(String message, String errorCode, HttpStatus httpStatus, Throwable cause) {
        super(message, errorCode, httpStatus, cause);
    }
    
    // Common notification exceptions as static inner classes
    
    /**
     * Exception thrown when email sending fails.
     */
    public static class EmailSendingException extends NotificationException {
        public EmailSendingException(String recipient, String reason) {
            super(String.format("Failed to send email to '%s': %s", recipient, reason),
                  "EMAIL_SENDING_FAILED", HttpStatus.SERVICE_UNAVAILABLE);
        }
        
        public EmailSendingException(String recipient, Throwable cause) {
            super(String.format("Failed to send email to '%s'", recipient),
                  "EMAIL_SENDING_FAILED", HttpStatus.SERVICE_UNAVAILABLE, cause);
        }
    }
    
    /**
     * Exception thrown when SMS delivery fails.
     */
    public static class SMSDeliveryException extends NotificationException {
        public SMSDeliveryException(String phoneNumber, String error) {
            super(String.format("Failed to deliver SMS to %s: %s", phoneNumber, error),
                  "SMS_DELIVERY_FAILED", HttpStatus.SERVICE_UNAVAILABLE);
        }
    }
    
    /**
     * Exception thrown when push notification fails.
     */
    public static class PushNotificationException extends NotificationException {
        public PushNotificationException(String deviceToken, String error) {
            super(String.format("Failed to send push notification to device %s: %s", deviceToken, error),
                  "PUSH_NOTIFICATION_FAILED", HttpStatus.SERVICE_UNAVAILABLE);
        }
    }
    
    /**
     * Exception thrown when notification template is not found.
     */
    public static class TemplateNotFoundException extends NotificationException {
        public TemplateNotFoundException(String templateName) {
            super(String.format("Notification template '%s' not found", templateName),
                  "TEMPLATE_NOT_FOUND", HttpStatus.NOT_FOUND);
        }
    }
    
    /**
     * Exception thrown when template rendering fails.
     */
    public static class TemplateRenderingException extends NotificationException {
        public TemplateRenderingException(String templateName, String error) {
            super(String.format("Failed to render template '%s': %s", templateName, error),
                  "TEMPLATE_RENDERING_FAILED", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * Exception thrown when notification rate limit is exceeded.
     */
    public static class NotificationRateLimitException extends NotificationException {
        public NotificationRateLimitException(String recipient, int limit, String period) {
            super(String.format("Notification rate limit exceeded for '%s': %d per %s", recipient, limit, period),
                  "NOTIFICATION_RATE_LIMIT_EXCEEDED", HttpStatus.TOO_MANY_REQUESTS);
        }
    }
    
    /**
     * Exception thrown when recipient is invalid or blocked.
     */
    public static class InvalidRecipientException extends NotificationException {
        public InvalidRecipientException(String recipient, String reason) {
            super(String.format("Invalid recipient '%s': %s", recipient, reason),
                  "INVALID_RECIPIENT", HttpStatus.BAD_REQUEST);
        }
    }
    
    /**
     * Exception thrown when notification channel is not configured.
     */
    public static class ChannelNotConfiguredException extends NotificationException {
        public ChannelNotConfiguredException(String channel) {
            super(String.format("Notification channel '%s' is not configured", channel),
                  "CHANNEL_NOT_CONFIGURED", HttpStatus.SERVICE_UNAVAILABLE);
        }
    }
}
