package com.example.backend.service;

import com.example.backend.config.AppProperties;
import org.springframework.stereotype.Service;

@Service
public class PasswordPolicyService {

    private final AppProperties props;

    public PasswordPolicyService(AppProperties props) {
        this.props = props;
    }

    public void validateOrThrow(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Password required");
        }

        // Explicit type (no 'var')
        AppProperties.Password p = props.getPassword();

        int upper = 0;
        int lower = 0;
        int digit = 0;
        int special = 0;

        for (char c : value.toCharArray()) {
            if (Character.isUpperCase(c)) upper++;
            else if (Character.isLowerCase(c)) lower++;
            else if (Character.isDigit(c)) digit++;
            else special++;
        }

        boolean ok =
                value.length() >= p.getMinLength()
                        && upper >= p.getMinUppercase()
                        && lower >= p.getMinLowercase()
                        && digit >= p.getMinDigits()
                        && special >= p.getMinSpecial();

        if (!ok) {
            String message = String.format(
                    "Password must be at least %d chars, with %d upper, %d lower, %d digits, %d special",
                    p.getMinLength(), p.getMinUppercase(), p.getMinLowercase(), p.getMinDigits(), p.getMinSpecial()
            );
            throw new IllegalArgumentException(message);
        }
    }
}