package com.cyberdefense.assetmanager.dto;

import com.cyberdefense.assetmanager.entity.TicketVulnerabilita;

public class TicketResponseDTO {

    private Long id;
    private String descrizione;
    private String gravita;
    private String stato;
    private Long assetId;
    private String assetIp;

    public static TicketResponseDTO fromEntity(TicketVulnerabilita ticket) {
        TicketResponseDTO dto = new TicketResponseDTO();
        dto.setId(ticket.getId());
        dto.setDescrizione(ticket.getDescrizione());
        dto.setGravita(ticket.getGravita());
        dto.setStato(ticket.getStato());
        dto.setAssetId(ticket.getAsset().getId());
        dto.setAssetIp(ticket.getAsset().getIndirizzoIp());
        return dto;
    }

    // Getter e Setter

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDescrizione() {
        return descrizione;
    }

    public void setDescrizione(String descrizione) {
        this.descrizione = descrizione;
    }

    public String getGravita() {
        return gravita;
    }

    public void setGravita(String gravita) {
        this.gravita = gravita;
    }

    public String getStato() {
        return stato;
    }

    public void setStato(String stato) {
        this.stato = stato;
    }

    public Long getAssetId() {
        return assetId;
    }

    public void setAssetId(Long assetId) {
        this.assetId = assetId;
    }

    public String getAssetIp() {
        return assetIp;
    }

    public void setAssetIp(String assetIp) {
        this.assetIp = assetIp;
    }
}