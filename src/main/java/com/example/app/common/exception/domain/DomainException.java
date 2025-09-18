package com.example.app.common.exception.domain;

import com.example.app.common.exception.base.ApplicationException;
import org.springframework.http.HttpStatus;

/**
 * Base exception class for domain-specific exceptions.
 * Each business domain should have its own exception hierarchy extending from this class.
 */
public abstract class DomainException extends ApplicationException {
    
    public DomainException(String message, String errorCode, HttpStatus httpStatus) {
        super(message, errorCode, httpStatus);
    }
    
    public DomainException(String message, String errorCode, HttpStatus httpStatus, Throwable cause) {
        super(message, errorCode, httpStatus, cause);
    }
    
    public DomainException(String message, String errorCode, HttpStatus httpStatus, Object[] args) {
        super(message, errorCode, httpStatus, args);
    }
    
    public DomainException(String message, String errorCode, HttpStatus httpStatus, Object[] args, Throwable cause) {
        super(message, errorCode, httpStatus, args, cause);
    }
}
