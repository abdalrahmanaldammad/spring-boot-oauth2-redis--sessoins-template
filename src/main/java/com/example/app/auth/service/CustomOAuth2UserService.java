package com.example.app.auth.service;
import com.example.app.email.entity.TokenType;

import com.example.app.common.entity.AuthProvider;
import com.example.app.common.entity.Role;
import com.example.app.user.entity.User;
import com.example.app.email.entity.TokenType;
import com.example.app.common.repository.RoleRepository;
import com.example.app.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import com.example.app.email.entity.TokenType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

/**
 * Custom OAuth2 user service that handles user registration and login
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest oAuth2UserRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(oAuth2UserRequest);

        try {
            return processOAuth2User(oAuth2UserRequest, oauth2User);
        } catch (AuthenticationException ex) {
            throw ex;
        } catch (Exception ex) {
            // Throwing an instance of AuthenticationException will trigger the OAuth2AuthenticationFailureHandler
            throw new InternalAuthenticationServiceException(ex.getMessage(), ex.getCause());
        }
    }

    private OAuth2User processOAuth2User(OAuth2UserRequest oAuth2UserRequest, OAuth2User oauth2User) {
        String registrationId = oAuth2UserRequest.getClientRegistration().getRegistrationId();
        Map<String, Object> attributes = new java.util.HashMap<>(oauth2User.getAttributes());
        
        // Handle missing email from GitHub (when user has private email)
        if ("github".equalsIgnoreCase(registrationId)) {
            String email = (String) attributes.get("email");
            if (!StringUtils.hasText(email)) {
                // For GitHub, we can use the login (username) as a fallback email
                String login = (String) attributes.get("login");
                if (StringUtils.hasText(login)) {
                    // Create a pseudo-email using GitHub username
                    String pseudoEmail = login + "@github.local";
                    attributes.put("email", pseudoEmail);
                    log.warn("GitHub user {} has no public email, using pseudo-email: {}", login, pseudoEmail);
                }
            }
        }
        
        OAuth2UserInfo oAuth2UserInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(registrationId, attributes);
        
        if (!StringUtils.hasText(oAuth2UserInfo.getEmail())) {
            throw new OAuth2AuthenticationException("Email not found from OAuth2 provider. For GitHub users, please make your email public in GitHub settings.");
        }

        Optional<User> userOptional = userRepository.findByEmail(oAuth2UserInfo.getEmail());
        User user;
        
        if (userOptional.isPresent()) {
            user = userOptional.get();
            if (!user.getProvider().equals(AuthProvider.valueOf(registrationId.toUpperCase()))) {
                throw new OAuth2AuthenticationException(
                    "Looks like you're signed up with " + user.getProvider() + 
                    " account. Please use your " + user.getProvider() + 
                    " account to login."
                );
            }
            user = updateExistingUser(user, oAuth2UserInfo);
        } else {
            user = registerNewUser(oAuth2UserRequest, oAuth2UserInfo);
        }

        return UserPrincipal.create(user, attributes);
    }

    private User registerNewUser(OAuth2UserRequest oAuth2UserRequest, OAuth2UserInfo oAuth2UserInfo) {
        String registrationId = oAuth2UserRequest.getClientRegistration().getRegistrationId();
        
        User user = User.builder()
                .provider(AuthProvider.valueOf(registrationId.toUpperCase()))
                .providerId(oAuth2UserInfo.getId())
                .username(generateUsername(oAuth2UserInfo))
                .firstName(oAuth2UserInfo.getFirstName())
                .lastName(oAuth2UserInfo.getLastName())
                .email(oAuth2UserInfo.getEmail())
                .imageUrl(oAuth2UserInfo.getImageUrl())
                .emailVerified(true) // OAuth emails are considered verified
                .enabled(true)
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .build();

        // Assign default role
        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new RuntimeException("Default role not found"));
        user.getRoles().add(userRole);

        User savedUser = userRepository.save(user);
        log.info("New user registered via OAuth2: {}", savedUser.getEmail());
        
        return savedUser;
    }

    private User updateExistingUser(User existingUser, OAuth2UserInfo oAuth2UserInfo) {
        existingUser.setFirstName(oAuth2UserInfo.getFirstName());
        existingUser.setLastName(oAuth2UserInfo.getLastName());
        existingUser.setImageUrl(oAuth2UserInfo.getImageUrl());
        existingUser.setLastLogin(LocalDateTime.now());
        
        User updatedUser = userRepository.save(existingUser);
        log.info("Existing user updated via OAuth2: {}", updatedUser.getEmail());
        
        return updatedUser;
    }

    private String generateUsername(OAuth2UserInfo oAuth2UserInfo) {
        String baseUsername;
        
        if (StringUtils.hasText(oAuth2UserInfo.getName())) {
            baseUsername = oAuth2UserInfo.getName().toLowerCase()
                    .replaceAll("[^a-z0-9]", "");
        } else {
            baseUsername = oAuth2UserInfo.getEmail().substring(0, 
                    oAuth2UserInfo.getEmail().indexOf("@")).toLowerCase()
                    .replaceAll("[^a-z0-9]", "");
        }
        
        String username = baseUsername;
        int counter = 1;
        
        while (userRepository.existsByUsername(username)) {
            username = baseUsername + counter;
            counter++;
        }
        
        return username;
    }
}
