package com.projet.notifications.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO pour la réponse de création/mise à jour d'un abonnement Web Push.
 * Module: notifications
 */
public class PushSubscriptionResponseDTO {

    private UUID id;
    private String endpoint;
    private LocalDateTime dateCreation;

    public PushSubscriptionResponseDTO(UUID id, String endpoint, LocalDateTime dateCreation) {
        this.id = id;
        this.endpoint = endpoint;
        this.dateCreation = dateCreation;
    }

    // Getters et Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public LocalDateTime getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(LocalDateTime dateCreation) {
        this.dateCreation = dateCreation;
    }
}
