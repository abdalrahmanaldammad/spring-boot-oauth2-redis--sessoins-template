package com.example.app.common.exception.security;

import com.example.app.common.exception.base.ApplicationException;
import org.springframework.http.HttpStatus;

/**
 * Base exception class for all security-related exceptions.
 * This includes authentication, authorization, and other security concerns.
 */
public abstract class SecurityException extends ApplicationException {
    
    public SecurityException(String message, String errorCode, HttpStatus httpStatus) {
        super(message, errorCode, httpStatus);
    }
    
    public SecurityException(String message, String errorCode, HttpStatus httpStatus, Throwable cause) {
        super(message, errorCode, httpStatus, cause);
    }
    
    public SecurityException(String message, String errorCode, HttpStatus httpStatus, Object[] args) {
        super(message, errorCode, httpStatus, args);
    }
    
    public SecurityException(String message, String errorCode, HttpStatus httpStatus, Object[] args, Throwable cause) {
        super(message, errorCode, httpStatus, args, cause);
    }
}
