package com.cyberdefense.assetmanager.controller;

import com.cyberdefense.assetmanager.config.SecurityLogger;
import com.cyberdefense.assetmanager.service.NvdService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cve")
public class CveController {

    private final NvdService nvdService;
    private final SecurityLogger securityLogger;

    public CveController(NvdService nvdService, SecurityLogger securityLogger) {
        this.nvdService = nvdService;
        this.securityLogger = securityLogger;
    }

    // ricerca CVE pubbliche per parola chiave
    // tutti i ruoli possono cercare, è informazione pubblica già disponibile online
    // ma logga chi cerca cosa per audit interno
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST', 'VIEWER')")
    public ResponseEntity<?> cercaCve(@RequestParam String keyword) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // controllo che la keyword non sia vuota o troppo corta per evitare query inutili
        if (keyword == null || keyword.trim().length() < 3) {
            return ResponseEntity.badRequest().body(Map.of("errore", "La keyword deve avere almeno 3 caratteri"));
        }

        securityLogger.logAccesso("CERCA_CVE", "Ricerca CVE '" + keyword + "' da " + auth.getName());

        List<Map<String, Object>> risultati = nvdService.cercaCve(keyword);

        return ResponseEntity.ok(Map.of(
                "keyword", keyword,
                "totale", risultati.size(),
                "risultati", risultati
        ));
    }
}