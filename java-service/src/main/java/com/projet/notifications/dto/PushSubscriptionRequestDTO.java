package com.projet.notifications.dto;

/**
 * DTO pour la création/mise à jour d'un abonnement Web Push.
 * Module: notifications
 */
public class PushSubscriptionRequestDTO {

    private String endpoint;
    private String cleP256dh;
    private String cleAuth;
    private String userAgent;

    // Getters et Setters
    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getCleP256dh() {
        return cleP256dh;
    }

    public void setCleP256dh(String cleP256dh) {
        this.cleP256dh = cleP256dh;
    }

    public String getCleAuth() {
        return cleAuth;
    }

    public void setCleAuth(String cleAuth) {
        this.cleAuth = cleAuth;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }
}
