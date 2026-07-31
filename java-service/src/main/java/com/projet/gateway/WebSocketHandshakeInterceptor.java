package com.projet.gateway;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

/**
 * Interceptor pour valider l'authentification JWT lors du handshake WebSocket.
 * 
 * Le JWT est transmis via un cookie httpOnly (géré par le navigateur).
 * L'intercepteur valide le token avant d'autoriser la connexion.
 */
@Component
public class WebSocketHandshakeInterceptor implements HandshakeInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketHandshakeInterceptor.class);
    private static final String COOKIE_NAME = "jwt";

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes) throws Exception {

        if (request instanceof ServletServerHttpRequest) {
            HttpServletRequest servletRequest = ((ServletServerHttpRequest) request).getServletRequest();
            String uri = servletRequest.getRequestURI();
            
            // Autoriser les requêtes SockJS de négociation sans authentification
            // Ces requêtes sont utilisées par SockJS pour établir la connexion
            if (uri != null && (uri.contains("/info") || uri.contains("/iframe") || uri.contains("/chunking"))) {
                logger.debug("Autorisation requête SockJS de négociation: {}", uri);
                return true;
            }
            
            // Récupérer le cookie JWT
            String token = extractTokenFromCookie(servletRequest);
            
            if (token == null) {
                logger.warn("WebSocket handshake rejeté : cookie JWT non trouvé");
                return false;
            }

            // Valider le token
            try {
                Claims claims = validateToken(token);
                attributes.put("userId", claims.getSubject());
                attributes.put("role", claims.get("role"));
                logger.info("WebSocket handshake autorisé pour utilisateur: {}", claims.getSubject());
                return true;
            } catch (Exception e) {
                logger.warn("WebSocket handshake rejeté : token JWT invalide - {}", e.getMessage());
                return false;
            }
        }

        logger.warn("WebSocket handshake rejeté : requête non ServletServerHttpRequest");
        return false;
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception) {
        // Rien à faire après le handshake
    }

    /**
     * Extrait le token JWT depuis le cookie httpOnly.
     */
    private String extractTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (COOKIE_NAME.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    /**
     * Valide le token JWT et retourne les claims.
     */
    private Claims validateToken(String token) {
        byte[] keyBytes = Base64.getDecoder().decode(jwtSecret.getBytes(StandardCharsets.UTF_8));
        return Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(keyBytes))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
