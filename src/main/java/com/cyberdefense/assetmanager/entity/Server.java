package com.cyberdefense.assetmanager.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "servers")
@Getter
@Setter
public class Server extends AssetIT {

    @Column(nullable = false)
    private String hostname;

    // Ruolo del server nell'infrastruttura (es. "Web Server", "Database Server", "Mail Server")
    @Column(nullable = false)
    private String ruolo;

    // Ambiente di appartenenza (es. "Produzione", "Staging", "Sviluppo")
    @Column(nullable = false)
    private String ambiente;
}