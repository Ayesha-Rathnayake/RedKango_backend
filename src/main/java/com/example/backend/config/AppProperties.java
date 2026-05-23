package com.example.backend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    // ── Top-level URL fields (used directly by EmailService) ──────────────────
    // These map to: app.frontendBaseUrl and app.backendBaseUrl
    private String frontendBaseUrl;
    private String backendBaseUrl;

    // ── Nested config blocks ───────────────────────────────────────────────────
    private final Jwt jwt                = new Jwt();
    private final Verification verification = new Verification();
    private final Reset reset            = new Reset();
    private final Password password      = new Password();
    private final Admin admin            = new Admin();
    private final Cors cors              = new Cors();
    private final EmailLinks emailLinks  = new EmailLinks();

    // ── JWT ───────────────────────────────────────────────────────────────────
    @Getter @Setter
    public static class Jwt {
        private String secret;
        private int accessTokenTtlMinutes;
        private int refreshTokenTtlDays;
        private int refreshTokenTtlDaysRememberMe;
    }

    // ── Verification token ────────────────────────────────────────────────────
    @Getter @Setter
    public static class Verification {
        private int ttlMinutes;
    }

    // ── Password reset token ──────────────────────────────────────────────────
    @Getter @Setter
    public static class Reset {
        private int ttlMinutes;
    }

    // ── Password policy ───────────────────────────────────────────────────────
    @Getter @Setter
    public static class Password {
        private int minLength;
        private int minUppercase;
        private int minLowercase;
        private int minDigits;
        private int minSpecial;
    }

    // ── Admin seed account ────────────────────────────────────────────────────
    // Maps to: app.admin.email, app.admin.password, etc.
    // In production override with env vars: APP_ADMIN_EMAIL, APP_ADMIN_PASSWORD
    @Getter @Setter
    public static class Admin {
        private String firstName = "Admin";
        private String lastName  = "RedKango";
        private String email;
        private String password;
        private String phone     = "0000000000";
    }

    // ── CORS ──────────────────────────────────────────────────────────────────
    // Maps to: app.cors.allowedOrigins
    // Used by CorsConfig: props.getCors().getAllowedOrigins()
    @Getter @Setter
    public static class Cors {
        private List<String> allowedOrigins;
    }

    // ── Email link strategy ───────────────────────────────────────────────────
    // Maps to: app.emailLinks.frontendEnabled
    // Used by EmailService: props.getEmailLinks().isFrontendEnabled()
    @Getter @Setter
    public static class EmailLinks {
        private boolean frontendEnabled = true;
    }
}