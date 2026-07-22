package com.projet.auth.service;

import com.projet.auth.model.Superviseur;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;

@Component
public class MustChangePasswordFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication != null && authentication.isAuthenticated() 
                && authentication.getPrincipal() instanceof Superviseur) {
            
            Superviseur superviseur = (Superviseur) authentication.getPrincipal();
            String requestUri = request.getRequestURI();
            
            // List of allowed paths even when password change is required
            boolean isAllowedPath = requestUri.startsWith("/api/auth/change-password")
                    || requestUri.startsWith("/api/auth/logout")
                    || requestUri.startsWith("/api/auth/refresh")
                    || requestUri.startsWith("/api/auth/me");
            
            if (superviseur.isMustChangePassword() && !isAllowedPath) {
                // Return 403 with PASSWORD_CHANGE_REQUIRED code, write JSON manually
                String jsonResponse = String.format(
                    "{\"code\":403,\"message\":\"PASSWORD_CHANGE_REQUIRED\",\"timestamp\":\"%s\"}",
                    LocalDateTime.now().toString()
                );
                response.setStatus(HttpStatus.FORBIDDEN.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.getWriter().write(jsonResponse);
                return;
            }
        }
        
        filterChain.doFilter(request, response);
    }
}
