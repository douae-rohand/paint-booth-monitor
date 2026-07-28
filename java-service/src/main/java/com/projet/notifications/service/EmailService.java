package com.projet.notifications.service;

public interface EmailService {
    void envoyerLienActivation(String email, String lienActivation);
}
