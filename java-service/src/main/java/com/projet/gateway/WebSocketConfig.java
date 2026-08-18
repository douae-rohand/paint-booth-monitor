package com.projet.gateway;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.messaging.simp.config.ChannelRegistration;

/**
 * Configuration WebSocket avec STOMP et SockJS fallback.
 *
 * Topics disponibles :
 * - /topic/statut-temps-reel : statut temps réel de tous les points de mesure
 * - /topic/mesures/{idPointMesure}/{metrique} : dernières valeurs pour le graphe en direct
 * - /topic/kpis : recalcul des KPI globaux
 * - /topic/alertes : nouvelles alertes (création + résolution)
 * - /user/queue/notifications : notifications IN_APP personnelles (par utilisateur)
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketHandshakeInterceptor handshakeInterceptor;
    private final StompPrincipalChannelInterceptor stompPrincipalChannelInterceptor;

    public WebSocketConfig(
            WebSocketHandshakeInterceptor handshakeInterceptor,
            StompPrincipalChannelInterceptor stompPrincipalChannelInterceptor) {
        this.handshakeInterceptor = handshakeInterceptor;
        this.stompPrincipalChannelInterceptor = stompPrincipalChannelInterceptor;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // /topic = diffusion globale, /queue = queues personnelles par utilisateur
        config.enableSimpleBroker("/topic", "/queue");
        // Préfixe pour les messages envoyés par le client vers le serveur
        config.setApplicationDestinationPrefixes("/app");
        // Préfixe pour les destinations personnelles (convertAndSendToUser)
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .addInterceptors(handshakeInterceptor)
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // Injecte le Principal STOMP à partir du userId stocké dans les attributs
        // de session WebSocket par WebSocketHandshakeInterceptor.
        // Nécessaire pour que convertAndSendToUser(userId, ...) route correctement.
        registration.interceptors(stompPrincipalChannelInterceptor);
    }
}
