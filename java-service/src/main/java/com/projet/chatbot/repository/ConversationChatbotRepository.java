package com.projet.chatbot.repository;

import com.projet.auth.model.Superviseur;
import com.projet.chatbot.model.ConversationChatbot;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository pour l'entité ConversationChatbot.
 * Module: chatbot
 */
@Repository
public interface ConversationChatbotRepository extends JpaRepository<ConversationChatbot, UUID> {

    /**
     * Récupère les derniers échanges d'un superviseur, triés par date décroissante.
     * Le service se chargera d'inverser la liste pour obtenir l'ordre chronologique ASC.
     *
     * @param superviseur Superviseur demandeur
     * @param pageable    Pagination pour limiter au nombre souhaité (ex. 10)
     * @return Liste des échanges récents
     */
    @Query("""
        SELECT c FROM ConversationChatbot c
        WHERE c.superviseur = :superviseur
        ORDER BY c.dateEchange DESC
        """)
    List<ConversationChatbot> findRecentBySuperviseur(@Param("superviseur") Superviseur superviseur, Pageable pageable);
}
