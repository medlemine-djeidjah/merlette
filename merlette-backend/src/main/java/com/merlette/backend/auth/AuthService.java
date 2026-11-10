package com.merlette.backend.auth;

import com.merlette.backend.api.ApiException;
import com.merlette.backend.user.*;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

@Service
public class AuthService {
    public record Registration(
        @NotBlank @Pattern(regexp = "[a-zA-Z0-9_]{3,30}") String username,
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(min = 8, max = 72) String password) {}
    public record Login(@NotBlank @Email String email, @NotBlank @Size(max = 72) String password) {}
    public record UserView(Integer id, String username, String email, User.Role role,
                           String bio, String avatarUrl, Instant createdAt) {
        public static UserView of(User user) {
            return new UserView(user.getId(), user.getUsername(), user.getEmail(), user.getRole(),
                user.getBio(), user.getAvatarUrl(), user.getCreatedAt());
        }
    }
    public record Session(String token, UserView user) {}
    private final UserRepository users;
    private final PasswordEncoder passwords;
    private final JwtEncoder encoder;

    public AuthService(UserRepository users, PasswordEncoder passwords, JwtEncoder encoder) {
        this.users = users; this.passwords = passwords; this.encoder = encoder;
    }
    @Transactional
    public Session register(Registration input) {
        String email = input.email().strip().toLowerCase(Locale.ROOT);
        if (users.existsByEmailOrUsername(email, input.username())) {
            throw new ApiException(HttpStatus.CONFLICT, "CONFLICT", "Cet email ou ce pseudo est déjà utilisé.");
        }
        checkPasswordBytes(input.password());
        return session(users.saveAndFlush(new User(input.username(), email, passwords.encode(input.password()))));
    }
    public Session login(Login input) {
        var user = users.findByEmail(input.email().strip().toLowerCase(Locale.ROOT));
        // A real BCrypt comparison also runs for an unknown address.
        String hash = user.map(User::getPasswordHash).orElse(DUMMY_HASH);
        boolean matches = input.password().getBytes(java.nio.charset.StandardCharsets.UTF_8).length <= 72
            && passwords.matches(input.password(), hash);
        if (!matches || user.isEmpty()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Email ou mot de passe incorrect.");
        }
        return session(user.get());
    }
    private static final String DUMMY_HASH = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("unused-login-placeholder");
    private void checkPasswordBytes(String password) {
        if (password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            throw ApiException.invalid("Le mot de passe dépasse 72 octets UTF-8.");
        }
    }
    private Session session(User user) {
        Instant now = Instant.now();
        var claims = JwtClaimsSet.builder().issuer("merlette").subject(user.getId().toString())
            .issuedAt(now).expiresAt(now.plus(24, ChronoUnit.HOURS)).build();
        String token = encoder.encode(JwtEncoderParameters.from(
            JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        return new Session(token, UserView.of(user));
    }
}
