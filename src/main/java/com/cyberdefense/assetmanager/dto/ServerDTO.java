package com.cyberdefense.assetmanager.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ServerDTO {

    @NotBlank(message = "L'indirizzo IP è obbligatorio")
    @Size(max = 45, message = "L'indirizzo IP non può superare i 45 caratteri")
    private String indirizzoIp;

    @NotBlank(message = "Il sistema operativo è obbligatorio")
    @Size(max = 100, message = "Il sistema operativo non può superare i 100 caratteri")
    private String sistemaOperativo;

    @NotBlank(message = "L'hostname è obbligatorio")
    @Size(max = 255, message = "L'hostname non può superare i 255 caratteri")
    private String hostname;

    @NotBlank(message = "Il ruolo è obbligatorio")
    @Size(max = 100, message = "Il ruolo non può superare i 100 caratteri")
    private String ruolo;

    @NotBlank(message = "L'ambiente è obbligatorio")
    @Size(max = 50, message = "L'ambiente non può superare i 50 caratteri")
    private String ambiente;

    // Getter e Setter

    public String getIndirizzoIp() {
        return indirizzoIp;
    }

    public void setIndirizzoIp(String indirizzoIp) {
        this.indirizzoIp = indirizzoIp;
    }

    public String getSistemaOperativo() {
        return sistemaOperativo;
    }

    public void setSistemaOperativo(String sistemaOperativo) {
        this.sistemaOperativo = sistemaOperativo;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getRuolo() {
        return ruolo;
    }

    public void setRuolo(String ruolo) {
        this.ruolo = ruolo;
    }

    public String getAmbiente() {
        return ambiente;
    }

    public void setAmbiente(String ambiente) {
        this.ambiente = ambiente;
    }
}