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

    // uso EnumType.STRING così nel DB vedo il nome dell'enum invece di un numero
    // più leggibile se devo fare query a mano
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Gravita gravita;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatoTicket stato;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id", nullable = false)
    private AssetIT asset;
}