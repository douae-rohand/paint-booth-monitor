package com.projet.auth.service;

import com.projet.auth.model.Superviseur;
import com.projet.auth.repository.SuperviseurRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AuthService implements UserDetailsService {

    private final SuperviseurRepository superviseurRepository;
    private final JwtUtil jwtUtil;

    public AuthService(SuperviseurRepository superviseurRepository, JwtUtil jwtUtil) {
        this.superviseurRepository = superviseurRepository;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return superviseurRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("Superviseur not found with email: " + username));
    }

    public String generateToken(Superviseur superviseur) {
        return jwtUtil.generateToken(superviseur, superviseur.getRole());
    }

    public Superviseur findByUsername(String username) {
        return superviseurRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("Superviseur not found with email: " + username));
    }
}
