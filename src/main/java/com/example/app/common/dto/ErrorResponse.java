package com.example.app.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Standardized error response DTO for all API errors
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    
    private boolean success = false;
    private String message;
    private String errorCode;
    private int status;
    private LocalDateTime timestamp;
    private String path;
    private String method;
    private String traceId;
    
    // For validation errors
    private List<FieldError> fieldErrors;
    
    // For additional error details (not exposed in production)
    private Map<String, Object> debugInfo;
    
    /**
     * Field-specific error for validation failures
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FieldError {
        private String field;
        private Object rejectedValue;
        private String message;
        private String code;
    }
    
    /**
     * Create a simple error response
     */
    public static ErrorResponse of(String message, String errorCode, int status) {
        return ErrorResponse.builder()
                .success(false)
                .message(message)
                .errorCode(errorCode)
                .status(status)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    /**
     * Create error response with path and method
     */
    public static ErrorResponse of(String message, String errorCode, int status, 
                                 String path, String method) {
        return ErrorResponse.builder()
                .success(false)
                .message(message)
                .errorCode(errorCode)
                .status(status)
                .path(path)
                .method(method)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
