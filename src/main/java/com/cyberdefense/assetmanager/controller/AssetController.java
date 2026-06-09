package com.cyberdefense.assetmanager.controller;

import com.cyberdefense.assetmanager.config.SecurityLogger;
import com.cyberdefense.assetmanager.dto.AssetResponseDTO;
import com.cyberdefense.assetmanager.dto.FirewallDTO;
import com.cyberdefense.assetmanager.dto.ServerDTO;
import com.cyberdefense.assetmanager.entity.AssetIT;
import com.cyberdefense.assetmanager.entity.Firewall;
import com.cyberdefense.assetmanager.entity.Server;
import com.cyberdefense.assetmanager.service.AssetService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/assets")
public class AssetController {

    private final AssetService assetService;
    private final SecurityLogger securityLogger;

    public AssetController(AssetService assetService, SecurityLogger securityLogger) {
        this.assetService = assetService;
        this.securityLogger = securityLogger;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST', 'VIEWER')")
    public ResponseEntity<List<AssetResponseDTO>> getTuttiAsset() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        securityLogger.logAccesso("GET_ASSETS", "Recupero lista completa asset da " + auth.getName());

        List<AssetResponseDTO> assets = assetService.trovatutti()
                .stream()
                .map(AssetResponseDTO::fromEntity)
                .toList();

        return ResponseEntity.ok(assets);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST', 'VIEWER')")
    public ResponseEntity<AssetResponseDTO> getAssetPerId(@PathVariable Long id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        securityLogger.logAccesso("GET_ASSET", "Recupero asset id=" + id + " da " + auth.getName());

        return assetService.trovaPerId(id)
                .map(asset -> ResponseEntity.ok(AssetResponseDTO.fromEntity(asset)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/servers")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> creaServer(@Valid @RequestBody ServerDTO dto) {
        // prima di salvare controllo che l'IP non sia già usato da un altro asset
        // così evito duplicati nell'inventario
        if (assetService.ipGiaUsato(dto.getIndirizzoIp())) {
            securityLogger.logViolazione("CREA_SERVER", "Tentativo di duplicare IP " + dto.getIndirizzoIp());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("errore", "L'indirizzo IP è già registrato a un altro asset"));
        }

        Server server = new Server();
        server.setIndirizzoIp(dto.getIndirizzoIp());
        server.setSistemaOperativo(dto.getSistemaOperativo());
        server.setHostname(dto.getHostname());
        server.setRuolo(dto.getRuolo());
        server.setAmbiente(dto.getAmbiente());

        AssetIT salvato = assetService.salva(server);
        securityLogger.logModifica("CREA_SERVER", "Server", salvato.getId().toString());

        return ResponseEntity.status(HttpStatus.CREATED).body(AssetResponseDTO.fromEntity(salvato));
    }

    @PostMapping("/firewalls")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> creaFirewall(@Valid @RequestBody FirewallDTO dto) {
        if (assetService.ipGiaUsato(dto.getIndirizzoIp())) {
            securityLogger.logViolazione("CREA_FIREWALL", "Tentativo di duplicare IP " + dto.getIndirizzoIp());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("errore", "L'indirizzo IP è già registrato a un altro asset"));
        }

        Firewall firewall = new Firewall();
        firewall.setIndirizzoIp(dto.getIndirizzoIp());
        firewall.setSistemaOperativo(dto.getSistemaOperativo());
        firewall.setMarca(dto.getMarca());
        firewall.setFirmware(dto.getFirmware());
        firewall.setZona(dto.getZona());

        AssetIT salvato = assetService.salva(firewall);
        securityLogger.logModifica("CREA_FIREWALL", "Firewall", salvato.getId().toString());

        return ResponseEntity.status(HttpStatus.CREATED).body(AssetResponseDTO.fromEntity(salvato));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> eliminaAsset(@PathVariable Long id) {
        if (!assetService.esiste(id)) {
            return ResponseEntity.notFound().build();
        }

        assetService.elimina(id);
        securityLogger.logModifica("ELIMINA_ASSET", "Asset", id.toString());

        return ResponseEntity.noContent().build();
    }
}