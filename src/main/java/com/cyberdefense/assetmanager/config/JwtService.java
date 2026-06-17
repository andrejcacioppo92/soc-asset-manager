package com.cyberdefense.assetmanager.config;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    // la chiave non è più generata in RAM ma caricata dall'ambiente
    // così sopravvive ai restart ed è uguale su tutte le istanze (stateless reale)
    private final SecretKey secretKey;

    // issuer e audience identificano chi emette e chi può usare il token
    // li valido in fase di parsing per rifiutare token non destinati a questo servizio
    private static final String ISSUER = "soc-asset-manager";
    private static final String AUDIENCE = "soc-dashboard-client";

    // scadenza del token: 1 ora
    private static final long JWT_EXPIRATION = 3600000;

    // tolleranza di 30 secondi per piccoli disallineamenti di orologio tra server
    private static final long CLOCK_SKEW_SECONDS = 30;

    public JwtService(@Value("${jwt.secret}") String secret) {
        // il segreto arriva in Base64 dall'ambiente, lo decodifico nei byte reali della chiave
        byte[] keyBytes = Base64.getDecoder().decode(secret);
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(String username) {
        long now = System.currentTimeMillis();

        return Jwts.builder()
                .subject(username)
                .issuer(ISSUER)
                .audience().add(AUDIENCE).and()
                // jti è un id univoco del token, utile per tracciamento e per eventuale revoca futura
                .id(UUID.randomUUID().toString())
                .issuedAt(new Date(now))
                // nbf: il token non è valido prima di adesso
                .notBefore(new Date(now))
                .expiration(new Date(now + JWT_EXPIRATION))
                .signWith(secretKey)
                .compact();
    }

    public String extractUsername(String token) {
        return parseToken(token).getSubject();
    }

    public boolean isTokenValid(String token) {
        try {
            // se firma, issuer, audience, scadenza o nbf non tornano, il parser lancia eccezione
            parseToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // parsing centralizzato in un solo punto, con tutte le verifiche di sicurezza
    private io.jsonwebtoken.Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .requireIssuer(ISSUER)
                .requireAudience(AUDIENCE)
                .clockSkewSeconds(CLOCK_SKEW_SECONDS)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}