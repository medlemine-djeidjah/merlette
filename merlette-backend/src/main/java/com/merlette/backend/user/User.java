package com.merlette.backend.user;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor
public class User {
    public enum Role { USER, ADMIN }
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private String username;
    private String email;
    @Column(name = "password_hash")
    private String passwordHash;
    @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(columnDefinition = "user_role")
    private Role role = Role.USER;
    private String bio;
    private String avatarUrl;
    private Instant createdAt = Instant.now();

    public User(String username, String email, String passwordHash) {
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
    }
}
