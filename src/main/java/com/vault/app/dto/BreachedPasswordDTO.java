package com.vault.app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BreachedPasswordDTO {
    private Long passwordId;
    private String platform;
    private String loginUsername;
    private String message;
}