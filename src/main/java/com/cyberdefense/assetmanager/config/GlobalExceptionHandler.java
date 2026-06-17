package com.cyberdefense.assetmanager.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

// rete di sicurezza centralizzata per le eccezioni non gestite localmente nei controller
// l'obiettivo è restituire status code corretti e messaggi generici, senza mai esporre stack trace
@RestControllerAdvice
public class GlobalExceptionHandler {

    // chiave usata in tutte le response di errore, la tengo costante per non ripeterla
    private static final String CHIAVE_ERRORE = "errore";

    // quando un @Valid fallisce restituisco 400 con i campi che non vanno
    // senza questo handler la violazione di validazione finirebbe come 403, fuorviante
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidazione(MethodArgumentNotValidException ex) {
        Map<String, String> errori = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errori.put(error.getField(), error.getDefaultMessage())
        );

        Map<String, Object> response = Map.of(
                CHIAVE_ERRORE, "Dati non validi",
                "campi", errori
        );
        return ResponseEntity.badRequest().body(response);
    }

    // accesso negato da @PreAuthorize, restituisco 403 pulito senza dettagli interni
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAccessoNegato(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of(CHIAVE_ERRORE, "Accesso negato"));
    }

    // fallback per tutto il resto: meglio un 500 generico che uno stack trace esposto al client
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGenerico(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(CHIAVE_ERRORE, "Errore interno del server"));
    }
}