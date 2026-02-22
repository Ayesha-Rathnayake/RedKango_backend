package com.example.backend.dto;

import com.example.backend.validation.StrongPassword;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ResetPasswordRequest {
    @NotBlank
    private String token;

    @StrongPassword
    @NotBlank
    private String newPassword;
}