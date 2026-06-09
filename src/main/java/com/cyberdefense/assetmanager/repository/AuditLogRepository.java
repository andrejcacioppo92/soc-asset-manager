package com.cyberdefense.assetmanager.repository;

import com.cyberdefense.assetmanager.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    // recupero tutti gli eventi di un certo tipo, utile per i report di sicurezza
    List<AuditLog> findByCategoria(String categoria);

    // recupero gli eventi di un operatore specifico, per indagare su un account sospetto
    List<AuditLog> findByOperatoreOrderByTimestampDesc(String operatore);
}