package com.cyberdefense.assetmanager.controller;

import com.cyberdefense.assetmanager.config.SecurityLogger;
import com.cyberdefense.assetmanager.dto.AiMitigationResponseDTO;
import com.cyberdefense.assetmanager.service.GeminiService;
import com.cyberdefense.assetmanager.service.TicketService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
public class AiMitigationController {

    private final GeminiService geminiService;
    private final TicketService ticketService;
    private final SecurityLogger securityLogger;

    public AiMitigationController(GeminiService geminiService, TicketService ticketService, SecurityLogger securityLogger) {
        this.geminiService = geminiService;
        this.ticketService = ticketService;
        this.securityLogger = securityLogger;
    }

    @GetMapping("/mitigate/{ticketId}")
    public ResponseEntity<AiMitigationResponseDTO> getAiMitigation(@PathVariable Long ticketId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentOperator = authentication.getName();

        if (!"soc_admin".equals(currentOperator)) {
            securityLogger.logViolazione("AI_MITIGATION", "Operatore non autorizzato per ticket id=" + ticketId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ticketService.trovaPerId(ticketId)
                .map(ticket -> {
                    securityLogger.logAccesso("AI_MITIGATION", "Richiesta mitigazione per ticket id=" + ticketId);

                    String descrizione = "Vulnerabilità: " + ticket.getDescrizione()
                            + ". Gravità: " + ticket.getGravita()
                            + ". Asset colpito: " + ticket.getAsset().getIndirizzoIp()
                            + " (" + ticket.getAsset().getSistemaOperativo() + ")";

                    String pianoMitigazione = geminiService.generaPianoMitigazione(descrizione);

                    securityLogger.logModifica("AI_MITIGATION", "Piano generato per ticket", ticketId.toString());

                    AiMitigationResponseDTO response = new AiMitigationResponseDTO();
                    response.setTicketId(ticketId.toString());
                    response.setAssetIp(ticket.getAsset().getIndirizzoIp());
                    response.setGravita(ticket.getGravita());
                    response.setAiModel("Google Gemini 2.5 Flash");
                    response.setStatus("SUCCESS");
                    response.setMitigationPlan(pianoMitigazione);

                    return ResponseEntity.ok(response);
                })
                .orElseGet(() -> {
                    securityLogger.logViolazione("AI_MITIGATION", "Ticket id=" + ticketId + " non trovato");
                    return ResponseEntity.notFound().build();
                });
    }
}