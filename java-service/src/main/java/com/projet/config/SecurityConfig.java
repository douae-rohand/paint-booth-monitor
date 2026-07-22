package com.projet.config;

import com.projet.auth.service.JwtFilter;
import com.projet.auth.service.MustChangePasswordFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtFilter jwtFilter;
    private final CsrfOriginFilter csrfOriginFilter;
    private final MustChangePasswordFilter mustChangePasswordFilter;

    public SecurityConfig(JwtFilter jwtFilter, CsrfOriginFilter csrfOriginFilter, MustChangePasswordFilter mustChangePasswordFilter) {
        this.jwtFilter = jwtFilter;
        this.csrfOriginFilter = csrfOriginFilter;
        this.mustChangePasswordFilter = mustChangePasswordFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/login", "/api/auth/register", "/api/auth/refresh", "/api/auth/logout", "/api/auth/me", "/api/auth/change-password").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .addFilterBefore(csrfOriginFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(jwtFilter, CsrfOriginFilter.class)
            .addFilterAfter(mustChangePasswordFilter, JwtFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }


    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
