package com.merlette.backend.service;

import com.merlette.backend.dto.AuthResponse;
import com.merlette.backend.dto.LoginRequest;
import com.merlette.backend.dto.MeDto;
import com.merlette.backend.dto.RegisterRequest;
import com.merlette.backend.entity.User;
import com.merlette.backend.exception.ApiException;
import com.merlette.backend.repository.UserRepository;
import com.merlette.backend.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflict("Cet email est déjà utilisé.");
        }
        if (userRepository.existsByUsernameIgnoreCase(request.username())) {
            throw ApiException.conflict("Ce nom d'utilisateur est déjà pris.");
        }
        User user = new User();
        user.setUsername(request.username());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        userRepository.save(user);
        return new AuthResponse(jwtService.createToken(user), MeDto.from(user));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmailIgnoreCase(request.email().trim())
                .filter(u -> passwordEncoder.matches(request.password(), u.getPasswordHash()))
                // même message que l'email soit inconnu ou le mot de passe faux
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS",
                        "Email ou mot de passe incorrect."));
        return new AuthResponse(jwtService.createToken(user), MeDto.from(user));
    }
}
