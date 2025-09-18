package com.example.app.common.exception.base;

import org.springframework.http.HttpStatus;

/**
 * Base exception class for all application-specific exceptions.
 * This provides a unified structure for error handling across the application.
 * 
 * All custom exceptions should extend from this class or its subclasses.
 */
public abstract class ApplicationException extends RuntimeException {
    
    private final String errorCode;
    private final HttpStatus httpStatus;
    private final Object[] args;  // For message formatting/i18n
    
    /**
     * Creates an ApplicationException with message, error code, and HTTP status.
     * 
     * @param message The error message
     * @param errorCode The unique error code for this exception type
     * @param httpStatus The HTTP status to return
     */
    public ApplicationException(String message, String errorCode, HttpStatus httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
        this.args = null;
    }
    
    /**
     * Creates an ApplicationException with message, error code, HTTP status, and cause.
     * 
     * @param message The error message
     * @param errorCode The unique error code for this exception type
     * @param httpStatus The HTTP status to return
     * @param cause The underlying cause of this exception
     */
    public ApplicationException(String message, String errorCode, HttpStatus httpStatus, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
        this.args = null;
    }
    
    /**
     * Creates an ApplicationException with message formatting arguments for i18n support.
     * 
     * @param message The error message (can be a message key for i18n)
     * @param errorCode The unique error code for this exception type
     * @param httpStatus The HTTP status to return
     * @param args Arguments for message formatting
     */
    public ApplicationException(String message, String errorCode, HttpStatus httpStatus, Object[] args) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
        this.args = args;
    }
    
    /**
     * Creates an ApplicationException with message formatting arguments and a cause.
     * 
     * @param message The error message (can be a message key for i18n)
     * @param errorCode The unique error code for this exception type
     * @param httpStatus The HTTP status to return
     * @param args Arguments for message formatting
     * @param cause The underlying cause of this exception
     */
    public ApplicationException(String message, String errorCode, HttpStatus httpStatus, Object[] args, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
        this.args = args;
    }
    
    /**
     * Gets the error code associated with this exception.
     * Error codes should be unique and follow the UPPER_SNAKE_CASE convention.
     * 
     * @return The error code
     */
    public String getErrorCode() {
        return errorCode;
    }
    
    /**
     * Gets the HTTP status associated with this exception.
     * 
     * @return The HTTP status
     */
    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
    
    /**
     * Gets the HTTP status code as an integer.
     * 
     * @return The HTTP status code
     */
    public int getHttpStatusCode() {
        return httpStatus.value();
    }
    
    /**
     * Gets the message formatting arguments.
     * These can be used for internationalization or dynamic message construction.
     * 
     * @return The message arguments, or null if none were provided
     */
    public Object[] getArgs() {
        return args;
    }
    
    /**
     * Checks if this exception has message formatting arguments.
     * 
     * @return true if arguments are available, false otherwise
     */
    public boolean hasArgs() {
        return args != null && args.length > 0;
    }
    
    /**
     * Gets a formatted message using the provided arguments.
     * This is useful for i18n or when the message contains placeholders.
     * 
     * @return The formatted message
     */
    public String getFormattedMessage() {
        if (hasArgs()) {
            return String.format(getMessage(), args);
        }
        return getMessage();
    }
    
    @Override
    public String toString() {
        return String.format("%s[errorCode=%s, httpStatus=%d, message=%s]",
                getClass().getSimpleName(), errorCode, httpStatus.value(), getMessage());
    }
}
