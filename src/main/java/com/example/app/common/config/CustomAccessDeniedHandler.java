package com.example.app.common.config;

import com.example.app.common.dto.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Custom access denied handler for handling forbidden requests
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                      AccessDeniedException accessDeniedException) throws IOException {
        
        String traceId = UUID.randomUUID().toString().substring(0, 8);
        String username = request.getUserPrincipal() != null ? request.getUserPrincipal().getName() : "anonymous";
        
        log.warn("Access denied: {} | TraceId: {} | Path: {} | Method: {} | User: {} | IP: {} | UserAgent: {}", 
                accessDeniedException.getMessage(), traceId, request.getRequestURI(), request.getMethod(),
                username, getClientIpAddress(request), request.getHeader("User-Agent"));

        ErrorResponse errorResponse = ErrorResponse.builder()
                .success(false)
                .message(determineErrorMessage(request, username))
                .errorCode("ACCESS_DENIED")
                .status(HttpServletResponse.SC_FORBIDDEN)
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .method(request.getMethod())
                .traceId(traceId)
                .build();

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        
        // Add security headers
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("X-XSS-Protection", "1; mode=block");
        
        objectMapper.writeValue(response.getWriter(), errorResponse);
    }

    private String determineErrorMessage(HttpServletRequest request, String username) {
        String path = request.getRequestURI();
        String method = request.getMethod();
        
        if (path.contains("/admin/")) {
            return "Access denied: Administrative privileges required";
        } else if (path.contains("/manager/")) {
            return "Access denied: Manager role required";
        } else if (path.contains("/moderator/")) {
            return "Access denied: Moderator privileges required";
        } else if (method.equals("DELETE")) {
            return "Access denied: You don't have permission to delete this resource";
        } else if (method.equals("PUT") || method.equals("PATCH")) {
            return "Access denied: You don't have permission to modify this resource";
        } else {
            return "Access denied: Insufficient privileges to access this resource";
        }
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
