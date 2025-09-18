package com.example.app.auth.service;
import com.example.app.email.entity.TokenType;

import com.example.app.common.entity.Role;
import com.example.app.user.entity.User;
import com.example.app.email.entity.TokenType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import com.example.app.email.entity.TokenType;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * UserPrincipal class implementing UserDetails for Spring Security
 * This class represents the authenticated user in the security context
 */
@Getter
@Builder
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"password", "authorities"})
public class UserPrincipal implements OAuth2User, UserDetails {
    
    private final Long id;
    private final String username;
    private final String email;
    
    @JsonIgnore
    private final String password;
    
    private final String firstName;
    private final String lastName;
    private final boolean enabled;
    private final boolean accountNonExpired;
    private final boolean accountNonLocked;
    private final boolean credentialsNonExpired;
    private final Collection<? extends GrantedAuthority> authorities;
    private final Map<String, Object> attributes;
    
    // UserDetails implementation methods (Lombok getters handle most of these)
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }
    
    @Override
    public boolean isAccountNonExpired() {
        return accountNonExpired;
    }
    
    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }
    
    @Override
    public boolean isCredentialsNonExpired() {
        return credentialsNonExpired;
    }
    
    @Override
    public boolean isEnabled() {
        return enabled;
    }
    
    @Override
    public String getUsername() {
        return username;
    }
    
    @Override
    public String getPassword() {
        return password;
    }
    
    // OAuth2User implementation methods
    @Override
    public String getName() {
        // Return username for session indexing compatibility
        // Spring Session uses getName() to index sessions by principal name
        return username;
    }
    
    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }
    
    // Custom business method
    public String getFullName() {
        return firstName + " " + lastName;
    }
    
    // Static factory methods
    public static UserPrincipal create(User user) {
        List<GrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getName()))
                .collect(Collectors.toList());
        
        return UserPrincipal.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .password(user.getPassword())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .enabled(user.getEnabled())
                .accountNonExpired(user.getAccountNonExpired())
                .accountNonLocked(user.getAccountNonLocked())
                .credentialsNonExpired(user.getCredentialsNonExpired())
                .authorities(authorities)
                .attributes(null)
                .build();
    }
    
    public static UserPrincipal create(User user, Map<String, Object> attributes) {
        UserPrincipal userPrincipal = UserPrincipal.create(user);
        return UserPrincipal.builder()
                .id(userPrincipal.getId())
                .username(userPrincipal.getUsername())
                .email(userPrincipal.getEmail())
                .password(userPrincipal.getPassword())
                .firstName(userPrincipal.getFirstName())
                .lastName(userPrincipal.getLastName())
                .enabled(userPrincipal.isEnabled())
                .accountNonExpired(userPrincipal.isAccountNonExpired())
                .accountNonLocked(userPrincipal.isAccountNonLocked())
                .credentialsNonExpired(userPrincipal.isCredentialsNonExpired())
                .authorities(userPrincipal.getAuthorities())
                .attributes(attributes)
                .build();
    }
}
