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

/**
 * REST Controller handling all secure vault operations.
 * Exposes endpoints for CRUD operations on encrypted passwords,
 * password generation, strength checking, and breach detection.
 * All endpoints require a valid JWT bearer token.
 */
@RestController
@RequestMapping("/api/passwords")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class VaultController {

    private final VaultService vaultService;
    private final PasswordGenerator passwordGenerator;
    private final PasswordStrengthChecker strengthChecker;
    private final BreachDetectionService breachService;

    /**
     * Encrypts and saves a new credential to the user's vault.
     *
     * @param principal The authenticated user's security context
     * @param request The DTO containing plaintext credentials to be encrypted
     * @return ApiResponse containing the success status
     */
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

    /**
     * Retrieves and decrypts all credentials owned by the authenticated user.
     *
     * @param principal The authenticated user's security context
     * @return ApiResponse containing a list of decrypted SavedPassword objects
     */
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

    /**
     * Updates an existing credential. Validates ownership before processing to prevent IDOR.
     *
     * @param principal The authenticated user's security context
     * @param passwordId The database ID of the credential to update
     * @param request The DTO containing the updated plaintext credentials
     * @return ApiResponse indicating success or failure
     */
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

    /**
     * Permanently deletes a credential from the vault.
     * Validates ownership to prevent unauthorized deletion.
     *
     * @param principal The authenticated user's security context
     * @param passwordId The database ID of the credential to delete
     * @return ApiResponse indicating success or failure
     */
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

    /**
     * Generates a cryptographically secure random password based on specified parameters.
     */
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

    /**
     * Triggers a live threat intelligence scan against the HaveIBeenPwned API
     * for all credentials in the user's vault.
     */
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