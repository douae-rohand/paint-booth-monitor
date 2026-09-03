package com.projet.auth.controller;

import com.projet.audit.model.enums.ActionAudit;
import com.projet.audit.service.LogAuditService;
import com.projet.auth.dto.AuthResponse;
import com.projet.auth.dto.ChangePasswordRequest;
import com.projet.auth.dto.LoginRequest;
import com.projet.auth.model.Superviseur;
import com.projet.auth.repository.SuperviseurRepository;
import com.projet.auth.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthenticationManager authenticationManager;
    private final AuthService authService;
    private final LogAuditService logAuditService;
    private final SuperviseurRepository superviseurRepository;

    @Value("${app.security.secure-cookie:false}")
    private boolean secureCookie;

    public AuthController(AuthenticationManager authenticationManager,
                          AuthService authService,
                          LogAuditService logAuditService,
                          SuperviseurRepository superviseurRepository) {
        this.authenticationManager = authenticationManager;
        this.authService = authService;
        this.logAuditService = logAuditService;
        this.superviseurRepository = superviseurRepository;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @RequestBody LoginRequest request,
            @RequestHeader(value = "User-Agent", required = false) String userAgent,
            HttpServletResponse response) {
        System.out.println("Login request received for username: " + request.getUsername());

        Authentication auth;
        try {
            auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
        } catch (AuthenticationException ex) {
            // Loguer TENTATIVE_CONNEXION_ECHOUEE uniquement si l'email existe en DB
            // (id_superviseur est NOT NULL dans log_audit)
            try {
                superviseurRepository.findByEmail(request.getUsername()).ifPresent(superviseur ->
                        logAuditService.logger(superviseur.getIdSuperviseur(), ActionAudit.TENTATIVE_CONNEXION_ECHOUEE)
                );
            } catch (Exception auditEx) {
                log.error("[AUDIT] Erreur lors du log de TENTATIVE_CONNEXION_ECHOUEE pour {} : {}",
                        request.getUsername(), auditEx.getMessage(), auditEx);
            }
            throw ex;
        }

        Superviseur user = (Superviseur) auth.getPrincipal();

        // Initialize lazy-loaded admin association
        if (user.getAdmin() != null) {
            user.getAdmin().getIdAdmin(); // Touch the proxy to load it
        }

        System.out.println("Authenticated user: " + user.getEmail() + ", has admin: " + (user.getAdmin() != null)
                + ", role: " + user.getRole() + ", isActif: " + user.isActif());

        String token = authService.generateToken(user);

        // Access token (JWT) cookie - HttpOnly, 1 day duration
        ResponseCookie jwtCookie = ResponseCookie.from("jwt", token)
                .httpOnly(true)
                .secure(secureCookie)
                .path("/")
                .maxAge(24 * 60 * 60)
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, jwtCookie.toString());

        // Refresh token - HttpOnly, 7 days, path restricted to /api/auth/* to minimize
        // exposure
        String rawRefreshToken = authService.createRefreshToken(user, userAgent != null ? userAgent : "Unknown");
        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", rawRefreshToken)
                .httpOnly(true)
                .secure(secureCookie)
                .path("/api/auth") // restricted: browser sends it only to /api/auth/refresh and /api/auth/logout
                .maxAge(7 * 24 * 60 * 60)
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

        // Audit: connexion réussie
        logAuditService.logger(user.getIdSuperviseur(), ActionAudit.CONNEXION);

        return ResponseEntity
                .ok(new AuthResponse(null, user.getUsername(), user.getRole(), user.isMustChangePassword()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(
            @CookieValue(name = "refreshToken", required = false) String refreshToken,
            @RequestHeader(value = "User-Agent", required = false) String userAgent,
            HttpServletResponse response) {

        if (refreshToken == null || refreshToken.isEmpty()) {
            // Missing cookie → 401 via GlobalExceptionHandler pattern
            throw new com.projet.auth.exception.InvalidTokenException(
                    "Cookie refreshToken absent — veuillez vous reconnecter");
        }

        // Rotation atomique (@Transactional dans AuthService) :
        // revoque l'ancien RT → insère un nouveau RT hashé → génère un nouveau JWT
        // InvalidTokenException (invalide / révoqué / expiré) remonte au
        // GlobalExceptionHandler → 401
        AuthService.RotationResult result = authService.rotateRefreshToken(
                refreshToken, userAgent != null ? userAgent : "Unknown");

        // New access token cookie (15 min)
        ResponseCookie jwtCookie = ResponseCookie.from("jwt", result.newAccessToken())
                .httpOnly(true)
                .secure(secureCookie)
                .path("/")
                .maxAge(15 * 60)
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, jwtCookie.toString());

        // New refresh token cookie (7 days, path restreint /api/auth)
        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", result.newRefreshToken())
                .httpOnly(true)
                .secure(secureCookie)
                .path("/api/auth")
                .maxAge(7 * 24 * 60 * 60)
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

        return ResponseEntity.ok().build();
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(
            @CookieValue(name = "refreshToken", required = false) String refreshToken,
            HttpServletResponse response) {

        // Récupérer l'utilisateur courant avant de révoquer (le SecurityContext est encore actif)
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof Superviseur superviseur) {
            logAuditService.logger(superviseur.getIdSuperviseur(), ActionAudit.DECONNEXION);
        }

        if (refreshToken != null && !refreshToken.isEmpty()) {
            authService.revokeRefreshToken(refreshToken);
        }

        // Delete cookies
        ResponseCookie jwtCookieClear = ResponseCookie.from("jwt", "")
                .httpOnly(true)
                .secure(secureCookie)
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, jwtCookieClear.toString());

        ResponseCookie refreshCookieClear = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(secureCookie)
                .path("/api/auth")
                .maxAge(0)
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookieClear.toString());

        return ResponseEntity.ok().build();
    }

    @GetMapping("/me")
    public ResponseEntity<AuthResponse> me(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Superviseur user = (Superviseur) authentication.getPrincipal();
        return ResponseEntity
                .ok(new AuthResponse(null, user.getUsername(), user.getRole(), user.isMustChangePassword()));
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody ChangePasswordRequest request, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Superviseur user = (Superviseur) authentication.getPrincipal();
        authService.changePassword(user, request.getOldPassword(), request.getNewPassword());
        return ResponseEntity.ok().body("Mot de passe mis à jour avec succès");
    }
}
