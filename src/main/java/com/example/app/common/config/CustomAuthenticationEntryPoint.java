package com.example.app.common.config;

import com.example.app.common.dto.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Custom authentication entry point for handling unauthorized requests
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                        AuthenticationException authException) throws IOException {
        
        String traceId = UUID.randomUUID().toString().substring(0, 8);
        
        log.warn("Authentication failed: {} | TraceId: {} | Path: {} | Method: {} | IP: {} | UserAgent: {}", 
                authException.getMessage(), traceId, request.getRequestURI(), request.getMethod(),
                getClientIpAddress(request), request.getHeader("User-Agent"));

        ErrorResponse errorResponse = ErrorResponse.builder()
                .success(false)
                .message(determineErrorMessage(authException, request))
                .errorCode(determineErrorCode(authException))
                .status(HttpServletResponse.SC_UNAUTHORIZED)
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .method(request.getMethod())
                .traceId(traceId)
                .build();

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        
        // Add security headers
        response.setHeader("WWW-Authenticate", "Bearer");
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("X-XSS-Protection", "1; mode=block");
        
        objectMapper.writeValue(response.getWriter(), errorResponse);
    }

    private String determineErrorMessage(AuthenticationException authException, HttpServletRequest request) {
        // Don't expose internal details for security reasons
        String path = request.getRequestURI();
        
        if (path.contains("/admin/")) {
            return "Administrative access requires proper authentication";
        } else if (path.contains("/user/")) {
            return "User access requires authentication";
        } else if (request.getSession(false) != null && request.getSession(false).isNew()) {
            return "Your session has expired. Please log in again";
        } else {
            return "Authentication required to access this resource";
        }
    }

    private String determineErrorCode(AuthenticationException authException) {
        String exceptionName = authException.getClass().getSimpleName();
        
        return switch (exceptionName) {
            case "BadCredentialsException" -> "INVALID_CREDENTIALS";
            case "AccountExpiredException" -> "ACCOUNT_EXPIRED";
            case "LockedException" -> "ACCOUNT_LOCKED";
            case "DisabledException" -> "ACCOUNT_DISABLED";
            case "CredentialsExpiredException" -> "CREDENTIALS_EXPIRED";
            case "SessionAuthenticationException" -> "SESSION_EXPIRED";
            case "InsufficientAuthenticationException" -> "INSUFFICIENT_AUTHENTICATION";
            default -> "AUTHENTICATION_REQUIRED";
        };
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}
