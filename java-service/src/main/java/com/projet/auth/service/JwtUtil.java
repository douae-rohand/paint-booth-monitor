package com.projet.auth.service;

import com.projet.auth.model.Superviseur;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * JWT Utility — access token generation and validation.
 *
 * Payload (signed, NOT encrypted — never put sensitive data here):
 *   sub  : id_superviseur (UUID) — never email or password
 *   role : ROLE_SUPERVISEUR | ROLE_ADMIN
 *   iat  : issued-at (auto-set by JJWT)
 *   exp  : expiration timestamp
 *
 * Algorithm : HS256
 * Secret    : ${jwt.secret} (env variable, never hardcoded)
 * Lifetime  : ${jwt.expiration} ms — default 15 min (900000 ms)
 */
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    /** Lifetime in milliseconds. Default = 900000 = 15 minutes. */
    @Value("${jwt.expiration:900000}")
    private long expiration;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Generates a signed JWT access token.
     *
     * @param superviseur the authenticated supervisor — only UUID and role go into the payload
     * @param role        ROLE_SUPERVISEUR or ROLE_ADMIN
     */
    public String generateToken(Superviseur superviseur, String role) {
        Map<String, Object> claims = new HashMap<>();
        // Only non-sensitive identifiers go into the payload
        claims.put("role", role);
        return Jwts.builder()
                .claims(claims)
                .subject(superviseur.getIdSuperviseur().toString())  // sub = UUID, never email
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())  // HS256
                .compact();
    }

    /** Extracts the subject claim (id_superviseur UUID string). */
    public String extractSubject(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /** Extracts the role claim. */
    public String extractRole(String token) {
        return extractClaim(token, c -> c.get("role", String.class));
    }

    /**
     * Validates the token against the superviseur's UUID.
     *
     * @param token       the JWT string
     * @param superviseur the superviseur entity to validate against
     */
    public boolean isTokenValid(String token, Superviseur superviseur) {
        final String subject = extractSubject(token);
        return subject.equals(superviseur.getIdSuperviseur().toString()) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claimsResolver.apply(claims);
    }
}

