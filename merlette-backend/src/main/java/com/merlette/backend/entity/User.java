package com.merlette.backend.entity;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "users")
@Getter
@Setter
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String username;

    private String email;

    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private Role role = Role.USER;

    private String avatarUrl;

    private String bio;

    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = TimeUtils.now();
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }
}
