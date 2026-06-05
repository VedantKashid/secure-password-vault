package com.vault.app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BreachScanResultDTO {
    private int totalPasswords;
    private int breachedCount;
    private List<BreachedPasswordDTO> breachedPasswords;
    private LocalDateTime scanTime;
}