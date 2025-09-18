package com.example.app.common.config;

import com.example.app.auth.service.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

/**
 * Handles successful OAuth2 authentication
 */
@Component
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    @Value("${app.oauth2.authorized-redirect-uri:http://localhost:5173/oauth2/redirect}")
    private String authorizedRedirectUri;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                      Authentication authentication) throws IOException {
        
        if (response.isCommitted()) {
            logger.debug("Response has already been committed. Unable to redirect.");
            return;
        }

        // Store authentication in session (using Redis)
        SecurityContextHolder.getContext().setAuthentication(authentication);
        
        // Check for existing session first to avoid creating duplicates
        HttpSession session = request.getSession(false);
        if (session == null) {
            // Only create a new session if one doesn't exist
            session = request.getSession(true);
        }
        
        // Store the security context in the session
        session.setAttribute(
            HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
            SecurityContextHolder.getContext()
        );

        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        
        // Create success redirect URL with session info
        String targetUrl = authorizedRedirectUri + 
            "?success=true" +
            "&sessionId=" + URLEncoder.encode(session.getId(), StandardCharsets.UTF_8) +
            "&userId=" + userPrincipal.getId() +
            "&username=" + URLEncoder.encode(userPrincipal.getUsername(), StandardCharsets.UTF_8);

        clearAuthenticationAttributes(request);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
