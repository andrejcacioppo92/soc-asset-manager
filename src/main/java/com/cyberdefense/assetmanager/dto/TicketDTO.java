package com.cyberdefense.assetmanager.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class TicketDTO {

    @NotBlank(message = "La descrizione della vulnerabilità è obbligatoria")
    @Size(max = 500, message = "La descrizione non può superare i 500 caratteri")
    private String descrizione;

    @NotBlank(message = "La gravità è obbligatoria")
    @Size(max = 50, message = "La gravità non può superare i 50 caratteri")
    private String gravita;

    @NotBlank(message = "Lo stato è obbligatorio")
    @Size(max = 50, message = "Lo stato non può superare i 50 caratteri")
    private String stato;

    @NotNull(message = "L'ID dell'asset è obbligatorio")
    private Long assetId;

    // Getter e Setter

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
}