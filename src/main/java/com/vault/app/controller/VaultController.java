package com.vault.app.controller;

import com.vault.app.dto.*;
import com.vault.app.entity.SavedPassword;
import com.vault.app.service.VaultService;
import com.vault.app.service.BreachDetectionService;
import com.vault.app.util.PasswordGenerator;
import com.vault.app.util.PasswordStrengthChecker;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/passwords")  // ← Changed from /api/vault
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class VaultController {

    private final VaultService vaultService;
    private final PasswordGenerator passwordGenerator;
    private final PasswordStrengthChecker strengthChecker;
    private final BreachDetectionService breachService;

    // POST /api/passwords (was /api/vault/add)
    @PostMapping
    public ResponseEntity<?> addPassword(Principal principal, @Valid @RequestBody PasswordRequestDTO request) {
        try {
            vaultService.savePassword(principal.getName(), request);
            return new ResponseEntity<>(
                    new ApiResponse("Password saved successfully!", null),
                    HttpStatus.CREATED
            );
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest()
                    .body(new ApiResponse("Error saving password", e.getMessage()));
        }
    }

    // GET /api/passwords (was /api/vault/all)
    @GetMapping
    public ResponseEntity<?> getAllPasswords(Principal principal) {
        try {
            List<SavedPassword> passwords = vaultService.getUserPasswords(principal.getName());
            return ResponseEntity.ok(new ApiResponse("Success", passwords));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest()
                    .body(new ApiResponse("Error", e.getMessage()));
        }
    }

    // PUT /api/passwords/{passwordId}
    @PutMapping("/{passwordId}")
    public ResponseEntity<?> updatePassword(
            Principal principal,
            @PathVariable Long passwordId,
            @Valid @RequestBody PasswordRequestDTO request) {
        try {
            vaultService.updatePassword(principal.getName(), passwordId, request);
            return ResponseEntity.ok(new ApiResponse("Password updated successfully!", null));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest()
                    .body(new ApiResponse("Error updating password", e.getMessage()));
        }
    }

    // DELETE /api/passwords/{passwordId}
    @DeleteMapping("/{passwordId}")
    public ResponseEntity<?> deletePassword(Principal principal, @PathVariable Long passwordId) {
        try {
            vaultService.deletePassword(principal.getName(), passwordId);
            return ResponseEntity.ok(new ApiResponse("Password deleted successfully!", null));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest()
                    .body(new ApiResponse("Error deleting password", e.getMessage()));
        }
    }

    // GET /api/passwords/search?keyword=... (was /api/vault/search)
    @GetMapping("/search")
    public ResponseEntity<?> searchPasswords(Principal principal, @RequestParam String keyword) {
        try {
            List<SavedPassword> results = vaultService.searchPasswords(principal.getName(), keyword);
            return ResponseEntity.ok(new ApiResponse("Search results", results));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest()
                    .body(new ApiResponse("Error searching", e.getMessage()));
        }
    }

    // GET /api/passwords/generate
    @GetMapping("/generate")
    public ResponseEntity<?> generatePassword(
            @RequestParam(defaultValue = "16") int length,
            @RequestParam(defaultValue = "true") boolean useSpecial) {
        try {
            if (length < 8 || length > 128) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse("Invalid length", "Must be 8-128"));
            }
            String password = passwordGenerator.generatePassword(length, useSpecial);
            return ResponseEntity.ok(new ApiResponse("Success",
                    new PasswordGeneratorDTO(password, length, useSpecial)));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse("Error generating password", e.getMessage()));
        }
    }

    // POST /api/passwords/check-strength
    @PostMapping("/check-strength")
    public ResponseEntity<?> checkPasswordStrength(@Valid @RequestBody CheckStrengthDTO dto) {
        try {
            PasswordStrengthDTO result = strengthChecker.checkStrength(dto.getPassword());
            return ResponseEntity.ok(new ApiResponse("Success", result));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse("Error", e.getMessage()));
        }
    }

    // GET /api/passwords/scan-breaches
    @GetMapping("/scan-breaches")
    public ResponseEntity<?> scanForBreaches(Principal principal) {
        try {
            BreachScanResultDTO result = breachService.scanAllPasswords(principal.getName());
            return ResponseEntity.ok(new ApiResponse("Scan complete", result));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest()
                    .body(new ApiResponse("Error scanning", e.getMessage()));
        }
    }

    // GET /api/passwords/breached-passwords
    @GetMapping("/breached-passwords")
    public ResponseEntity<?> getBreachedPasswords(Principal principal) {
        try {
            List<SavedPassword> all = vaultService.getUserPasswords(principal.getName());
            List<SavedPassword> breached = all.stream()
                    .filter(p -> Boolean.TRUE.equals(p.getIsBreached()))
                    .toList();
            return ResponseEntity.ok(new ApiResponse("Breached passwords", breached));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse("Error", e.getMessage()));
        }
    }
}