package com.cyberdefense.assetmanager.service;

import com.cyberdefense.assetmanager.entity.TicketVulnerabilita;
import com.cyberdefense.assetmanager.repository.TicketVulnerabilitaRepository;
import org.springframework.stereotype.Service;

import java.util.List;
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
}