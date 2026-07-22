package com.projet.auth.dto;

public class AuthResponse {
    private String token;
    private String username;
    private String role;
    private boolean mustChangePassword;

    public AuthResponse(String token, String username, String role, boolean mustChangePassword) {
        this.token = token;
        this.username = username;
        this.role = role;
        this.mustChangePassword = mustChangePassword;
    }

    public String getToken() { return token; }
    public String getUsername() { return username; }
    public String getRole() { return role; }
    public boolean isMustChangePassword() { return mustChangePassword; }
}
