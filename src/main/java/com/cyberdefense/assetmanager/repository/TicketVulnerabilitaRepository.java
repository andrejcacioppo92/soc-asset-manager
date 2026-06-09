package com.cyberdefense.assetmanager.repository;

import com.cyberdefense.assetmanager.entity.Gravita;
import com.cyberdefense.assetmanager.entity.StatoTicket;
import com.cyberdefense.assetmanager.entity.TicketVulnerabilita;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketVulnerabilitaRepository extends JpaRepository<TicketVulnerabilita, Long> {

    List<TicketVulnerabilita> findByAssetId(Long assetId);

    // filtro per gravità o per stato, utile nella dashboard analista
    List<TicketVulnerabilita> findByGravita(Gravita gravita, Sort sort);

    List<TicketVulnerabilita> findByStato(StatoTicket stato, Sort sort);

    // query combinata con due filtri opzionali, gestita con JPQL custom
    // i parametri null vengono ignorati così posso filtrare a piacere
    @Query("SELECT t FROM TicketVulnerabilita t " +
            "WHERE (:gravita IS NULL OR t.gravita = :gravita) " +
            "AND (:stato IS NULL OR t.stato = :stato)")
    List<TicketVulnerabilita> filtra(@Param("gravita") Gravita gravita,
                                     @Param("stato") StatoTicket stato,
                                     Sort sort);

    // aggregazione per contare i ticket per ogni gravità
    // restituisce coppie (gravita, count) che poi il service trasforma in mappa
    @Query("SELECT t.gravita, COUNT(t) FROM TicketVulnerabilita t GROUP BY t.gravita")
    List<Object[]> contaPerGravita();

    // aggregazione per stato, serve per le statistiche operative del SOC
    @Query("SELECT t.stato, COUNT(t) FROM TicketVulnerabilita t GROUP BY t.stato")
    List<Object[]> contaPerStato();
}