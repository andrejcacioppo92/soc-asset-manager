package com.cyberdefense.assetmanager.dto;

import com.cyberdefense.assetmanager.entity.AssetIT;
import com.cyberdefense.assetmanager.entity.Firewall;
import com.cyberdefense.assetmanager.entity.Server;

public class AssetResponseDTO {

    private Long id;
    private String indirizzoIp;
    private String sistemaOperativo;
    private String tipo;

    // Campi specifici di Server
    private String hostname;
    private String ruolo;
    private String ambiente;

    // Campi specifici di Firewall
    private String marca;
    private String firmware;
    private String zona;

    // Converte qualsiasi asset nel DTO di risposta
    public static AssetResponseDTO fromEntity(AssetIT entity) {
        AssetResponseDTO dto = new AssetResponseDTO();
        dto.setId(entity.getId());
        dto.setIndirizzoIp(entity.getIndirizzoIp());
        dto.setSistemaOperativo(entity.getSistemaOperativo());

        if (entity instanceof Server s) {
            dto.setTipo("SERVER");
            dto.setHostname(s.getHostname());
            dto.setRuolo(s.getRuolo());
            dto.setAmbiente(s.getAmbiente());
        } else if (entity instanceof Firewall f) {
            dto.setTipo("FIREWALL");
            dto.setMarca(f.getMarca());
            dto.setFirmware(f.getFirmware());
            dto.setZona(f.getZona());
        }

        return dto;
    }

    // Getter e Setter

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
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