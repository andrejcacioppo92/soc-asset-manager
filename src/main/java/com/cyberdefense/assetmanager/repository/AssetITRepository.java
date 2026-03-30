package com.cyberdefense.assetmanager.repository;

import com.cyberdefense.assetmanager.entity.AssetIT;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AssetITRepository extends JpaRepository<AssetIT, Long> {

}