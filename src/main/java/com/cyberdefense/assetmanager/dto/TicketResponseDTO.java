package com.cyberdefense.assetmanager.dto;

import com.cyberdefense.assetmanager.entity.TicketVulnerabilita;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TicketResponseDTO {

    private Long id;
    private String descrizione;
    private String gravita;
    private String stato;
    private String assetIp;

    // factory method per convertire l'entity in DTO pulito da mandare al client
    // converto gli enum in stringa così il front-end li può leggere senza problemi
    public static TicketResponseDTO fromEntity(TicketVulnerabilita ticket) {
        TicketResponseDTO dto = new TicketResponseDTO();
        dto.setId(ticket.getId());
        dto.setDescrizione(ticket.getDescrizione());
        dto.setGravita(ticket.getGravita().name());
        dto.setStato(ticket.getStato().name());
        dto.setAssetIp(ticket.getAsset().getIndirizzoIp());
        return dto;
    }
}