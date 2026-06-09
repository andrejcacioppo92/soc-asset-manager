package com.cyberdefense.assetmanager.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Getter
@Setter
public abstract class AssetIT {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // l'IP deve essere unico nel sistema, non posso avere due asset con lo stesso indirizzo
    // il vincolo è anche a livello DB così il database stesso blocca il duplicato
    @Column(nullable = false, unique = true)
    private String indirizzoIp;

    @Column(nullable = false)
    private String sistemaOperativo;
}