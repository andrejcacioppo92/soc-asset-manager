package com.cyberdefense.assetmanager.repository;

import com.cyberdefense.assetmanager.entity.PianoMitigazione;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PianoMitigazioneRepository extends JpaRepository<PianoMitigazione, Long> {

    // tutti i piani relativi a un ticket specifico, ordinati dal più recente
    List<PianoMitigazione> findByTicketIdOrderByDataGenerazioneDesc(Long ticketId);

    // tutti i piani in attesa di revisione, utili per la dashboard del revisore
    List<PianoMitigazione> findByStato(String stato);
}