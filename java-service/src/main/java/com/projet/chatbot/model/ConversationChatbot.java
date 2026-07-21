package com.projet.chatbot.model;

import com.projet.auth.model.Superviseur;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Module: chatbot
 * SQL Table: conversation_chatbot
 * 
 * Stores conversations with the AI chatbot. 
 * 
 * Note: The response is produced by the Python service (RAG) and transmitted 
 * to this field by the Java service after receiving it.
 */
@Entity
@Table(name = "conversation_chatbot")
public class ConversationChatbot {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id_conversation")
    private UUID idConversation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_superviseur", nullable = false)
    private Superviseur superviseur;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String question;

    @Column(columnDefinition = "TEXT")
    private String reponse;

    @Column(name = "date_echange", nullable = false, updatable = false)
    private LocalDateTime dateEchange = LocalDateTime.now();

    // ── Constructors ──────────────────────────────────────────────────────────

    public ConversationChatbot() {}

    // ── Getters / Setters ─────────────────────────────────────────────────────

    public UUID getIdConversation() { return idConversation; }
    public void setIdConversation(UUID idConversation) { this.idConversation = idConversation; }

    public Superviseur getSuperviseur() { return superviseur; }
    public void setSuperviseur(Superviseur superviseur) { this.superviseur = superviseur; }

    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }

    public String getReponse() { return reponse; }
    public void setReponse(String reponse) { this.reponse = reponse; }

    public LocalDateTime getDateEchange() { return dateEchange; }
    public void setDateEchange(LocalDateTime dateEchange) { this.dateEchange = dateEchange; }
}
