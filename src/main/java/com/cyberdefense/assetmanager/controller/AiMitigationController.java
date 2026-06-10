package com.cyberdefense.assetmanager.controller;

import com.cyberdefense.assetmanager.config.SecurityLogger;
import com.cyberdefense.assetmanager.dto.AiMitigationResponseDTO;
import com.cyberdefense.assetmanager.dto.RevisionePianoDTO;
import com.cyberdefense.assetmanager.entity.PianoMitigazione;
import com.cyberdefense.assetmanager.entity.TicketVulnerabilita;
import com.cyberdefense.assetmanager.repository.PianoMitigazioneRepository;
import com.cyberdefense.assetmanager.service.GeminiService;
import com.cyberdefense.assetmanager.service.TicketService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/ai")
public class AiMitigationController {

    private final GeminiService geminiService;
    private final TicketService ticketService;
    private final PianoMitigazioneRepository pianoRepository;
    private final SecurityLogger securityLogger;

    public AiMitigationController(GeminiService geminiService,
                                  TicketService ticketService,
                                  PianoMitigazioneRepository pianoRepository,
                                  SecurityLogger securityLogger) {
        this.geminiService = geminiService;
        this.ticketService = ticketService;
        this.pianoRepository = pianoRepository;
        this.securityLogger = securityLogger;
    }

    // genera un nuovo piano di mitigazione e lo salva come PENDING
    // qui parte il flusso Human-in-the-Loop, l'AI propone ma il piano non è attivo
    @GetMapping("/mitigate/{ticketId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    public ResponseEntity<AiMitigationResponseDTO> getAiMitigation(@PathVariable Long ticketId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentOperator = authentication.getName();

        Optional<TicketVulnerabilita> ticketOpt = ticketService.trovaPerId(ticketId);
        if (ticketOpt.isEmpty()) {
            securityLogger.logViolazione("AI_MITIGATION", "Ticket id=" + ticketId + " non trovato");
            return ResponseEntity.notFound().build();
        }

        TicketVulnerabilita ticket = ticketOpt.get();
        securityLogger.logAccesso("AI_MITIGATION", "Richiesta mitigazione ticket id=" + ticketId + " da " + currentOperator);

        // costruisco il prompt con i dati reali del ticket e dell'asset
        String descrizione = "Vulnerabilità: " + ticket.getDescrizione()
                + ". Gravità: " + ticket.getGravita().name()
                + ". Asset colpito: " + ticket.getAsset().getIndirizzoIp()
                + " (" + ticket.getAsset().getSistemaOperativo() + ")";

        String pianoTesto = geminiService.generaPianoMitigazione(descrizione);

        // salvo il piano nel database come PENDING in attesa di revisione umana
        // se domani serve sapere chi ha proposto cosa, è tutto tracciato
        PianoMitigazione piano = new PianoMitigazione();
        piano.setTicket(ticket);
        piano.setContenuto(pianoTesto);
        piano.setRichiestoDa(currentOperator);
        PianoMitigazione salvato = pianoRepository.save(piano);

        securityLogger.logModifica("AI_MITIGATION", "Piano id=" + salvato.getId() + " generato per ticket", ticketId.toString());

        AiMitigationResponseDTO response = new AiMitigationResponseDTO();
        response.setTicketId(ticketId.toString());
        response.setAssetIp(ticket.getAsset().getIndirizzoIp());
        response.setGravita(ticket.getGravita().name());
        response.setAiModel("Google Gemini 2.5 Flash");
        response.setStatus("PENDING_REVIEW");
        response.setMitigationPlan(pianoTesto);

        return ResponseEntity.ok(response);
    }

    // lista dei piani in attesa di revisione, per la dashboard del revisore
    @GetMapping("/piani/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> getPianiInAttesa() {
        List<Map<String, Object>> piani = pianoRepository.findByStato("PENDING").stream()
                .map(p -> Map.<String, Object>of(
                        "id", p.getId(),
                        "ticketId", p.getTicket().getId(),
                        "richiestoDa", p.getRichiestoDa(),
                        "dataGenerazione", p.getDataGenerazione(),
                        "contenuto", p.getContenuto()
                ))
                .toList();

        return ResponseEntity.ok(piani);
    }

    // solo l'admin può approvare o rifiutare un piano, è l'autorità di revisione
    // questo è il cuore del Human-in-the-Loop, qui l'umano firma la decisione
    // ora uso un DTO tipizzato con validazione invece di una Map generica
    @PatchMapping("/piani/{pianoId}/revisione")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> revisionaPiano(@PathVariable Long pianoId, @Valid @RequestBody RevisionePianoDTO dto) {
        Optional<PianoMitigazione> pianoOpt = pianoRepository.findById(pianoId);
        if (pianoOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        PianoMitigazione piano = pianoOpt.get();
        // non posso rivedere un piano già revisionato, evito modifiche retroattive
        if (!"PENDING".equals(piano.getStato())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("errore", "Il piano è già stato " + piano.getStato()));
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        piano.setStato(dto.getDecisione());
        piano.setRevisionatoDa(auth.getName());
        piano.setNoteRevisione(dto.getNote() != null ? dto.getNote() : "");
        piano.setDataRevisione(LocalDateTime.now());

        PianoMitigazione salvato = pianoRepository.save(piano);
        securityLogger.logModifica("REVISIONE_PIANO", "Piano id=" + pianoId + " " + dto.getDecisione() + " da " + auth.getName(), pianoId.toString());

        return ResponseEntity.ok(Map.of(
                "id", salvato.getId(),
                "stato", salvato.getStato(),
                "revisionatoDa", salvato.getRevisionatoDa(),
                "dataRevisione", salvato.getDataRevisione()
        ));
    }
}