package com.vault.app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;

@Data
@AllArgsConstructor
public class PasswordStrengthDTO {
    private String strength;
    private int score;
    private List<String> warnings;
}