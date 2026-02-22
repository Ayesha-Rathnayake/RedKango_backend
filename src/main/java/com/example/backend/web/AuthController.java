
package com.example.backend.web;

import com.example.backend.dto.*;
import com.example.backend.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService auth;

    public AuthController(AuthService auth) { this.auth = auth; }

    @PostMapping("/register")
    public ResponseEntity<ApiMessage> register(@Valid @RequestBody RegisterRequest request) {
        auth.register(request);
        return ResponseEntity.ok(new ApiMessage("Registration successful. Please verify your email."));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(auth.login(request));
    }

    /**
     * Email verification link: GET /api/auth/verify?token=xxx
     * When frontendEnabled=false this endpoint is linked directly in the email.
     * After verifying it redirects the browser to the Angular login page
     * instead of returning raw JSON.
     */
    @GetMapping("/verify")
    public RedirectView verify(
            @RequestParam String token,
            @org.springframework.beans.factory.annotation.Value("${app.frontendBaseUrl}") String frontendBaseUrl) {
        try {
            auth.verifyEmail(token);
            // Redirect to Angular login with a success flag Angular can read
            return new RedirectView(frontendBaseUrl + "/login?verified=true");
        } catch (Exception e) {
            return new RedirectView(frontendBaseUrl + "/login?verifyError=true");
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiMessage> forgot(@Valid @RequestBody ForgotPasswordRequest req) {
        auth.forgotPassword(req.getEmail());
        return ResponseEntity.ok(new ApiMessage("Reset link sent to your email"));
    }

    /**
     * POST /api/auth/reset-password — called by Angular form submission
     */
    @PostMapping("/reset-password")
    public ResponseEntity<ApiMessage> reset(@Valid @RequestBody ResetPasswordRequest req) {
        auth.resetPassword(req.getToken(), req.getNewPassword());
        return ResponseEntity.ok(new ApiMessage("Password reset successful"));
    }

    /**
     * GET /api/auth/reset-password?token=xxx
     *
     * This is what the email link points to when frontendEnabled=false.
     * Instead of returning JSON (which was the bug), it redirects the browser
     * to the Angular reset-password page, passing the token as a query param.
     * Angular's ResetPasswordComponent then reads ?token= and submits the form.
     */
    @GetMapping("/reset-password")
    public RedirectView resetViaEmailLink(
            @RequestParam String token,
            @org.springframework.beans.factory.annotation.Value("${app.frontendBaseUrl}") String frontendBaseUrl) {
        // Just redirect — don't validate token here, Angular form will POST it
        return new RedirectView(frontendBaseUrl + "/reset-password?token=" + token);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest req) {
        return ResponseEntity.ok(auth.refresh(req.getRefreshToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiMessage> logout(@Valid @RequestBody RefreshRequest req) {
        auth.logout(req.getRefreshToken());
        return ResponseEntity.ok(new ApiMessage("Logged out"));
    }
}