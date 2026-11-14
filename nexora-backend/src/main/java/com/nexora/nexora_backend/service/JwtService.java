package com.nexora.nexora_backend.service;

import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long jwtExpiration;

    public JwtService(
            @Value("${security.jwt.secret}") String secret,
            @Value("${security.jwt.expiration}") long jwtExpiration) {

        byte[] keyBytes = Decoders.BASE64.decode(secret);
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.jwtExpiration = jwtExpiration;
    }

    // Generate JWT token
    public String generateToken(String subject) {

        Date currentDate = new Date();
        Date expirationDate =
                new Date(currentDate.getTime() + jwtExpiration);

        return Jwts.builder()
                .subject(subject)
                .issuedAt(currentDate)
                .expiration(expirationDate)
                .signWith(signingKey)
                .compact();
    }

    // Extract subject (email) from JWT
    public String extractSubject(String token) {
        return extractAllClaims(token).getSubject();
    }

    // Extract all claims after verifying the signature
    private Claims extractAllClaims(String token) {

        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // Check whether token belongs to the supplied email
    // and has not expired
    public boolean isTokenValid(String token, String email) {

        try {
            String tokenSubject = extractSubject(token);
            return tokenSubject.equals(email)
                    && !isTokenExpired(token);
        } catch (io.jsonwebtoken.JwtException
                 | IllegalArgumentException exception) {
            return false;
        }
    }

    private boolean isTokenExpired(String token) {
        Date expiration =
                extractAllClaims(token).getExpiration();

        return expiration.before(new Date());
    }
}