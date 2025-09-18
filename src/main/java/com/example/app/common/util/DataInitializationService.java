package com.example.app.common.util;
import com.example.app.email.entity.TokenType;

import com.example.app.common.entity.Role;
import com.example.app.user.entity.User;
import com.example.app.email.entity.TokenType;
import com.example.app.common.repository.RoleRepository;
import com.example.app.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

/**
 * Service to initialize database with sample data
 */
@Service
public class DataInitializationService implements CommandLineRunner {
    
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    
    @Autowired
    public DataInitializationService(UserRepository userRepository,
                                   RoleRepository roleRepository,
                                   PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }
    
    @Override
    @Transactional
    public void run(String... args) throws Exception {
        initializeRoles();
        initializeUsers();
    }
    
    private void initializeRoles() {
        List<String> roleNames = Arrays.asList("ROLE_ADMIN", "ROLE_MANAGER", "ROLE_MODERATOR", "ROLE_USER");
        
        for (String roleName : roleNames) {
            if (!roleRepository.existsByName(roleName)) {
                Role role = new Role(roleName, getRoleDescription(roleName));
                roleRepository.save(role);
                System.out.println("Created role: " + roleName);
            }
        }
    }
    
    private String getRoleDescription(String roleName) {
        return switch (roleName) {
            case "ROLE_ADMIN" -> "System administrator with full access";
            case "ROLE_MANAGER" -> "Manager with administrative privileges";
            case "ROLE_MODERATOR" -> "Content moderator with limited admin access";
            case "ROLE_USER" -> "Standard user with basic access";
            default -> "Unknown role";
        };
    }
    
    private void initializeUsers() {
        // Create Admin User
        if (!userRepository.existsByUsername("admin")) {
            User admin = createUser("admin", "admin@example.com", "admin123", 
                                  "System", "Administrator");
            
            Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                .orElseThrow(() -> new RuntimeException("Admin role not found"));
            admin.getRoles().add(adminRole);
            
            userRepository.save(admin);
            System.out.println("Created admin user: admin/admin123");
        }
        
        // Create Manager User
        if (!userRepository.existsByUsername("manager")) {
            User manager = createUser("manager", "manager@example.com", "manager123",
                                    "John", "Manager");
            
            Role managerRole = roleRepository.findByName("ROLE_MANAGER")
                .orElseThrow(() -> new RuntimeException("Manager role not found"));
            Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new RuntimeException("User role not found"));
            
            manager.getRoles().addAll(Arrays.asList(managerRole, userRole));
            
            userRepository.save(manager);
            System.out.println("Created manager user: manager/manager123");
        }
        
        // Create Moderator User
        if (!userRepository.existsByUsername("moderator")) {
            User moderator = createUser("moderator", "moderator@example.com", "moderator123",
                                      "Jane", "Moderator");
            
            Role moderatorRole = roleRepository.findByName("ROLE_MODERATOR")
                .orElseThrow(() -> new RuntimeException("Moderator role not found"));
            Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new RuntimeException("User role not found"));
            
            moderator.getRoles().addAll(Arrays.asList(moderatorRole, userRole));
            
            userRepository.save(moderator);
            System.out.println("Created moderator user: moderator/moderator123");
        }
        
        // Create Regular Users
        List<UserData> regularUsers = Arrays.asList(
            new UserData("alice", "alice@example.com", "alice123", "Alice", "Johnson"),
            new UserData("bob", "bob@example.com", "bob123", "Bob", "Smith"),
            new UserData("charlie", "charlie@example.com", "charlie123", "Charlie", "Brown"),
            new UserData("diana", "diana@example.com", "diana123", "Diana", "Wilson"),
            new UserData("eve", "eve@example.com", "eve123", "Eve", "Davis")
        );
        
        Role userRole = roleRepository.findByName("ROLE_USER")
            .orElseThrow(() -> new RuntimeException("User role not found"));
        
        for (UserData userData : regularUsers) {
            if (!userRepository.existsByUsername(userData.username)) {
                User user = createUser(userData.username, userData.email, userData.password,
                                     userData.firstName, userData.lastName);
                user.getRoles().add(userRole);
                
                userRepository.save(user);
                System.out.println("Created user: " + userData.username + "/" + userData.password);
            }
        }
        
        System.out.println("Data initialization completed successfully!");
        System.out.println("You can now login with any of the following accounts:");
        System.out.println("- Admin: admin/admin123");
        System.out.println("- Manager: manager/manager123");
        System.out.println("- Moderator: moderator/moderator123");
        System.out.println("- Users: alice/alice123, bob/bob123, charlie/charlie123, diana/diana123, eve/eve123");
    }
    
    private User createUser(String username, String email, String password,
                          String firstName, String lastName) {
        return User.builder()
            .username(username)
            .email(email)
            .password(passwordEncoder.encode(password))
            .firstName(firstName)
            .lastName(lastName)
            .build();
    }
    
    private static class UserData {
        final String username;
        final String email;
        final String password;
        final String firstName;
        final String lastName;
        
        UserData(String username, String email, String password, String firstName, String lastName) {
            this.username = username;
            this.email = email;
            this.password = password;
            this.firstName = firstName;
            this.lastName = lastName;
        }
    }
}
