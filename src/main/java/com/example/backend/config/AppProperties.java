


package com.example.backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {
    private String frontendBaseUrl;
    private String backendBaseUrl;        // NEW
    private EmailLinks emailLinks = new EmailLinks(); // NEW
    private Jwt jwt = new Jwt();
    private Password password = new Password();
    private Verification verification = new Verification();
    private Reset reset = new Reset();
    private Cors cors = new Cors();

    @Data
    public static class EmailLinks {
        private boolean frontendEnabled = false; // choose frontend vs backend links in emails
    }

    @Data
    public static class Jwt {
        private String secret;
        private int accessTokenTtlMinutes;
        private int refreshTokenTtlDays;
        private int refreshTokenTtlDaysRememberMe;
    }

    @Data
    public static class Password {
        private int minLength;
        private int minUppercase;
        private int minLowercase;
        private int minDigits;
        private int minSpecial;
    }

    @Data
    public static class Verification { private int ttlMinutes; }

    @Data
    public static class Reset { private int ttlMinutes; }

    @Data
    public static class Cors { private List<String> allowedOrigins; }
}