package com.cyberdefense.assetmanager.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AggiornaProfiloDTO {

    // nessun campo è obbligatorio, l'utente aggiorna solo quello che vuole cambiare
    // se passa null lascio il valore vecchio
    @Size(max = 50)
    private String nome;

    @Size(max = 50)
    private String cognome;

    // url dell'immagine profilo, può essere un link esterno o un base64
    @Size(max = 500)
    private String immagineProfilo;
}