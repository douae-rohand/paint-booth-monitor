package com.projet.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Filter to protect against CSRF attacks by validating the Origin/Referer headers 
 * for all mutating HTTP requests (POST, PUT, PATCH, DELETE).
 * Relies on SameSite=Lax cookies to prevent most attacks, block cross-origin requests.
 */
@Component
public class CsrfOriginFilter extends OncePerRequestFilter {

    private final List<String> allowedOrigins;
    
    // Explicit list of allowed origins, defaulting to the same values as CorsConfig
    public CsrfOriginFilter(@Value("${app.cors.allowed-origins:http://localhost:5173,http://localhost:3000}") String[] allowedOriginsArray) {
        this.allowedOrigins = Arrays.asList(allowedOriginsArray);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String method = request.getMethod();

        // Les requêtes OPTIONS (preflight CORS) ne doivent JAMAIS être bloquées par ce filtre.
        if ("OPTIONS".equalsIgnoreCase(method)) {
            filterChain.doFilter(request, response);
            return;
        }

        // CSRF checks only matter for state-changing operations
        Set<String> mutatingMethods = Set.of("POST", "PUT", "PATCH", "DELETE");

        if (mutatingMethods.contains(method)) {
            String origin = request.getHeader("Origin");

            if (origin == null || origin.isEmpty()) {
                String referer = request.getHeader("Referer");
                if (referer != null && !referer.isEmpty()) {
                    try {
                        URI uri = new URI(referer);
                        origin = uri.getScheme() + "://" + uri.getAuthority();
                    } catch (Exception ignored) {
                        origin = null;
                    }
                }
            }

            if (origin == null || origin.isEmpty() || !allowedOrigins.contains(origin)) {
                String requestOrigin = request.getHeader("Origin");
                if (requestOrigin != null && allowedOrigins.contains(requestOrigin)) {
                    response.setHeader("Access-Control-Allow-Origin", requestOrigin);
                    response.setHeader("Access-Control-Allow-Credentials", "true");
                }
                response.setStatus(HttpStatus.FORBIDDEN.value());
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");
                response.setHeader("Cache-Control", "no-store, no-cache");
                response.setHeader("Pragma", "no-cache");
                String jsonResponse = String.format(
                        "{\"code\":403,\"message\":\"Requete bloquee par protection CSRF (Origin invalide ou manquant : '%s')\",\"timestamp\":\"%s\"}",
                        origin, LocalDateTime.now().toString()
                );
                response.getWriter().write(jsonResponse);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}
