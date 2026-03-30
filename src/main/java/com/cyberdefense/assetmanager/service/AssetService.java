package com.cyberdefense.assetmanager.service;

import com.cyberdefense.assetmanager.entity.AssetIT;
import com.cyberdefense.assetmanager.repository.AssetITRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AssetService {

    private final AssetITRepository assetRepository;

    public AssetService(AssetITRepository assetRepository) {
        this.assetRepository = assetRepository;
    }

    public List<AssetIT> trovatutti() {
        return assetRepository.findAll();
    }

    public Optional<AssetIT> trovaPerId(Long id) {
        return assetRepository.findById(id);
    }

    // Accetta sia Server che Firewall grazie al polimorfismo sull'entità astratta
    public AssetIT salva(AssetIT asset) {
        return assetRepository.save(asset);
    }

    public void elimina(Long id) {
        assetRepository.deleteById(id);
    }

    public boolean esiste(Long id) {
        return assetRepository.existsById(id);
    }
}