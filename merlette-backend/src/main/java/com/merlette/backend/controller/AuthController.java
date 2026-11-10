package com.merlette.backend.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import com.merlette.backend.dto.AuthResponse;
import com.merlette.backend.dto.LoginRequest;
import com.merlette.backend.dto.MeDto;
import com.merlette.backend.dto.RegisterRequest;
import com.merlette.backend.security.CurrentUser;
import com.merlette.backend.service.AuthService;
import com.merlette.backend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@Tag(name = "Auth", description = "Inscription, connexion, utilisateur courant")
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserService userService;
    private final CurrentUser currentUser;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    public MeDto me() {
        return userService.me(currentUser.id());
    }
}
