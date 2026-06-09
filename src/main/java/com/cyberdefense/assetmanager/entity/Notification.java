package com.cyberdefense.assetmanager.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter
@Setter
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // destinatario della notifica, riferimento all'utente che deve riceverla
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User destinatario;

    // titolo breve della notifica, mostrato come anteprima
    @Column(nullable = false, length = 150)
    private String titolo;

    // contenuto esteso del messaggio
    @Column(nullable = false, length = 500)
    private String messaggio;

    // tipo di notifica per scegliere l'icona e il colore lato UI
    // es. INFO, ALERT, WARNING, SUCCESS
    @Column(nullable = false)
    private String tipo;

    // flag per sapere se l'operatore l'ha già letta
    @Column(nullable = false)
    private boolean letta;

    // quando è stata creata la notifica
    @Column(nullable = false, updatable = false)
    private LocalDateTime dataCreazione;

    @PrePersist
    protected void onCreate() {
        this.dataCreazione = LocalDateTime.now();
        this.letta = false;
    }
}