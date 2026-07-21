package com.projet.auth.service;

import com.projet.auth.model.Superviseur;
import com.projet.auth.repository.SuperviseurRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final SuperviseurRepository superviseurRepository;

    public JwtFilter(JwtUtil jwtUtil, SuperviseurRepository superviseurRepository) {
        this.jwtUtil = jwtUtil;
        this.superviseurRepository = superviseurRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // 1. Extract JWT — cookie first, then Authorization header fallback
        String token = null;
        if (request.getCookies() != null) {
            for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
                if ("jwt".equals(cookie.getName())) {
                    token = cookie.getValue();
                    break;
                }
            }
        }
        if (token == null) {
            final String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                token = authHeader.substring(7);
            }
        }

        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        // 2. Extract sub (id_superviseur UUID) — never the email
        final String superviseurId;
        try {
            superviseurId = jwtUtil.extractSubject(token);
        } catch (Exception e) {
            // Invalid or tampered token — continue without authenticating
            filterChain.doFilter(request, response);
            return;
        }

        // 3. Load and authenticate only if not already in context
        if (superviseurId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                UUID uuid = UUID.fromString(superviseurId);
                Optional<Superviseur> superviseurOpt = superviseurRepository.findById(uuid);
                if (superviseurOpt.isPresent()) {
                    Superviseur superviseur = superviseurOpt.get();
                    if (jwtUtil.isTokenValid(token, superviseur)) {
                        String role = jwtUtil.extractRole(token);
                        java.util.Collection<? extends org.springframework.security.core.GrantedAuthority> authorities;
                        if (role != null && !role.isEmpty()) {
                            authorities = java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority(role));
                        } else {
                            authorities = superviseur.getAuthorities();
                        }
                        UsernamePasswordAuthenticationToken authToken =
                                new UsernamePasswordAuthenticationToken(
                                        superviseur, null, authorities);
                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                    }
                }
            } catch (IllegalArgumentException ignored) {
                // sub is not a valid UUID — skip
            }
        }

        filterChain.doFilter(request, response);
    }
}
