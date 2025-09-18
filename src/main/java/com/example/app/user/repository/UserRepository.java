package com.example.app.user.repository;
import com.example.app.email.entity.TokenType;

import com.example.app.user.entity.User;
import com.example.app.email.entity.TokenType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for User entity operations
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    /**
     * Find user by username
     */
    Optional<User> findByUsername(String username);
    
    /**
     * Find user by email
     */
    Optional<User> findByEmail(String email);
    
    /**
     * Find user by username or email
     */
    Optional<User> findByUsernameOrEmail(String username, String email);
    
    /**
     * Check if username exists
     */
    Boolean existsByUsername(String username);
    
    /**
     * Check if email exists
     */
    Boolean existsByEmail(String email);
    
    /**
     * Find all enabled users
     */
    List<User> findByEnabledTrue();
    
    /**
     * Find all disabled users
     */
    List<User> findByEnabledFalse();
    
    /**
     * Find users by role name
     */
    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.name = :roleName")
    List<User> findByRoleName(@Param("roleName") String roleName);
    
    /**
     * Find users created after a specific date
     */
    List<User> findByCreatedAtAfter(LocalDateTime date);
    
    /**
     * Find users by last login after a specific date
     */
    List<User> findByLastLoginAfter(LocalDateTime date);
    
    /**
     * Update user's last login time
     */
    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.lastLogin = :lastLogin WHERE u.id = :userId")
    void updateLastLogin(@Param("userId") Long userId, @Param("lastLogin") LocalDateTime lastLogin);
    
    /**
     * Enable or disable user account
     */
    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.enabled = :enabled WHERE u.id = :userId")
    void updateUserEnabled(@Param("userId") Long userId, @Param("enabled") Boolean enabled);
    
    /**
     * Lock or unlock user account
     */
    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.accountNonLocked = :accountNonLocked WHERE u.id = :userId")
    void updateUserLocked(@Param("userId") Long userId, @Param("accountNonLocked") Boolean accountNonLocked);
    
    /**
     * Find users with specific name pattern (case-insensitive)
     */
    @Query("SELECT u FROM User u WHERE LOWER(u.firstName) LIKE LOWER(CONCAT('%', :name, '%')) " +
           "OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<User> findByNameContaining(@Param("name") String name);
    
    /**
     * Count active users (enabled and not expired)
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.enabled = true AND u.accountNonExpired = true")
    Long countActiveUsers();
    
    /**
     * Find users by multiple criteria
     */
    @Query("SELECT u FROM User u WHERE " +
           "(:username IS NULL OR u.username LIKE %:username%) AND " +
           "(:email IS NULL OR u.email LIKE %:email%) AND " +
           "(:enabled IS NULL OR u.enabled = :enabled)")
    List<User> findUsersByCriteria(@Param("username") String username, 
                                   @Param("email") String email, 
                                   @Param("enabled") Boolean enabled);
}
