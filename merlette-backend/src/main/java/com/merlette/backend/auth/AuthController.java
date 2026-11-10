package com.merlette.backend.auth;

import com.merlette.backend.api.ApiException;
import com.merlette.backend.user.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService auth;
    private final UserRepository users;
    public AuthController(AuthService auth, UserRepository users) { this.auth = auth; this.users = users; }
    @PostMapping("/register") @ResponseStatus(HttpStatus.CREATED)
    public AuthService.Session register(@Valid @RequestBody AuthService.Registration input) { return auth.register(input); }
    @PostMapping("/login")
    public AuthService.Session login(@Valid @RequestBody AuthService.Login input) { return auth.login(input); }
    @GetMapping("/me")
    public AuthService.UserView me(@AuthenticationPrincipal Jwt jwt) {
        return AuthService.UserView.of(users.findById(Integer.valueOf(jwt.getSubject())).orElseThrow(ApiException::notFound));
    }
}
