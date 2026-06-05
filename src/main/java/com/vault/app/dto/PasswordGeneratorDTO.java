package com.vault.app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PasswordGeneratorDTO {
    private String password;
    private int length;
    private boolean useSpecial;
}