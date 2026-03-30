package com.cyberdefense.assetmanager.config;

import io.jsonwebtoken.Jwts;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

@Service
public class JwtService {

    // Generazione di una chiave HMAC a 256-bit sicura in RAM (Zero-Trust/No Hardcoded Secrets)
    private final SecretKey secretKey = Jwts.SIG.HS256.key().build();

    // Scadenza del token: 1 ora (3600000 millisecondi)
    private final long jwtExpiration = 3600000;

    public String generateToken(String username) {
        return Jwts.builder()
                .subject(username)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(secretKey)
                .compact();
    }

    public String extractUsername(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public boolean isTokenValid(String token) {
        try {
            // Se il token è stato manomesso, è scaduto o firmato con una chiave diversa,
            // il parser lancerà un'eccezione, invalidando immediatamente l'accesso.
            Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}