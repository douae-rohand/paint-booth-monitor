package com.projet.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.Map;

/**
 * ChannelInterceptor STOMP qui injecte un Principal dans chaque message CONNECT
 * à partir du userId (UUID string) stocké dans les attributs de session WebSocket
 * par {@link WebSocketHandshakeInterceptor}.
 *
 * Sans ce Principal, {@link org.springframework.messaging.simp.SimpMessagingTemplate#convertAndSendToUser}
 * ne peut pas router les messages vers /user/queue/notifications — le userId serait null
 * et le routage silencieusement ignoré.
 *
 * Le Principal.getName() retourne l'UUID string du Superviseur, cohérent avec
 * l'appel convertAndSendToUser(superviseur.getIdSuperviseur().toString(), ...).
 */
@Component
public class StompPrincipalChannelInterceptor implements ChannelInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(StompPrincipalChannelInterceptor.class);

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null) return message;

        // Injecter le Principal uniquement sur CONNECT (une seule fois par session)
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
            if (sessionAttributes != null) {
                Object userId = sessionAttributes.get("userId");
                if (userId instanceof String userIdStr && !userIdStr.isBlank()) {
                    accessor.setUser(new StompPrincipal(userIdStr));
                    logger.debug("[STOMP] Principal injecté pour userId={}", userIdStr);
                } else {
                    logger.warn("[STOMP] CONNECT sans userId dans les attributs de session");
                }
            }
        }

        return message;
    }

    /**
     * Principal minimal pour STOMP — porte uniquement le UUID du Superviseur.
     * Immuable, thread-safe.
     */
    private record StompPrincipal(String name) implements Principal {
        @Override
        public String getName() { return name; }
    }
}
