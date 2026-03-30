package com.cyberdefense.assetmanager.entity; 

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "ticket_vulnerabilita")
@Getter
@Setter
public class TicketVulnerabilita {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String descrizione; 

    @Column(nullable = false)
    private String gravita; 

    @Column(nullable = false)
    private String stato; 

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id", nullable = false)
    private AssetIT asset;
}