package com.example.app.common.entity;
import com.example.app.email.entity.TokenType;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

import com.example.app.user.entity.User;
import com.example.app.email.entity.TokenType;

/**
 * Role entity for role-based access control
 */
@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = {"id", "name"})
@ToString(exclude = "users")
public class Role {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank
    @Size(max = 50)
    @Column(name = "name", nullable = false, unique = true)
    private String name;
    
    @Size(max = 255)
    @Column(name = "description")
    private String description;
    
    @ManyToMany(mappedBy = "roles", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<User> users = new HashSet<>();
    
    // Custom constructors for convenience
    public Role(String name) {
        this.name = name;
        this.users = new HashSet<>();
    }
    
    public Role(String name, String description) {
        this.name = name;
        this.description = description;
        this.users = new HashSet<>();
    }
}

