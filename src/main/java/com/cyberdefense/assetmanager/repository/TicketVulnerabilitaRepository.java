package com.cyberdefense.assetmanager.repository;

import com.cyberdefense.assetmanager.entity.TicketVulnerabilita;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketVulnerabilitaRepository extends JpaRepository<TicketVulnerabilita, Long> {
    
    List<TicketVulnerabilita> findByAssetId(Long assetId);
}