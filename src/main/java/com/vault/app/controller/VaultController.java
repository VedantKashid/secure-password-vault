package com.vault.app.controller;

import com.vault.app.dto.PasswordRequestDTO;
import com.vault.app.entity.SavedPassword;
import com.vault.app.service.VaultService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/vault")
@RequiredArgsConstructor
public class VaultController {

    private final VaultService vaultService;
    private final com.vault.app.util.PasswordGenerator passwordGenerator;
    private final com.vault.app.util.PasswordStrengthChecker strengthChecker;
    private final com.vault.app.service.BreachDetectionService breachService;

    // Notice we removed /{userId} from the URL!
    @PostMapping("/add")
    public ResponseEntity<String> addPassword(Principal principal, @Valid @RequestBody PasswordRequestDTO request) {
        try {
            // principal.getName() automatically extracts the username from the verified JWT
            vaultService.savePassword(principal.getName(), request);
            return new ResponseEntity<>("Password saved securely!", HttpStatus.CREATED);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/all")
    public ResponseEntity<List<SavedPassword>> getAllPasswords(Principal principal) {
        return ResponseEntity.ok(vaultService.getUserPasswords(principal.getName()));
    }
    // Update an existing password
    @PutMapping("/{passwordId}")
    public ResponseEntity<String> updatePassword(Principal principal, @PathVariable Long passwordId, @Valid @RequestBody PasswordRequestDTO request) {
        try {
            vaultService.updatePassword(principal.getName(), passwordId, request);
            return ResponseEntity.ok("Password updated successfully!");
        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    // Delete a password
    @DeleteMapping("/{passwordId}")
    public ResponseEntity<String> deletePassword(Principal principal, @PathVariable Long passwordId) {
        try {
            vaultService.deletePassword(principal.getName(), passwordId);
            return ResponseEntity.ok("Password deleted successfully!");
        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }
    // Search passwords by platform keyword
    @GetMapping("/search")
    public ResponseEntity<List<SavedPassword>> searchPasswords(Principal principal, @RequestParam String keyword) {
        try {
            List<SavedPassword> results = vaultService.searchPasswords(principal.getName(), keyword);
            return ResponseEntity.ok(results);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }
    // Generate a secure random password
    @GetMapping("/generate")
    public ResponseEntity<?> generatePassword(
            @RequestParam(defaultValue = "16") int length,
            @RequestParam(defaultValue = "true") boolean useSpecial) {

        if (length < 8 || length > 128) {
            return new ResponseEntity<>("Length must be between 8 and 128", HttpStatus.BAD_REQUEST);
        }

        String generatedPassword = passwordGenerator.generatePassword(length, useSpecial);
        return ResponseEntity.ok(new com.vault.app.dto.PasswordGeneratorDTO(generatedPassword, length, useSpecial));
    }
    // Check password strength
    @PostMapping("/check-strength")
    public ResponseEntity<com.vault.app.dto.PasswordStrengthDTO> checkPasswordStrength(
            @RequestBody com.vault.app.dto.CheckStrengthDTO dto) {

        com.vault.app.dto.PasswordStrengthDTO result = strengthChecker.checkStrength(dto.getPassword());
        return ResponseEntity.ok(result);
    }
    // Trigger a full vault breach scan
    @GetMapping("/scan-breaches")
    public ResponseEntity<com.vault.app.dto.BreachScanResultDTO> scanForBreaches(Principal principal) {
        return ResponseEntity.ok(breachService.scanAllPasswords(principal.getName()));
    }

    // Retrieve only the passwords currently marked as breached
    // Retrieve only the passwords currently marked as breached
    @GetMapping("/breached-passwords")
    public ResponseEntity<List<SavedPassword>> getBreachedPasswords(Principal principal) {
        // Fetch all passwords and filter for the breached ones
        List<SavedPassword> allPasswords = vaultService.getUserPasswords(principal.getName());
        List<SavedPassword> breachedOnly = allPasswords.stream()
                .filter(p -> Boolean.TRUE.equals(p.getIsBreached()))
                .toList();

        return ResponseEntity.ok(breachedOnly);
    }
}