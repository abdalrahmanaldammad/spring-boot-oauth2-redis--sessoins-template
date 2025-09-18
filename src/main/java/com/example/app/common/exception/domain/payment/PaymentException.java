package com.example.app.common.exception.domain.payment;

import com.example.app.common.exception.domain.DomainException;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;

/**
 * Base exception class for payment domain-related exceptions.
 * All payment-specific exceptions should extend from this class.
 */
public class PaymentException extends DomainException {
    
    public PaymentException(String message, String errorCode, HttpStatus httpStatus) {
        super(message, errorCode, httpStatus);
    }
    
    public PaymentException(String message, String errorCode, HttpStatus httpStatus, Throwable cause) {
        super(message, errorCode, httpStatus, cause);
    }
    
    // Common payment exceptions as static inner classes
    
    /**
     * Exception thrown when a payment fails to process.
     */
    public static class PaymentFailedException extends PaymentException {
        public PaymentFailedException(String transactionId, String reason) {
            super(String.format("Payment failed for transaction %s: %s", transactionId, reason),
                  "PAYMENT_FAILED", HttpStatus.PAYMENT_REQUIRED);
        }
        
        public PaymentFailedException(String reason) {
            super(String.format("Payment processing failed: %s", reason),
                  "PAYMENT_FAILED", HttpStatus.PAYMENT_REQUIRED);
        }
    }
    
    /**
     * Exception thrown when there are insufficient funds.
     */
    public static class InsufficientFundsException extends PaymentException {
        public InsufficientFundsException(BigDecimal required, BigDecimal available) {
            super(String.format("Insufficient funds. Required: %s, Available: %s", required, available),
                  "INSUFFICIENT_FUNDS", HttpStatus.PAYMENT_REQUIRED);
        }
    }
    
    /**
     * Exception thrown when a refund fails.
     */
    public static class RefundException extends PaymentException {
        public RefundException(String transactionId, String reason) {
            super(String.format("Refund failed for transaction %s: %s", transactionId, reason),
                  "REFUND_FAILED", HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }
    
    /**
     * Exception thrown when payment method is invalid or expired.
     */
    public static class InvalidPaymentMethodException extends PaymentException {
        public InvalidPaymentMethodException(String method, String reason) {
            super(String.format("Invalid payment method '%s': %s", method, reason),
                  "INVALID_PAYMENT_METHOD", HttpStatus.BAD_REQUEST);
        }
    }
    
    /**
     * Exception thrown when transaction is not found.
     */
    public static class TransactionNotFoundException extends PaymentException {
        public TransactionNotFoundException(String transactionId) {
            super(String.format("Transaction with ID '%s' not found", transactionId),
                  "TRANSACTION_NOT_FOUND", HttpStatus.NOT_FOUND);
        }
    }
    
    /**
     * Exception thrown when payment is already processed.
     */
    public static class DuplicatePaymentException extends PaymentException {
        public DuplicatePaymentException(String transactionId) {
            super(String.format("Payment for transaction '%s' has already been processed", transactionId),
                  "DUPLICATE_PAYMENT", HttpStatus.CONFLICT);
        }
    }
    
    /**
     * Exception thrown when payment gateway is unavailable.
     */
    public static class PaymentGatewayException extends PaymentException {
        public PaymentGatewayException(String gateway, String error) {
            super(String.format("Payment gateway '%s' error: %s", gateway, error),
                  "PAYMENT_GATEWAY_ERROR", HttpStatus.SERVICE_UNAVAILABLE);
        }
        
        public PaymentGatewayException(String gateway, Throwable cause) {
            super(String.format("Payment gateway '%s' is unavailable", gateway),
                  "PAYMENT_GATEWAY_ERROR", HttpStatus.SERVICE_UNAVAILABLE, cause);
        }
    }
}
