package com.cyberdefense.assetmanager.controller;

import com.cyberdefense.assetmanager.config.SecurityLogger;
import com.cyberdefense.assetmanager.dto.TicketDTO;
import com.cyberdefense.assetmanager.dto.TicketResponseDTO;
import com.cyberdefense.assetmanager.entity.AssetIT;
import com.cyberdefense.assetmanager.entity.Gravita;
import com.cyberdefense.assetmanager.entity.StatoTicket;
import com.cyberdefense.assetmanager.entity.TicketVulnerabilita;
import com.cyberdefense.assetmanager.service.AssetService;
import com.cyberdefense.assetmanager.service.TicketService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService ticketService;
    private final AssetService assetService;
    private final SecurityLogger securityLogger;

    public TicketController(TicketService ticketService, AssetService assetService, SecurityLogger securityLogger) {
        this.ticketService = ticketService;
        this.assetService = assetService;
        this.securityLogger = securityLogger;
    }

    // endpoint principale per leggere i ticket, accetta filtri opzionali
    // se non passo niente restituisco tutto, altrimenti applico i filtri richiesti
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST', 'VIEWER')")
    public ResponseEntity<List<TicketResponseDTO>> getTickets(
            @RequestParam(required = false) Gravita gravita,
            @RequestParam(required = false) StatoTicket stato,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        securityLogger.logAccesso("GET_TICKETS", "Lista ticket da " + auth.getName() + " filtri=" + gravita + "/" + stato);

        // costruisco l'ordinamento dinamicamente, di default decrescente per ID
        Sort.Direction dir = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sort = Sort.by(dir, sortBy);

        List<TicketResponseDTO> tickets = ticketService.filtra(gravita, stato, sort)
                .stream()
                .map(TicketResponseDTO::fromEntity)
                .toList();

        return ResponseEntity.ok(tickets);
    }

    // aggregazioni per la dashboard, conteggi raggruppati per gravità e stato
    // il front-end le usa per disegnare grafici e indicatori
    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST', 'VIEWER')")
    public ResponseEntity<Map<String, Map<String, Long>>> getStatistiche() {
        Map<String, Map<String, Long>> stats = Map.of(
                "perGravita", ticketService.contaPerGravita(),
                "perStato", ticketService.contaPerStato()
        );
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST', 'VIEWER')")
    public ResponseEntity<TicketResponseDTO> getTicketPerId(@PathVariable Long id) {
        return ticketService.trovaPerId(id)
                .map(ticket -> ResponseEntity.ok(TicketResponseDTO.fromEntity(ticket)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/asset/{assetId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST', 'VIEWER')")
    public ResponseEntity<List<TicketResponseDTO>> getTicketPerAsset(@PathVariable Long assetId) {
        List<TicketResponseDTO> tickets = ticketService.trovaPerAssetId(assetId)
                .stream()
                .map(TicketResponseDTO::fromEntity)
                .toList();

        return ResponseEntity.ok(tickets);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    public ResponseEntity<TicketResponseDTO> creaTicket(@Valid @RequestBody TicketDTO dto) {
        Optional<AssetIT> assetOpt = assetService.trovaPerId(dto.getAssetId());
        if (assetOpt.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        TicketVulnerabilita ticket = new TicketVulnerabilita();
        ticket.setDescrizione(dto.getDescrizione());
        ticket.setGravita(dto.getGravita());
        ticket.setStato(StatoTicket.OPEN);
        ticket.setAsset(assetOpt.get());

        TicketVulnerabilita salvato = ticketService.salva(ticket);
        securityLogger.logModifica("CREA_TICKET", "Ticket", salvato.getId().toString());

        return ResponseEntity.status(HttpStatus.CREATED).body(TicketResponseDTO.fromEntity(salvato));
    }

    @PatchMapping("/{id}/stato")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    public ResponseEntity<?> cambiaStato(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Optional<TicketVulnerabilita> ticketOpt = ticketService.trovaPerId(id);
        if (ticketOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        StatoTicket nuovoStato;
        try {
            nuovoStato = StatoTicket.valueOf(body.get("stato"));
        } catch (IllegalArgumentException | NullPointerException e) {
            return ResponseEntity.badRequest().body(Map.of("errore", "Stato non valido"));
        }

        TicketVulnerabilita ticket = ticketOpt.get();
        StatoTicket statoCorrente = ticket.getStato();

        if (!statoCorrente.puoPassareA(nuovoStato)) {
            securityLogger.logViolazione("CAMBIO_STATO", "Transizione illegale " + statoCorrente + " -> " + nuovoStato + " su ticket " + id);
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("errore", "Transizione non consentita da " + statoCorrente + " a " + nuovoStato));
        }

        ticket.setStato(nuovoStato);
        TicketVulnerabilita salvato = ticketService.salva(ticket);
        securityLogger.logModifica("CAMBIO_STATO", "Ticket id=" + id, statoCorrente + " -> " + nuovoStato);

        return ResponseEntity.ok(TicketResponseDTO.fromEntity(salvato));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> eliminaTicket(@PathVariable Long id) {
        if (ticketService.trovaPerId(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        ticketService.elimina(id);
        securityLogger.logModifica("ELIMINA_TICKET", "Ticket", id.toString());

        return ResponseEntity.noContent().build();
    }
}