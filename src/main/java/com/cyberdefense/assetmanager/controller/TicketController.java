package com.cyberdefense.assetmanager.controller;

import com.cyberdefense.assetmanager.config.SecurityLogger;
import com.cyberdefense.assetmanager.dto.TicketDTO;
import com.cyberdefense.assetmanager.dto.TicketResponseDTO;
import com.cyberdefense.assetmanager.entity.AssetIT;
import com.cyberdefense.assetmanager.entity.TicketVulnerabilita;
import com.cyberdefense.assetmanager.service.AssetService;
import com.cyberdefense.assetmanager.service.TicketService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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

    @GetMapping
    public ResponseEntity<List<TicketResponseDTO>> getTuttiTicket() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            securityLogger.logViolazione("GET_TICKETS", "Tentativo di accesso non autenticato");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        securityLogger.logAccesso("GET_TICKETS", "Recupero lista completa ticket");

        List<TicketResponseDTO> tickets = ticketService.trovaTutti()
                .stream()
                .map(TicketResponseDTO::fromEntity)
                .toList();

        return ResponseEntity.ok(tickets);
    }

    @GetMapping("/asset/{assetId}")
    public ResponseEntity<List<TicketResponseDTO>> getTicketPerAsset(@PathVariable Long assetId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            securityLogger.logViolazione("GET_TICKETS_ASSET", "Tentativo di accesso non autenticato");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        if (!assetService.esiste(assetId)) {
            return ResponseEntity.notFound().build();
        }

        securityLogger.logAccesso("GET_TICKETS_ASSET", "Recupero ticket per asset id=" + assetId);

        List<TicketResponseDTO> tickets = ticketService.trovaPerAssetId(assetId)
                .stream()
                .map(TicketResponseDTO::fromEntity)
                .toList();

        return ResponseEntity.ok(tickets);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TicketResponseDTO> getTicketPerId(@PathVariable Long id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            securityLogger.logViolazione("GET_TICKET", "Tentativo di accesso non autenticato");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        securityLogger.logAccesso("GET_TICKET", "Recupero ticket id=" + id);

        return ticketService.trovaPerId(id)
                .map(ticket -> ResponseEntity.ok(TicketResponseDTO.fromEntity(ticket)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<TicketResponseDTO> creaTicket(@Valid @RequestBody TicketDTO dto) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!"soc_admin".equals(auth.getName())) {
            securityLogger.logViolazione("CREA_TICKET", "Operatore non autorizzato");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Optional<AssetIT> asset = assetService.trovaPerId(dto.getAssetId());
        if (asset.isEmpty()) {
            securityLogger.logViolazione("CREA_TICKET", "Asset id=" + dto.getAssetId() + " non trovato");
            return ResponseEntity.badRequest().build();
        }

        TicketVulnerabilita ticket = new TicketVulnerabilita();
        ticket.setDescrizione(dto.getDescrizione());
        ticket.setGravita(dto.getGravita());
        ticket.setStato(dto.getStato());
        ticket.setAsset(asset.get());

        TicketVulnerabilita salvato = ticketService.salva(ticket);
        securityLogger.logModifica("CREA_TICKET", "Ticket", salvato.getId().toString());

        return ResponseEntity.status(HttpStatus.CREATED).body(TicketResponseDTO.fromEntity(salvato));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminaTicket(@PathVariable Long id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!"soc_admin".equals(auth.getName())) {
            securityLogger.logViolazione("ELIMINA_TICKET", "Operatore non autorizzato per id=" + id);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        if (!ticketService.esiste(id)) {
            return ResponseEntity.notFound().build();
        }

        ticketService.elimina(id);
        securityLogger.logModifica("ELIMINA_TICKET", "Ticket", id.toString());

        return ResponseEntity.noContent().build();
    }
}