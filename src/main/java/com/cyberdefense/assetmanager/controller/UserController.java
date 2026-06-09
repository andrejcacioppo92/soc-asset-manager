package com.cyberdefense.assetmanager.controller;

import com.cyberdefense.assetmanager.config.SecurityLogger;
import com.cyberdefense.assetmanager.dto.AggiornaProfiloDTO;
import com.cyberdefense.assetmanager.dto.RegistrazioneDTO;
import com.cyberdefense.assetmanager.dto.UserResponseDTO;
import com.cyberdefense.assetmanager.entity.Role;
import com.cyberdefense.assetmanager.entity.User;
import com.cyberdefense.assetmanager.repository.RoleRepository;
import com.cyberdefense.assetmanager.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecurityLogger securityLogger;

    public UserController(UserRepository userRepository,
                          RoleRepository roleRepository,
                          PasswordEncoder passwordEncoder,
                          SecurityLogger securityLogger) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.securityLogger = securityLogger;
    }

    // registrazione nuovo utente con ruolo VIEWER di default
    // solo l'admin può registrare nuovi utenti, è un'operazione sensibile
    @PostMapping("/registra")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> registra(@Valid @RequestBody RegistrazioneDTO dto) {
        if (userRepository.findByEmail(dto.getEmail()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("errore", "Email già registrata"));
        }

        // i nuovi utenti partono con ruolo VIEWER, è il principio del minimo privilegio
        // l'admin può poi cambiare ruolo dall'endpoint dedicato
        Role viewerRole = roleRepository.findByName("VIEWER")
                .orElseThrow(() -> new RuntimeException("Ruolo VIEWER non configurato"));

        User user = new User();
        user.setEmail(dto.getEmail());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setNome(dto.getNome());
        user.setCognome(dto.getCognome());
        user.setRoles(Set.of(viewerRole));

        User salvato = userRepository.save(user);
        securityLogger.logModifica("REGISTRA_USER", "Nuovo utente " + dto.getEmail(), salvato.getId().toString());

        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponseDTO.fromEntity(salvato));
    }

    // recupero del mio profilo, ogni utente vede sé stesso
    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST', 'VIEWER')")
    public ResponseEntity<UserResponseDTO> getMioProfilo() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByEmail(auth.getName())
                .map(u -> ResponseEntity.ok(UserResponseDTO.fromEntity(u)))
                .orElse(ResponseEntity.notFound().build());
    }

    // aggiornamento del proprio profilo, non posso aggiornare profili altrui
    // questa è una protezione anti-IDOR, l'utente prende sempre l'id dal token
    @PatchMapping("/me")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST', 'VIEWER')")
    public ResponseEntity<?> aggiornaProfilo(@Valid @RequestBody AggiornaProfiloDTO dto) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Optional<User> userOpt = userRepository.findByEmail(auth.getName());

        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        User user = userOpt.get();

        // aggiorno solo i campi che l'utente ha effettivamente passato
        // se un campo è null lascio il valore esistente
        if (dto.getNome() != null && !dto.getNome().isBlank()) {
            user.setNome(dto.getNome());
        }
        if (dto.getCognome() != null && !dto.getCognome().isBlank()) {
            user.setCognome(dto.getCognome());
        }
        if (dto.getImmagineProfilo() != null) {
            user.setImmagineProfilo(dto.getImmagineProfilo());
        }

        User salvato = userRepository.save(user);
        securityLogger.logModifica("AGGIORNA_PROFILO", "Profilo " + auth.getName(), salvato.getId().toString());

        return ResponseEntity.ok(UserResponseDTO.fromEntity(salvato));
    }

    // lista di tutti gli utenti, solo admin per ovvi motivi di privacy
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponseDTO>> getTuttiUtenti() {
        List<UserResponseDTO> utenti = userRepository.findAll().stream()
                .map(UserResponseDTO::fromEntity)
                .toList();
        return ResponseEntity.ok(utenti);
    }

    // cambio ruolo di un utente, operazione critica riservata all'admin
    @PatchMapping("/{id}/ruolo")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> cambiaRuolo(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String nuovoRuolo = body.get("ruolo");
        if (nuovoRuolo == null) {
            return ResponseEntity.badRequest().body(Map.of("errore", "Campo ruolo obbligatorio"));
        }

        // valido che il ruolo richiesto esista nel sistema
        Optional<Role> roleOpt = roleRepository.findByName(nuovoRuolo);
        if (roleOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("errore", "Ruolo non riconosciuto"));
        }

        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        User user = userOpt.get();
        user.setRoles(Set.of(roleOpt.get()));
        User salvato = userRepository.save(user);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        securityLogger.logModifica("CAMBIA_RUOLO", "Utente " + user.getEmail() + " -> " + nuovoRuolo + " da " + auth.getName(), id.toString());

        return ResponseEntity.ok(UserResponseDTO.fromEntity(salvato));
    }
}