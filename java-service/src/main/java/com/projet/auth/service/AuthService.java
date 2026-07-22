package com.projet.auth.service;

import com.projet.auth.exception.InvalidTokenException;
import com.projet.auth.model.RefreshToken;
import com.projet.auth.model.Superviseur;
import com.projet.auth.repository.RefreshTokenRepository;
import com.projet.auth.repository.SuperviseurRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService implements UserDetailsService {

    private final SuperviseurRepository superviseurRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    public AuthService(SuperviseurRepository superviseurRepository, 
                       RefreshTokenRepository refreshTokenRepository, 
                       JwtUtil jwtUtil,
                       PasswordEncoder passwordEncoder) {
        this.superviseurRepository = superviseurRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return superviseurRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("Superviseur not found with email: " + username));
    }

    public String generateToken(Superviseur superviseur) {
        // Initialize admin association if present to get correct role
        if (superviseur.getAdmin() != null) {
            superviseur.getAdmin().getIdAdmin();
        }
        return jwtUtil.generateToken(superviseur, superviseur.getRole());
    }

    /**
     * Immutable result of a refresh token rotation.
     *
     * @param newAccessToken  fresh JWT to set in the jwt cookie
     * @param newRefreshToken raw (unhashed) value to set in the refreshToken cookie
     */
    public record RotationResult(String newAccessToken, String newRefreshToken) {}

    public Superviseur findByUsername(String username) {
        return superviseurRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("Superviseur not found with email: " + username));
    }

    @Transactional
    public String createRefreshToken(Superviseur superviseur, String userAgent) {
        // Find and revoke existing active tokens for this user and user-agent/device
        List<RefreshToken> activeTokens = refreshTokenRepository
                .findBySuperviseurAndUserAgentAndRevoqueFalse(superviseur, userAgent);
        for (RefreshToken t : activeTokens) {
            t.setRevoque(true);
        }
        refreshTokenRepository.saveAll(activeTokens);

        // Generate raw token
        String rawToken = UUID.randomUUID().toString().replace("-", "") + 
                           UUID.randomUUID().toString().replace("-", "");
        String hash = hashToken(rawToken);

        // Create new RefreshToken in database (expires in 7 days)
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setSuperviseur(superviseur);
        refreshToken.setTokenHash(hash);
        refreshToken.setUserAgent(userAgent);
        refreshToken.setRevoque(false);
        refreshToken.setDateExpiration(LocalDateTime.now().plusDays(7));
        refreshTokenRepository.save(refreshToken);

        return rawToken;
    }

    /**
     * Performs a full refresh token rotation — all DB writes in a single transaction.
     *
     * Steps:
     *   1. Hash the raw incoming token and look it up in DB.
     *   2. Reject immediately if revoked or expired (throws InvalidTokenException → 401).
     *   3. Mark the old token revoque = true.
     *   4. Generate + hash a new refresh token and insert it.
     *   5. Generate a new JWT access token.
     *   6. Return both raw values so the controller can set the cookies.
     */
    @Transactional
    public RotationResult rotateRefreshToken(String rawRefreshToken, String userAgent) {
        String incomingHash = hashToken(rawRefreshToken);

        RefreshToken oldToken = refreshTokenRepository.findByTokenHash(incomingHash)
                .orElseThrow(() -> new InvalidTokenException("Refresh token invalide"));

        if (oldToken.isRevoque()) {
            throw new InvalidTokenException("Refresh token révoqué — veuillez vous reconnecter");
        }
        if (oldToken.getDateExpiration().isBefore(LocalDateTime.now())) {
            throw new InvalidTokenException("Refresh token expiré — veuillez vous reconnecter");
        }

        // Revoke old token
        oldToken.setRevoque(true);
        refreshTokenRepository.save(oldToken);

        // Generate and store new refresh token
        Superviseur superviseur = oldToken.getSuperviseur();
        String newRawRefreshToken = UUID.randomUUID().toString().replace("-", "")
                + UUID.randomUUID().toString().replace("-", "");
        String newHash = hashToken(newRawRefreshToken);

        RefreshToken newToken = new RefreshToken();
        newToken.setSuperviseur(superviseur);
        newToken.setTokenHash(newHash);
        newToken.setUserAgent(userAgent != null ? userAgent : "Unknown");
        newToken.setRevoque(false);
        newToken.setDateExpiration(LocalDateTime.now().plusDays(7));
        refreshTokenRepository.save(newToken);

        // Generate new access token
        String newAccessToken = generateToken(superviseur);

        return new RotationResult(newAccessToken, newRawRefreshToken);
    }

    @Transactional
    public void revokeRefreshToken(String rawRefreshToken) {
        String hash = hashToken(rawRefreshToken);
        Optional<RefreshToken> tokenEntityOpt = refreshTokenRepository.findByTokenHash(hash);
        if (tokenEntityOpt.isPresent()) {
            RefreshToken tokenEntity = tokenEntityOpt.get();
            tokenEntity.setRevoque(true);
            refreshTokenRepository.save(tokenEntity);
        }
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error hashing refresh token", e);
        }
    }

    @Transactional
    public void changePassword(Superviseur superviseur, String oldPassword, String newPassword) {
        // Verify old password
        if (!passwordEncoder.matches(oldPassword, superviseur.getMotDePasseHash())) {
            throw new IllegalArgumentException("Ancien mot de passe incorrect");
        }

        // Validate new password (at least 8 characters)
        if (newPassword.length() < 8) {
            throw new IllegalArgumentException("Le nouveau mot de passe doit contenir au moins 8 caractères");
        }

        // Update password and reset mustChangePassword
        superviseur.setMotDePasseHash(passwordEncoder.encode(newPassword));
        superviseur.setMustChangePassword(false);
        superviseur.setUpdatedAt(LocalDateTime.now());
        superviseurRepository.save(superviseur);
    }
}
