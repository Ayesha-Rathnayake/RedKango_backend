package com.example.backend.validation;

import com.example.backend.config.AppProperties;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.stereotype.Component;

@Component
public class StrongPasswordValidator implements ConstraintValidator<StrongPassword, String> {

    private final AppProperties props;

    public StrongPasswordValidator(AppProperties props) { this.props = props; }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) return false;

        AppProperties.Password p = props.getPassword();
        int upper = 0, lower = 0, digit = 0, special = 0;

        for (char c : value.toCharArray()) {
            if (Character.isUpperCase(c)) upper++;
            else if (Character.isLowerCase(c)) lower++;
            else if (Character.isDigit(c)) digit++;
            else special++;
        }

        boolean ok = value.length() >= p.getMinLength()
                && upper >= p.getMinUppercase()
                && lower >= p.getMinLowercase()
                && digit >= p.getMinDigits()
                && special >= p.getMinSpecial();

        if (!ok) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    String.format("Password must be at least %d chars, with %d upper, %d lower, %d digits, %d special",
                            p.getMinLength(), p.getMinUppercase(), p.getMinLowercase(), p.getMinDigits(), p.getMinSpecial())
            ).addConstraintViolation();
        }
        return ok;
    }
}