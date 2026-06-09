package com.cyberdefense.assetmanager.dto;

import com.cyberdefense.assetmanager.entity.User;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Setter
public class UserResponseDTO {

    private Long id;
    private String email;
    private String nome;
    private String cognome;
    private String immagineProfilo;
    private LocalDateTime dataRegistrazione;
    private Set<String> ruoli;

    // factory method che converte l'entity in DTO senza esporre la password
    // i ruoli li mappo solo per nome, l'entity completa resta nascosta al client
    public static UserResponseDTO fromEntity(User user) {
        UserResponseDTO dto = new UserResponseDTO();
        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setNome(user.getNome());
        dto.setCognome(user.getCognome());
        dto.setImmagineProfilo(user.getImmagineProfilo());
        dto.setDataRegistrazione(user.getDataRegistrazione());
        dto.setRuoli(user.getRoles().stream()
                .map(r -> r.getName())
                .collect(Collectors.toSet()));
        return dto;
    }
}