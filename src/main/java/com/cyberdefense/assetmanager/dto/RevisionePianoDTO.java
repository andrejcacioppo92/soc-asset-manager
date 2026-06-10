package com.cyberdefense.assetmanager.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RevisionePianoDTO {

    // accetto solo APPROVED o REJECTED, qualsiasi altro valore viene rifiutato
    // così evito che arrivi "DELETED" o stringhe inattese
    @NotBlank
    @Pattern(regexp = "APPROVED|REJECTED", message = "La decisione deve essere APPROVED o REJECTED")
    private String decisione;

    // note opzionali del revisore, limito la lunghezza per evitare abusi
    @Size(max = 500, message = "Le note non possono superare i 500 caratteri")
    private String note;
}
