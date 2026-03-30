package com.cyberdefense.assetmanager.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class LoginRequest {

    // OWASP ASVS V5.1.3: Validazione in ingresso rigorosa
    @NotBlank(message = "L'ID Operatore non può essere vuoto")
    @Size(min = 3, max = 50, message = "L'ID Operatore deve essere tra 3 e 50 caratteri")
    private String username;

    @NotBlank(message = "Il codice di sicurezza non può essere vuoto")
    @Size(min = 8, max = 128, message = "Il codice di sicurezza deve rispettare i parametri di lunghezza")
    private String password;

    // Getter e Setter
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}