package com.cyberdefense.assetmanager.repository;

import com.cyberdefense.assetmanager.entity.AssetIT;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AssetITRepository extends JpaRepository<AssetIT, Long> {

    // mi serve per controllare se un IP è già registrato prima di salvare un nuovo asset
    // così evito di affidarmi solo all'errore del DB e restituisco un 400 pulito
    boolean existsByIndirizzoIp(String indirizzoIp);
}