package com.cyberdefense.assetmanager.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "users")
@Getter
@Setter
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // username con cui l'operatore fa login, deve essere unico
    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    // dati anagrafici dell'operatore, utili per identificarlo nei log e nei report
    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private String cognome;

    // URL dell'immagine profilo, può essere aggiornata dall'utente dopo la registrazione
    @Column
    private String immagineProfilo;

    // data di registrazione, viene impostata automaticamente al salvataggio
    @Column(nullable = false, updatable = false)
    private LocalDateTime dataRegistrazione;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles;

    // viene chiamato prima del primo salvataggio nel DB
    // così la data di registrazione la imposta JPA in automatico
    @PrePersist
    protected void onCreate() {
        this.dataRegistrazione = LocalDateTime.now();
    }
}