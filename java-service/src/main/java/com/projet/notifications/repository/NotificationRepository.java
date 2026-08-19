package com.projet.notifications.repository;

import com.projet.notifications.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Repository pour l'entité Notification.
 * Appartient à Java (lecture + écriture). Python n'accède pas à cette table.
 */
public interface NotificationRepository extends JpaRepository<Notification, UUID> {
}
