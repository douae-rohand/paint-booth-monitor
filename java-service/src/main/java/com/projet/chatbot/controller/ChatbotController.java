package com.projet.chatbot.controller;

import com.projet.auth.model.Superviseur;
import com.projet.chatbot.dto.ChatbotQueryRequest;
import com.projet.chatbot.dto.ChatbotQueryResponse;
import com.projet.chatbot.service.ChatbotService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Controller pour l'assistant conversationnel (Chatbot).
 * Module: chatbot
 * Endpoint: POST /api/chatbot/query
 */
@RestController
@RequestMapping("/api/chatbot")
@RequiredArgsConstructor
public class ChatbotController {

    private final ChatbotService chatbotService;

    /**
     * Traite une question posée au chatbot.
     * Accessible à tout utilisateur authentifié (Superviseur ou Admin).
     *
     * @param request DTO contenant le message de l'utilisateur
     * @param superviseur Superviseur authentifié via JWT
     * @return ChatbotQueryResponse contenant la réponse générée
     */
    @PostMapping("/query")
    public ResponseEntity<ChatbotQueryResponse> query(
            @RequestBody ChatbotQueryRequest request,
            @AuthenticationPrincipal Superviseur superviseur) {

        String reponse = chatbotService.traiterQuestion(superviseur, request.message());
        return ResponseEntity.ok(new ChatbotQueryResponse(reponse));
    }
}
