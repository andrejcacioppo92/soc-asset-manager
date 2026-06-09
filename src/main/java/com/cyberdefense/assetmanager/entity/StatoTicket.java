package com.cyberdefense.assetmanager.entity;

import java.util.Set;

// stati possibili del ciclo di vita di un ticket
// OPEN appena creato, IN_PROGRESS quando ci sta lavorando un analista,
// RESOLVED quando il problema è stato sistemato, CLOSED chiuso definitivamente
public enum StatoTicket {
    OPEN,
    IN_PROGRESS,
    RESOLVED,
    CLOSED;

    // ogni stato dichiara le transizioni che gli sono permesse
    // così evito che un ticket salti da OPEN direttamente a RESOLVED senza passare dal lavoro
    public Set<StatoTicket> transizioniConsentite() {
        return switch (this) {
            case OPEN -> Set.of(IN_PROGRESS, CLOSED);
            case IN_PROGRESS -> Set.of(RESOLVED, CLOSED);
            case RESOLVED -> Set.of(CLOSED, IN_PROGRESS);
            case CLOSED -> Set.of();
        };
    }

    // metodo di comodo per controllare velocemente se una transizione è valida
    public boolean puoPassareA(StatoTicket nuovoStato) {
        return transizioniConsentite().contains(nuovoStato);
    }
}