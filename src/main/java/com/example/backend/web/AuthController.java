package com.example.backend.web;

import com.example.backend.dto.*;
import com.example.backend.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    @Value("${app.frontendBaseUrl}")
    private String frontendBaseUrl;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // ─── REGISTER ────────────────────────────────────────────────────────────

    @PostMapping("/register")
    public ResponseEntity<ApiMessage> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.ok(new ApiMessage(
                "Registration successful. Please check your email to verify your account."));
    }

    // ─── LOGIN ───────────────────────────────────────────────────────────────

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    // ─── EMAIL VERIFICATION ───────────────────────────────────────────────────

    /**
     * Browser hits this URL from the verification email.
     *
     * Redirect outcomes:
     *   /login?verified=true              → success (or already-used idempotent click)
     *   /login?verifyError=expired&email= → token expired, Angular shows resend form pre-filled
     *   /login?verifyError=invalid        → token not found
     */
    @GetMapping("/verify")
    public RedirectView verify(@RequestParam String token) {
        try {
            authService.verifyEmail(token);
            return new RedirectView(frontendBaseUrl + "/login?verified=true");

        } catch (IllegalStateException e) {
            // "Token already used" — idempotent double-click, treat as success
            return new RedirectView(frontendBaseUrl + "/login?verified=true");

        } catch (IllegalArgumentException e) {
            String msg = e.getMessage() != null ? e.getMessage() : "";

            if (msg.contains("expired")) {
                // Pass email back so Angular pre-fills the resend form
                try {
                    String userEmail = authService.getEmailForToken(token);
                    String encoded   = URLEncoder.encode(userEmail, StandardCharsets.UTF_8);
                    return new RedirectView(
                            frontendBaseUrl + "/login?verifyError=expired&email=" + encoded);
                } catch (Exception ignored) {
                    return new RedirectView(frontendBaseUrl + "/login?verifyError=expired");
                }
            }

            // Token not found in DB
            return new RedirectView(frontendBaseUrl + "/login?verifyError=invalid");
        }
    }

    // ─── RESEND VERIFICATION ─────────────────────────────────────────────────

    @PostMapping("/resend-verification")
    public ResponseEntity<ApiMessage> resendVerification(
            @Valid @RequestBody ResendVerificationRequest request) {
        authService.resendVerification(request.getEmail());
        return ResponseEntity.ok(new ApiMessage("Verification email sent. Please check your inbox."));
    }

    // ─── FORGOT PASSWORD ──────────────────────────────────────────────────────

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiMessage> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest req) {
        authService.forgotPassword(req.getEmail());
        return ResponseEntity.ok(new ApiMessage(
                "If that email is registered, a reset link has been sent."));
    }

    // ─── RESET PASSWORD (email link — GET) ───────────────────────────────────

    @GetMapping("/reset-password")
    public RedirectView resetViaEmailLink(@RequestParam String token) {
        return new RedirectView(frontendBaseUrl + "/reset-password?token=" + token);
    }

    // ─── RESET PASSWORD (Angular form — POST) ────────────────────────────────

    @PostMapping("/reset-password")
    public ResponseEntity<ApiMessage> resetPassword(
            @Valid @RequestBody ResetPasswordRequest req) {
        authService.resetPassword(req.getToken(), req.getNewPassword());
        return ResponseEntity.ok(new ApiMessage("Password reset successful. You can now log in."));
    }

    // ─── TOKEN REFRESH ────────────────────────────────────────────────────────

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest req) {
        return ResponseEntity.ok(authService.refresh(req.getRefreshToken()));
    }

    // ─── LOGOUT ──────────────────────────────────────────────────────────────

    @PostMapping("/logout")
    public ResponseEntity<ApiMessage> logout(@Valid @RequestBody RefreshRequest req) {
        authService.logout(req.getRefreshToken());
        return ResponseEntity.ok(new ApiMessage("Logged out successfully."));
    }
}