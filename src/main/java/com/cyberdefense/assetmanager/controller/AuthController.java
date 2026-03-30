package com.cyberdefense.assetmanager.controller;

import com.cyberdefense.assetmanager.config.JwtService;
import com.cyberdefense.assetmanager.config.SecurityLogger;
import com.cyberdefense.assetmanager.dto.LoginRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final SecurityLogger securityLogger;

    public AuthController(JwtService jwtService, UserDetailsService userDetailsService, PasswordEncoder passwordEncoder, SecurityLogger securityLogger) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
        this.securityLogger = securityLogger;
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@Valid @RequestBody LoginRequest request) {
        try {
            UserDetails user = userDetailsService.loadUserByUsername(request.getUsername());

            if (passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                String token = jwtService.generateToken(request.getUsername());

                securityLogger.logAccesso("LOGIN", "Autenticazione riuscita per " + request.getUsername());

                Map<String, String> response = new HashMap<>();
                response.put("token", token);
                return ResponseEntity.ok(response);
            } else {
                securityLogger.logViolazione("LOGIN", "Password errata per " + request.getUsername());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
        } catch (Exception e) {
            securityLogger.logViolazione("LOGIN", "Utente non trovato o errore interno");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }
}