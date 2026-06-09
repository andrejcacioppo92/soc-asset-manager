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

    // accetta sia Server che Firewall grazie al polimorfismo dell'entità astratta
    public AssetIT salva(AssetIT asset) {
        return assetRepository.save(asset);
    }

    public void elimina(Long id) {
        assetRepository.deleteById(id);
    }

    public boolean esiste(Long id) {
        return assetRepository.existsById(id);
    }

    // controllo se un IP è già usato, mi serve nei controller prima di salvare
    // così evito di duplicare lo stesso asset in inventario
    public boolean ipGiaUsato(String indirizzoIp) {
        return assetRepository.existsByIndirizzoIp(indirizzoIp);
    }
}