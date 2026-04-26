package com.psd.entity;

import com.psd.enums.UserRole;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Getter
@Setter
@AllArgsConstructor
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Будет логином
    @Column(nullable = false, unique = true)
    private String email;

    private String name;

    // BCrypt hash
    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private Boolean enabled = true;

    @Enumerated(EnumType.STRING)
    private UserRole role;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public User() {
    }
    public boolean hasRole(UserRole role) {
        return this.role == role;
    }

    public User(String email, String name, String password, UserRole role) {
        this.email = email;
        this.name = name;
        this.password = password;
        this.role = role;
        this.enabled = true;
        this.createdAt = LocalDateTime.now(ZoneId.of("Asia/Shanghai"));
    }
}