package com.cyberdefense.assetmanager.dto;

import com.cyberdefense.assetmanager.entity.Gravita;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TicketDTO {

    @NotBlank
    @Size(min = 5, max = 500)
    private String descrizione;

    // Spring valida automaticamente che il valore sia uno dell'enum
    // se arriva "BANANA" la richiesta viene rifiutata con 400
    @NotNull
    private Gravita gravita;

    @NotNull
    private Long assetId;
}