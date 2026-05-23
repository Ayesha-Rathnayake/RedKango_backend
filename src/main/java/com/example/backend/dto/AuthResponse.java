package com.example.backend.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Returned on successful login.
 * The "roles" list lets Angular decide which portal to open:
 *   - contains "ROLE_ADMIN"    → redirect to /admin/dashboard
 *   - contains "ROLE_CUSTOMER" → redirect to /home
 */
@Getter
@Setter
public class AuthResponse {

    private String accessToken;
    private String refreshToken;
    private String email;
    private String fullName;

    /** e.g. ["ROLE_CUSTOMER"] or ["ROLE_ADMIN"] */
    private List<String> roles;
}