package com.cyberdefense.assetmanager.repository;

import com.cyberdefense.assetmanager.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // tutte le notifiche di un utente, ordinate dalla più recente
    List<Notification> findByDestinatarioIdOrderByDataCreazioneDesc(Long userId);

    // solo le notifiche non lette, per mostrare il badge nella UI
    List<Notification> findByDestinatarioIdAndLettaFalse(Long userId);
}