package com.cyberdefense.assetmanager.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class FirewallDTO {

    @NotBlank(message = "L'indirizzo IP è obbligatorio")
    @Size(max = 45, message = "L'indirizzo IP non può superare i 45 caratteri")
    private String indirizzoIp;

    @NotBlank(message = "Il sistema operativo è obbligatorio")
    @Size(max = 100, message = "Il sistema operativo non può superare i 100 caratteri")
    private String sistemaOperativo;

    @NotBlank(message = "La marca è obbligatoria")
    @Size(max = 100, message = "La marca non può superare i 100 caratteri")
    private String marca;

    @NotBlank(message = "Il firmware è obbligatorio")
    @Size(max = 100, message = "Il firmware non può superare i 100 caratteri")
    private String firmware;

    @NotBlank(message = "La zona è obbligatoria")
    @Size(max = 100, message = "La zona non può superare i 100 caratteri")
    private String zona;

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

    public String getMarca() {
        return marca;
    }

    public void setMarca(String marca) {
        this.marca = marca;
    }

    public String getFirmware() {
        return firmware;
    }

    public void setFirmware(String firmware) {
        this.firmware = firmware;
    }

    public String getZona() {
        return zona;
    }

    public void setZona(String zona) {
        this.zona = zona;
    }
}