package com.example.backend.dto;

public record AdminProfileResponse(
        Long id,
        String firstName,
        String lastName,
        String fullName,
        String email,
        String phone,
        String role,
        String profileImageUrl
) {}