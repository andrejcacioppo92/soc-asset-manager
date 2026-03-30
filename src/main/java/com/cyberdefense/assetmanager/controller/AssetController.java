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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    public ResponseEntity<List<AssetResponseDTO>> getTuttiAsset() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            securityLogger.logViolazione("GET_ASSETS", "Tentativo di accesso non autenticato");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        securityLogger.logAccesso("GET_ASSETS", "Recupero lista completa asset");

        List<AssetResponseDTO> assets = assetService.trovatutti()
                .stream()
                .map(AssetResponseDTO::fromEntity)
                .toList();

        return ResponseEntity.ok(assets);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AssetResponseDTO> getAssetPerId(@PathVariable Long id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            securityLogger.logViolazione("GET_ASSET", "Tentativo di accesso non autenticato");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        securityLogger.logAccesso("GET_ASSET", "Recupero asset id=" + id);

        return assetService.trovaPerId(id)
                .map(asset -> ResponseEntity.ok(AssetResponseDTO.fromEntity(asset)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/servers")
    public ResponseEntity<AssetResponseDTO> creaServer(@Valid @RequestBody ServerDTO dto) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!"soc_admin".equals(auth.getName())) {
            securityLogger.logViolazione("CREA_SERVER", "Operatore non autorizzato");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
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
    public ResponseEntity<AssetResponseDTO> creaFirewall(@Valid @RequestBody FirewallDTO dto) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!"soc_admin".equals(auth.getName())) {
            securityLogger.logViolazione("CREA_FIREWALL", "Operatore non autorizzato");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
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
    public ResponseEntity<Void> eliminaAsset(@PathVariable Long id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!"soc_admin".equals(auth.getName())) {
            securityLogger.logViolazione("ELIMINA_ASSET", "Operatore non autorizzato per id=" + id);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        if (!assetService.esiste(id)) {
            return ResponseEntity.notFound().build();
        }

        assetService.elimina(id);
        securityLogger.logModifica("ELIMINA_ASSET", "Asset", id.toString());

        return ResponseEntity.noContent().build();
    }
}