package com.cyberdefense.assetmanager.service;

import com.cyberdefense.assetmanager.entity.Gravita;
import com.cyberdefense.assetmanager.entity.StatoTicket;
import com.cyberdefense.assetmanager.entity.TicketVulnerabilita;
import com.cyberdefense.assetmanager.repository.TicketVulnerabilitaRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class TicketService {

    private final TicketVulnerabilitaRepository ticketRepository;

    public TicketService(TicketVulnerabilitaRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    public List<TicketVulnerabilita> trovaTutti() {
        return ticketRepository.findAll();
    }

    public Optional<TicketVulnerabilita> trovaPerId(Long id) {
        return ticketRepository.findById(id);
    }

    public List<TicketVulnerabilita> trovaPerAssetId(Long assetId) {
        return ticketRepository.findByAssetId(assetId);
    }

    public TicketVulnerabilita salva(TicketVulnerabilita ticket) {
        return ticketRepository.save(ticket);
    }

    public void elimina(Long id) {
        ticketRepository.deleteById(id);
    }

    public boolean esiste(Long id) {
        return ticketRepository.existsById(id);
    }

    // ricerca con filtri combinati, gravità e stato sono entrambi opzionali
    // il sorting viene passato come parametro così posso decidere lato controller
    public List<TicketVulnerabilita> filtra(Gravita gravita, StatoTicket stato, Sort sort) {
        return ticketRepository.filtra(gravita, stato, sort);
    }

    // conteggio dei ticket raggruppati per gravità, restituisco una mappa pulita
    // così il front-end può disegnare un grafico senza dover trasformare i dati
    public Map<String, Long> contaPerGravita() {
        Map<String, Long> risultato = new HashMap<>();
        for (Object[] riga : ticketRepository.contaPerGravita()) {
            Gravita g = (Gravita) riga[0];
            Long count = (Long) riga[1];
            risultato.put(g.name(), count);
        }
        return risultato;
    }

    // conteggio dei ticket raggruppati per stato
    public Map<String, Long> contaPerStato() {
        Map<String, Long> risultato = new HashMap<>();
        for (Object[] riga : ticketRepository.contaPerStato()) {
            StatoTicket s = (StatoTicket) riga[0];
            Long count = (Long) riga[1];
            risultato.put(s.name(), count);
        }
        return risultato;
    }
}