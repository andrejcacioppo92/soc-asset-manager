package com.cyberdefense.assetmanager.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityLogger {

    private static final Logger log = LoggerFactory.getLogger("SECURITY_AUDIT");

    // Registra un accesso riuscito
    public void logAccesso(String operazione, String dettaglio) {
        String operatore = getOperatoreCorrente();
        log.info("[ACCESSO] operatore={} | operazione={} | dettaglio={}", operatore, operazione, dettaglio);
    }

    // Registra una modifica ai dati (creazione, modifica, cancellazione)
    public void logModifica(String operazione, String risorsa, String id) {
        String operatore = getOperatoreCorrente();
        log.info("[MODIFICA] operatore={} | operazione={} | risorsa={} | id={}", operatore, operazione, risorsa, id);
    }

    // Registra un tentativo di accesso non autorizzato
    public void logViolazione(String operazione, String motivo) {
        String operatore = getOperatoreCorrente();
        log.warn("[VIOLAZIONE] operatore={} | operazione={} | motivo={}", operatore, operazione, motivo);
    }

    // Registra un errore di sicurezza (token invalido, eccezione)
    public void logErroreSicurezza(String operazione, String errore) {
        String operatore = getOperatoreCorrente();
        log.error("[ERRORE_SICUREZZA] operatore={} | operazione={} | errore={}", operatore, operazione, errore);
    }

    private String getOperatoreCorrente() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            return auth.getName();
        }
        return "ANONIMO";
    }
}