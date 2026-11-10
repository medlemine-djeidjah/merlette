package com.merlette.backend.security;

import com.merlette.backend.entity.User;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Création des tokens JWT (HS256, valides 24 h). */
@Service
public class JwtService {

    private final SecretKey key;
    private final long expirationHours;

    public JwtService(SecretKey jwtSecretKey, @Value("${app.jwt.expiration-hours}") long expirationHours) {
        this.key = jwtSecretKey;
        this.expirationHours = expirationHours;
    }

    public String createToken(User user) {
        Instant now = Instant.now();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(user.getId().toString())
                .claim("username", user.getUsername())
                .claim("role", user.getRole().name())
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plus(expirationHours, ChronoUnit.HOURS)))
                .build();
        try {
            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
            jwt.sign(new MACSigner(key));
            return jwt.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException("Impossible de signer le token", e);
        }
    }
}
