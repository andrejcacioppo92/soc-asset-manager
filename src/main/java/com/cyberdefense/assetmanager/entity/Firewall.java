package com.cyberdefense.assetmanager.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "firewalls")
@Getter
@Setter
public class Firewall extends AssetIT {

    @Column(nullable = false)
    private String marca;

    @Column(nullable = false)
    private String firmware;

    // Zona di rete protetta (es. "DMZ", "LAN Interna", "Perimetrale")
    @Column(nullable = false)
    private String zona;
}