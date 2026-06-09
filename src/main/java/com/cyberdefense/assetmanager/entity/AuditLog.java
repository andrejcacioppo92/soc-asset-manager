package com.cyberdefense.assetmanager.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_log")
@Getter
@Setter
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // chi ha fatto l'operazione, salvo l'username preso dal token JWT
    @Column(nullable = false)
    private String operatore;

    // tipo di azione registrata, es. LOGIN_OK, CREA_TICKET, ELIMINA_ASSET
    @Column(nullable = false)
    private String azione;

    // dettagli aggiuntivi sull'operazione per le indagini forensi
    @Column(length = 1000)
    private String dettagli;

    // categoria per filtrare velocemente per tipo di evento
    @Column(nullable = false)
    private String categoria;

    // timestamp dell'evento, impostato automaticamente alla creazione
    @Column(nullable = false, updatable = false)
    private LocalDateTime timestamp;

    @PrePersist
    protected void onCreate() {
        this.timestamp = LocalDateTime.now();
    }
}