package com.cyberdefense.assetmanager.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegistrazioneDTO {

    @NotBlank
    @Size(min = 3, max = 50)
    private String email;

    // password con lunghezza minima ragionevole per evitare credenziali deboli
    @NotBlank
    @Size(min = 8, max = 100)
    private String password;

    @NotBlank
    @Size(max = 50)
    private String nome;

    @NotBlank
    @Size(max = 50)
    private String cognome;
}