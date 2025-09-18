package com.example.app.common.exception;

import com.example.app.common.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Global exception handler for all API endpoints
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    /**
     * Handle all application-specific exceptions (new base handler)
     */
    @ExceptionHandler(com.example.app.common.exception.base.ApplicationException.class)
    public ResponseEntity<ErrorResponse> handleApplicationException(
            com.example.app.common.exception.base.ApplicationException ex, 
            HttpServletRequest request) {
        String traceId = generateTraceId();
        
        // Log level based on HTTP status
        if (ex.getHttpStatus().is5xxServerError()) {
            log.error("Application exception occurred: {} | TraceId: {} | Path: {} | Method: {}", 
                    ex.getMessage(), traceId, request.getRequestURI(), request.getMethod(), ex);
        } else {
            log.warn("Application exception occurred: {} | TraceId: {} | Path: {} | Method: {}", 
                    ex.getMessage(), traceId, request.getRequestURI(), request.getMethod());
        }
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .success(false)
                .message(ex.getMessage())
                .errorCode(ex.getErrorCode())
                .status(ex.getHttpStatusCode())
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .method(request.getMethod())
                .traceId(traceId)
                .build();
        
        // Add debug info in non-production environments
        if (isDevelopmentMode()) {
            Map<String, Object> debugInfo = new HashMap<>();
            debugInfo.put("exceptionType", ex.getClass().getSimpleName());
            debugInfo.put("stackTrace", getStackTraceAsString(ex, 5));
            errorResponse.setDebugInfo(debugInfo);
        }
        
        return ResponseEntity.status(ex.getHttpStatus()).body(errorResponse);
    }
    

    /**
     * Handle Spring Security authentication exceptions
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException ex, 
                                                                     HttpServletRequest request) {
        String traceId = generateTraceId();
        
        log.warn("Authentication exception: {} | TraceId: {} | Path: {} | Method: {}", 
                ex.getMessage(), traceId, request.getRequestURI(), request.getMethod());
        
        ErrorResponse errorResponse = ErrorResponse.of(
            "Authentication failed: " + ex.getMessage(),
            "AUTHENTICATION_FAILED",
            HttpStatus.UNAUTHORIZED.value(),
            request.getRequestURI(),
            request.getMethod()
        );
        errorResponse.setTraceId(traceId);
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    /**
     * Handle Spring Security access denied exceptions
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException ex, 
                                                                   HttpServletRequest request) {
        String traceId = generateTraceId();
        
        log.warn("Access denied: {} | TraceId: {} | Path: {} | Method: {} | User: {}", 
                ex.getMessage(), traceId, request.getRequestURI(), request.getMethod(),
                request.getUserPrincipal() != null ? request.getUserPrincipal().getName() : "anonymous");
        
        ErrorResponse errorResponse = ErrorResponse.of(
            "Access denied: " + ex.getMessage(),
            "ACCESS_DENIED",
            HttpStatus.FORBIDDEN.value(),
            request.getRequestURI(),
            request.getMethod()
        );
        errorResponse.setTraceId(traceId);
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

    /**
     * Handle validation errors from @Valid annotations
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                     HttpServletRequest request) {
        String traceId = generateTraceId();
        
        log.warn("Validation error: {} | TraceId: {} | Path: {}", 
                ex.getMessage(), traceId, request.getRequestURI());
        
        List<ErrorResponse.FieldError> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> ErrorResponse.FieldError.builder()
                        .field(error.getField())
                        .rejectedValue(error.getRejectedValue())
                        .message(error.getDefaultMessage())
                        .code(error.getCode())
                        .build())
                .collect(Collectors.toList());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .success(false)
                .message("Validation failed")
                .errorCode("VALIDATION_FAILED")
                .status(HttpStatus.BAD_REQUEST.value())
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .method(request.getMethod())
                .traceId(traceId)
                .fieldErrors(fieldErrors)
                .build();
        
        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * Handle bind exceptions
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ErrorResponse> handleBindException(BindException ex,
                                                           HttpServletRequest request) {
        String traceId = generateTraceId();
        
        log.warn("Bind error: {} | TraceId: {} | Path: {}", 
                ex.getMessage(), traceId, request.getRequestURI());
        
        List<ErrorResponse.FieldError> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> ErrorResponse.FieldError.builder()
                        .field(error.getField())
                        .rejectedValue(error.getRejectedValue())
                        .message(error.getDefaultMessage())
                        .code(error.getCode())
                        .build())
                .collect(Collectors.toList());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .success(false)
                .message("Request binding failed")
                .errorCode("BINDING_FAILED")
                .status(HttpStatus.BAD_REQUEST.value())
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .method(request.getMethod())
                .traceId(traceId)
                .fieldErrors(fieldErrors)
                .build();
        
        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * Handle constraint violation exceptions
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex,
                                                                 HttpServletRequest request) {
        String traceId = generateTraceId();
        
        log.warn("Constraint violation: {} | TraceId: {} | Path: {}", 
                ex.getMessage(), traceId, request.getRequestURI());
        
        List<ErrorResponse.FieldError> fieldErrors = ex.getConstraintViolations()
                .stream()
                .map(violation -> ErrorResponse.FieldError.builder()
                        .field(getFieldName(violation))
                        .rejectedValue(violation.getInvalidValue())
                        .message(violation.getMessage())
                        .code(violation.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName())
                        .build())
                .collect(Collectors.toList());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .success(false)
                .message("Constraint validation failed")
                .errorCode("CONSTRAINT_VIOLATION")
                .status(HttpStatus.BAD_REQUEST.value())
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .method(request.getMethod())
                .traceId(traceId)
                .fieldErrors(fieldErrors)
                .build();
        
        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * Handle database integrity violations
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException ex,
                                                                     HttpServletRequest request) {
        String traceId = generateTraceId();
        
        log.error("Data integrity violation: {} | TraceId: {} | Path: {}", 
                 ex.getMessage(), traceId, request.getRequestURI(), ex);
        
        String message = "Data integrity violation occurred";
        String errorCode = "DATA_INTEGRITY_VIOLATION";
        
        // Check for common constraint violations
        if (ex.getMessage() != null) {
            if (ex.getMessage().contains("Unique index or primary key violation")) {
                message = "Resource already exists with the provided data";
                errorCode = "DUPLICATE_RESOURCE";
            } else if (ex.getMessage().contains("foreign key constraint")) {
                message = "Referenced resource does not exist";
                errorCode = "FOREIGN_KEY_VIOLATION";
            }
        }
        
        ErrorResponse errorResponse = ErrorResponse.of(
            message,
            errorCode,
            HttpStatus.CONFLICT.value(),
            request.getRequestURI(),
            request.getMethod()
        );
        errorResponse.setTraceId(traceId);
        
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    /**
     * Handle missing request parameters
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingServletRequestParameter(MissingServletRequestParameterException ex,
                                                                            HttpServletRequest request) {
        String traceId = generateTraceId();
        
        log.warn("Missing request parameter: {} | TraceId: {} | Path: {}", 
                ex.getMessage(), traceId, request.getRequestURI());
        
        ErrorResponse errorResponse = ErrorResponse.of(
            String.format("Missing required parameter: %s", ex.getParameterName()),
            "MISSING_PARAMETER",
            HttpStatus.BAD_REQUEST.value(),
            request.getRequestURI(),
            request.getMethod()
        );
        errorResponse.setTraceId(traceId);
        
        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * Handle method argument type mismatch
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                                         HttpServletRequest request) {
        String traceId = generateTraceId();
        
        log.warn("Method argument type mismatch: {} | TraceId: {} | Path: {}", 
                ex.getMessage(), traceId, request.getRequestURI());
        
        String message = String.format("Invalid value '%s' for parameter '%s'. Expected type: %s", 
                                      ex.getValue(), ex.getName(), 
                                      ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown");
        
        ErrorResponse errorResponse = ErrorResponse.of(
            message,
            "INVALID_PARAMETER_TYPE",
            HttpStatus.BAD_REQUEST.value(),
            request.getRequestURI(),
            request.getMethod()
        );
        errorResponse.setTraceId(traceId);
        
        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * Handle unsupported HTTP methods
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException ex,
                                                                            HttpServletRequest request) {
        String traceId = generateTraceId();
        
        log.warn("Method not supported: {} | TraceId: {} | Path: {} | Method: {}", 
                ex.getMessage(), traceId, request.getRequestURI(), request.getMethod());
        
        String message = String.format("Method '%s' is not supported for this endpoint. Supported methods: %s", 
                                      ex.getMethod(), String.join(", ", ex.getSupportedMethods()));
        
        ErrorResponse errorResponse = ErrorResponse.of(
            message,
            "METHOD_NOT_SUPPORTED",
            HttpStatus.METHOD_NOT_ALLOWED.value(),
            request.getRequestURI(),
            request.getMethod()
        );
        errorResponse.setTraceId(traceId);
        
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(errorResponse);
    }

    /**
     * Handle unsupported media types
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleHttpMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex,
                                                                        HttpServletRequest request) {
        String traceId = generateTraceId();
        
        log.warn("Media type not supported: {} | TraceId: {} | Path: {}", 
                ex.getMessage(), traceId, request.getRequestURI());
        
        String message = String.format("Media type '%s' is not supported. Supported types: %s", 
                                      ex.getContentType(), ex.getSupportedMediaTypes());
        
        ErrorResponse errorResponse = ErrorResponse.of(
            message,
            "MEDIA_TYPE_NOT_SUPPORTED",
            HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(),
            request.getRequestURI(),
            request.getMethod()
        );
        errorResponse.setTraceId(traceId);
        
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(errorResponse);
    }

    /**
     * Handle malformed JSON requests
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
                                                                     HttpServletRequest request) {
        String traceId = generateTraceId();
        
        log.warn("Message not readable: {} | TraceId: {} | Path: {}", 
                ex.getMessage(), traceId, request.getRequestURI());
        
        ErrorResponse errorResponse = ErrorResponse.of(
            "Invalid request format. Please check your request body",
            "INVALID_REQUEST_FORMAT",
            HttpStatus.BAD_REQUEST.value(),
            request.getRequestURI(),
            request.getMethod()
        );
        errorResponse.setTraceId(traceId);
        
        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * Handle 404 errors
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoHandlerFound(NoHandlerFoundException ex,
                                                            HttpServletRequest request) {
        String traceId = generateTraceId();
        
        log.warn("No handler found: {} | TraceId: {} | Path: {} | Method: {}", 
                ex.getMessage(), traceId, request.getRequestURI(), request.getMethod());
        
        ErrorResponse errorResponse = ErrorResponse.of(
            String.format("Endpoint '%s %s' not found", ex.getHttpMethod(), ex.getRequestURL()),
            "ENDPOINT_NOT_FOUND",
            HttpStatus.NOT_FOUND.value(),
            request.getRequestURI(),
            request.getMethod()
        );
        errorResponse.setTraceId(traceId);
        
        return ResponseEntity.notFound().build();
    }

    /**
     * Handle all other exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex,
                                                              HttpServletRequest request) {
        String traceId = generateTraceId();
        
        log.error("Unexpected error occurred: {} | TraceId: {} | Path: {} | Method: {}", 
                 ex.getMessage(), traceId, request.getRequestURI(), request.getMethod(), ex);
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .success(false)
                .message("An unexpected error occurred. Please try again later")
                .errorCode("INTERNAL_SERVER_ERROR")
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .method(request.getMethod())
                .traceId(traceId)
                .build();
        
        // Add debug info in development mode
        if (isDevelopmentMode()) {
            Map<String, Object> debugInfo = new HashMap<>();
            debugInfo.put("exceptionType", ex.getClass().getSimpleName());
            debugInfo.put("originalMessage", ex.getMessage());
            debugInfo.put("stackTrace", getStackTraceAsString(ex, 10));
            errorResponse.setDebugInfo(debugInfo);
        }
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    // Helper methods

    private String generateTraceId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private boolean isDevelopmentMode() {
        return "dev".equalsIgnoreCase(activeProfile) || "development".equalsIgnoreCase(activeProfile);
    }

    private String getFieldName(ConstraintViolation<?> violation) {
        String propertyPath = violation.getPropertyPath().toString();
        return propertyPath.contains(".") ? 
            propertyPath.substring(propertyPath.lastIndexOf('.') + 1) : propertyPath;
    }

    private String getStackTraceAsString(Throwable ex, int maxLines) {
        StackTraceElement[] stackTrace = ex.getStackTrace();
        StringBuilder sb = new StringBuilder();
        
        int lines = Math.min(stackTrace.length, maxLines);
        for (int i = 0; i < lines; i++) {
            sb.append(stackTrace[i].toString());
            if (i < lines - 1) {
                sb.append("\n");
            }
        }
        
        return sb.toString();
    }
}
