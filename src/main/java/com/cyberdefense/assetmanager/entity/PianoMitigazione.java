package com.cyberdefense.assetmanager.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "piani_mitigazione")
@Getter
@Setter
public class PianoMitigazione {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // il ticket a cui si riferisce il piano
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false)
    private TicketVulnerabilita ticket;

    // il testo del piano generato dall'AI
    @Column(nullable = false, length = 5000)
    private String contenuto;

    // chi ha richiesto la generazione del piano
    @Column(nullable = false)
    private String richiestoDa;

    // stato del piano: PENDING in attesa, APPROVED approvato, REJECTED rifiutato
    // qui sta il senso del Human-in-the-Loop, l'AI propone ma l'umano decide
    @Column(nullable = false)
    private String stato;

    // chi ha approvato o rifiutato, valorizzato solo dopo la revisione
    @Column
    private String revisionatoDa;

    // motivo del rifiuto o note di approvazione, utile per audit
    @Column(length = 500)
    private String noteRevisione;

    @Column(nullable = false, updatable = false)
    private LocalDateTime dataGenerazione;

    @Column
    private LocalDateTime dataRevisione;

    @PrePersist
    protected void onCreate() {
        this.dataGenerazione = LocalDateTime.now();
        this.stato = "PENDING";
    }
}