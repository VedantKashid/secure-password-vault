package com.vault.app.util;

import com.vault.app.dto.PasswordStrengthDTO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class PasswordStrengthChecker {

    public PasswordStrengthDTO checkStrength(String password) {
        int score = 0;
        List<String> warnings = new ArrayList<>();

        if (password.length() >= 8) score += 20;
        if (password.length() >= 12) score += 10;
        if (password.length() >= 16) score += 10;

        if (password.matches(".*[a-z].*")) score += 15;
        if (password.matches(".*[A-Z].*")) score += 15;
        if (password.matches(".*\\d.*")) score += 15;
        if (password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{}|;:,.<>?].*")) score += 15;

        // Warnings
        if (password.length() < 8) warnings.add("Password too short (min 8 chars)");
        if (!password.matches(".*[A-Z].*")) warnings.add("Add uppercase letters");
        if (!password.matches(".*[a-z].*")) warnings.add("Add lowercase letters");
        if (!password.matches(".*\\d.*")) warnings.add("Add numbers");
        if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{}|;:,.<>?].*"))
            warnings.add("Add special characters");
        if (password.matches(".*(.)(\\1{2,}).*"))
            warnings.add("Avoid repeating characters");

        String strength = score >= 80 ? "STRONG" : score >= 50 ? "MEDIUM" : "WEAK";

        return new PasswordStrengthDTO(strength, score, warnings);
    }
}