package com.example.app.common.config;

import com.example.app.auth.service.CustomOAuth2UserService;
import com.example.app.auth.service.CustomUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.session.web.http.CookieHttpSessionIdResolver;
import org.springframework.session.web.http.HttpSessionIdResolver;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/** Spring Security configuration with Redis session management */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true, jsr250Enabled = true)
public class SecurityConfig {

  private final CustomUserDetailsService customUserDetailsService;
  private final CustomOAuth2UserService customOAuth2UserService;
  private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
  private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
  private final CustomAccessDeniedHandler customAccessDeniedHandler;
  private final SessionRegistry sessionRegistry;

  @Value("${app.security.session.max-sessions:-1}")
  private int maxSessions;

  @Value("${app.security.session.prevent-login-if-maximum-exceeded:false}")
  private boolean preventLoginIfMaxExceeded;

  @Autowired
  public SecurityConfig(
      CustomUserDetailsService customUserDetailsService,
      CustomOAuth2UserService customOAuth2UserService,
      OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler,
      CustomAuthenticationEntryPoint customAuthenticationEntryPoint,
      CustomAccessDeniedHandler customAccessDeniedHandler,
      SessionRegistry sessionRegistry) {
    this.customUserDetailsService = customUserDetailsService;
    this.customOAuth2UserService = customOAuth2UserService;
    this.oAuth2AuthenticationSuccessHandler = oAuth2AuthenticationSuccessHandler;
    this.customAuthenticationEntryPoint = customAuthenticationEntryPoint;
    this.customAccessDeniedHandler = customAccessDeniedHandler;
    this.sessionRegistry = sessionRegistry;
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12);
  }

  @Bean
  public DaoAuthenticationProvider authenticationProvider() {
    DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
    authProvider.setUserDetailsService(customUserDetailsService);
    authProvider.setPasswordEncoder(passwordEncoder());
    authProvider.setHideUserNotFoundExceptions(false);
    return authProvider;
  }

  @Bean
  public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
      throws Exception {
    return config.getAuthenticationManager();
  }

  // SessionRegistry is now provided by RedisSessionRegistry configuration

  @Bean
  public HttpSessionEventPublisher httpSessionEventPublisher() {
    return new HttpSessionEventPublisher();
  }

  // Commenting out custom HttpSessionIdResolver to use Spring Boot defaults
  // The @EnableRedisHttpSession will handle session ID resolution automatically
  @Bean
  public HttpSessionIdResolver httpSessionIdResolver() {
    // HeaderHttpSessionIdResolver → Best for mobile apps, Postman/Thunder Client, or stateless APIs
    // that don’t want to deal with cookies.
    return new CookieHttpSessionIdResolver();
  }

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http, SessionRegistry sessionRegistry)
      throws Exception {
    http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .csrf(AbstractHttpConfigurer::disable)
        .authenticationProvider(authenticationProvider())

        // Session management configuration
        .sessionManagement(
            session -> {
              session
                  .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                  .sessionFixation()
                  .changeSessionId() // Use changeSessionId instead of migrateSession to avoid
                  // duplicate sessions
                  .invalidSessionUrl("/auth/login?expired=true")
                  .maximumSessions(maxSessions) // Configurable session limit
                  .maxSessionsPreventsLogin(preventLoginIfMaxExceeded) // Configurable behavior
                  .sessionRegistry(sessionRegistry);
            })

        // Authorization rules - Note: paths are relative to context path, which is /api
        .authorizeHttpRequests(
            authz ->
                authz
                    // Start with most specific public endpoints first
                    .requestMatchers(
                        "/auth/register",
                        "/auth/login",
                        "/auth/status",
                        "/auth/verify-email",
                        "/auth/forgot-password",
                        "/auth/reset-password",
                        "/auth/logout",
                        "/auth/oauth2/**")
                    .permitAll()
                    .requestMatchers("/oauth2/**", "/login/oauth2/**")
                    .permitAll()
                    .requestMatchers("/public/**")
                    .permitAll()
                    .requestMatchers("/actuator/health", "/actuator/info")
                    .permitAll()
                    .requestMatchers("/error")
                    .permitAll()

                    // Admin endpoints (relative to /api context path)
                    .requestMatchers("/admin/**")
                    .hasRole("ADMIN")
                    .requestMatchers("/actuator/**")
                    .hasRole("ADMIN")

                    // Manager endpoints (relative to /api context path)
                    .requestMatchers("/manager/**")
                    .hasAnyRole("ADMIN", "MANAGER")

                    // Moderator endpoints (relative to /api context path)
                    .requestMatchers("/moderator/**")
                    .hasAnyRole("ADMIN", "MODERATOR")

                    // User endpoints (relative to /api context path)
                    .requestMatchers("/user/**")
                    .hasAnyRole("USER", "ADMIN", "MANAGER", "MODERATOR")

                    // Any other request needs authentication
                    .anyRequest()
                    .authenticated())

        // Disable form login since we're using REST API for React
        .formLogin(AbstractHttpConfigurer::disable)

        // OAuth2 Login configuration
        .oauth2Login(
            oauth2 ->
                oauth2
                    .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                    .successHandler(oAuth2AuthenticationSuccessHandler)
                    .failureHandler(
                        (request, response, exception) -> {
                          response.setStatus(401);
                          response.setContentType("application/json");
                          response
                              .getWriter()
                              .write(
                                  "{\"success\":false,\"message\":\"OAuth2 authentication failed: "
                                      + exception.getMessage()
                                      + "\"}");
                        }))

        // Logout configuration
        .logout(
            logout ->
                logout
                    .logoutRequestMatcher(new AntPathRequestMatcher("/auth/logout", "POST"))
                    .logoutSuccessHandler(logoutSuccessHandler())
                    .invalidateHttpSession(true)
                    .clearAuthentication(true)
                    .deleteCookies("JSESSIONID")
                    .permitAll())

        // Exception handling
        .exceptionHandling(
            ex ->
                ex.authenticationEntryPoint(customAuthenticationEntryPoint)
                    .accessDeniedHandler(customAccessDeniedHandler));

    // H2 Console configuration (for development only)
    http.headers(headers -> headers.frameOptions(frameOptions -> frameOptions.sameOrigin()));

    return http.build();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();

    configuration.setAllowedOriginPatterns(
        Arrays.asList(
            "http://localhost:5173", // Vite React port
            "http://localhost:3000", // React default
            "http://localhost:3001", // Alternative React port
            "http://127.0.0.1:5173",
            "http://127.0.0.1:3000",
            "http://127.0.0.1:3001",
            "http://localhost:*", // Fallback for other ports
            "http://127.0.0.1:*" // Fallback for other ports
            ));

    configuration.setAllowedMethods(
        Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));

    configuration.setAllowedHeaders(
        Arrays.asList(
            "Authorization",
            "Content-Type",
            "X-Requested-With",
            "X-Auth-Token",
            "Cache-Control",
            "Accept",
            "Origin"));

    configuration.setExposedHeaders(Arrays.asList("X-Auth-Token", "Set-Cookie"));

    configuration.setAllowCredentials(true);
    configuration.setMaxAge(3600L);
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }

  @Bean
  public AuthenticationSuccessHandler authenticationSuccessHandler() {
    return (request, response, authentication) -> {
      response.setStatus(200);
      response.setContentType("application/json");
      response
          .getWriter()
          .write(
              "{\"success\":true,\"message\":\"Authentication successful\",\"user\":\""
                  + authentication.getName()
                  + "\"}");
    };
  }

  @Bean
  public AuthenticationFailureHandler authenticationFailureHandler() {
    return (request, response, exception) -> {
      response.setStatus(401);
      response.setContentType("application/json");
      response
          .getWriter()
          .write(
              "{\"success\":false,\"message\":\"Authentication failed: "
                  + exception.getMessage()
                  + "\"}");
    };
  }

  @Bean
  public LogoutSuccessHandler logoutSuccessHandler() {
    return (request, response, authentication) -> {
      response.setStatus(200);
      response.setContentType("application/json");
      response.getWriter().write("{\"success\":true,\"message\":\"Logout successful\"}");
    };
  }
}
