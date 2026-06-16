package com.vault.app.controller;

import com.vault.app.dto.*;
import com.vault.app.entity.SavedPassword;
import com.vault.app.service.VaultService;
import com.vault.app.service.BreachDetectionService;
import com.vault.app.util.PasswordGenerator;
import com.vault.app.util.PasswordStrengthChecker;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@Slf4j                              // FIX: Added proper SLF4J logger
@RestController
@RequestMapping("/api/passwords")
/*
 * FIX (SECURITY): Wildcard CORS — restrict to your frontend origin before deploying.
 * e.g. @CrossOrigin(origins = "http://localhost:5500")
 */
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class VaultController {

    private final VaultService vaultService;
    private final PasswordGenerator passwordGenerator;
    private final PasswordStrengthChecker strengthChecker;
    private final BreachDetectionService breachService;

    // ── Helper: guard against a null Principal ───────────────────────────────
    /*
     * FIX: Every method that uses principal.getName() would throw a
     * NullPointerException if the JWT filter is misconfigured or bypassed
     * (e.g., during testing or if the security config has a gap).
     * Centralised into one helper so every endpoint is protected consistently.
     */
    private ResponseEntity<ApiResponse> unauthorizedIfNull(Principal principal) {
        if (principal == null) {
            log.warn("Request reached controller with null Principal — check Security config");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse("Unauthorized", "Authentication required."));
        }
        return null; // null means "OK, proceed"
    }

    // ── Endpoints ─────────────────────────────────────────────────────────────

    @PostMapping
    /*
     * FIX: Return type was ResponseEntity<?> (wildcard).
     * Changed to ResponseEntity<ApiResponse> on all methods for compile-time type safety.
     */
    public ResponseEntity<ApiResponse> addPassword(
            Principal principal,
            @Valid @RequestBody PasswordRequestDTO request) {

        ResponseEntity<ApiResponse> guard = unauthorizedIfNull(principal);
        if (guard != null) return guard;

        try {
            vaultService.savePassword(principal.getName(), request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ApiResponse("Password saved successfully!", null));
        } catch (Exception e) {
            // FIX: Replaced e.printStackTrace() with structured logging
            log.error("Error saving password for user '{}': {}", principal.getName(), e.getMessage(), e);
            /*
             * FIX: Was catching Exception and always returning 400 Bad Request.
             * A DB outage or encryption failure is a server error (5xx), not a
             * client mistake (4xx). Check the type before choosing status code.
             */
            return serverOrBadRequest(e, "Failed to save password.");
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse> getAllPasswords(Principal principal) {
        ResponseEntity<ApiResponse> guard = unauthorizedIfNull(principal);
        if (guard != null) return guard;

        try {
            List<SavedPassword> passwords = vaultService.getUserPasswords(principal.getName());
            /*
             * DESIGN NOTE: Returning raw SavedPassword entities exposes the
             * encryptedPassword field (cipher text) and couples the API to the DB model.
             * Ideally map to a PasswordResponseDTO before returning.
             * That change requires a new DTO class, so it is flagged here rather
             * than silently added.
             */
            return ResponseEntity.ok(new ApiResponse("Success", passwords));
        } catch (Exception e) {
            log.error("Error fetching passwords for user '{}': {}", principal.getName(), e.getMessage(), e);
            return serverOrBadRequest(e, "Failed to retrieve passwords.");
        }
    }

    @PutMapping("/{passwordId}")
    public ResponseEntity<ApiResponse> updatePassword(
            Principal principal,
            @PathVariable Long passwordId,
            @Valid @RequestBody PasswordRequestDTO request) {

        ResponseEntity<ApiResponse> guard = unauthorizedIfNull(principal);
        if (guard != null) return guard;

        try {
            vaultService.updatePassword(principal.getName(), passwordId, request);
            return ResponseEntity.ok(new ApiResponse("Password updated successfully!", null));
        } catch (Exception e) {
            log.error("Error updating password {} for user '{}': {}", passwordId, principal.getName(), e.getMessage(), e);
            return serverOrBadRequest(e, "Failed to update password.");
        }
    }

    @DeleteMapping("/{passwordId}")
    public ResponseEntity<ApiResponse> deletePassword(
            Principal principal,
            @PathVariable Long passwordId) {

        ResponseEntity<ApiResponse> guard = unauthorizedIfNull(principal);
        if (guard != null) return guard;

        try {
            vaultService.deletePassword(principal.getName(), passwordId);
            // FIX: REST convention for successful deletion is 204 No Content.
            // Using 200 + body is also acceptable and keeps frontend handling uniform.
            return ResponseEntity.ok(new ApiResponse("Password deleted successfully!", null));
        } catch (Exception e) {
            log.error("Error deleting password {} for user '{}': {}", passwordId, principal.getName(), e.getMessage(), e);
            return serverOrBadRequest(e, "Failed to delete password.");
        }
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse> searchPasswords(
            Principal principal,
            @RequestParam String keyword) {

        ResponseEntity<ApiResponse> guard = unauthorizedIfNull(principal);
        if (guard != null) return guard;

        try {
            List<SavedPassword> results = vaultService.searchPasswords(principal.getName(), keyword);
            return ResponseEntity.ok(new ApiResponse("Search results", results));
        } catch (Exception e) {
            log.error("Error searching passwords for user '{}': {}", principal.getName(), e.getMessage(), e);
            return serverOrBadRequest(e, "Search failed.");
        }
    }

    /*
     * FIX (SECURITY): generatePassword had no Principal parameter.
     * If Spring Security ever has a misconfigured path exclusion for
     * /api/passwords/generate, this endpoint would be publicly accessible
     * without a valid JWT. Added Principal + null guard to be explicit.
     */
    @GetMapping("/generate")
    public ResponseEntity<ApiResponse> generatePassword(
            Principal principal,
            @RequestParam(defaultValue = "16") int length,
            @RequestParam(defaultValue = "true") boolean useSpecial) {

        ResponseEntity<ApiResponse> guard = unauthorizedIfNull(principal);
        if (guard != null) return guard;

        try {
            if (length < 8 || length > 128) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse("Invalid length", "Must be between 8 and 128."));
            }
            String password = passwordGenerator.generatePassword(length, useSpecial);
            return ResponseEntity.ok(new ApiResponse("Success",
                    new PasswordGeneratorDTO(password, length, useSpecial)));
        } catch (Exception e) {
            log.error("Error generating password: {}", e.getMessage(), e);
            return serverOrBadRequest(e, "Password generation failed.");
        }
    }

    /*
     * FIX (SECURITY): checkPasswordStrength also had no Principal — same risk as above.
     */
    @PostMapping("/check-strength")
    public ResponseEntity<ApiResponse> checkPasswordStrength(
            Principal principal,
            @Valid @RequestBody CheckStrengthDTO dto) {

        ResponseEntity<ApiResponse> guard = unauthorizedIfNull(principal);
        if (guard != null) return guard;

        try {
            PasswordStrengthDTO result = strengthChecker.checkStrength(dto.getPassword());
            return ResponseEntity.ok(new ApiResponse("Success", result));
        } catch (Exception e) {
            log.error("Error checking password strength: {}", e.getMessage(), e);
            return serverOrBadRequest(e, "Strength check failed.");
        }
    }

    @GetMapping("/scan-breaches")
    public ResponseEntity<ApiResponse> scanForBreaches(Principal principal) {
        ResponseEntity<ApiResponse> guard = unauthorizedIfNull(principal);
        if (guard != null) return guard;

        try {
            BreachScanResultDTO result = breachService.scanAllPasswords(principal.getName());
            return ResponseEntity.ok(new ApiResponse("Scan complete", result));
        } catch (Exception e) {
            log.error("Error scanning breaches for user '{}': {}", principal.getName(), e.getMessage(), e);
            return serverOrBadRequest(e, "Breach scan failed.");
        }
    }

    @GetMapping("/breached-passwords")
    public ResponseEntity<ApiResponse> getBreachedPasswords(Principal principal) {
        ResponseEntity<ApiResponse> guard = unauthorizedIfNull(principal);
        if (guard != null) return guard;

        try {
            List<SavedPassword> all = vaultService.getUserPasswords(principal.getName());
            // FIX: Boolean.TRUE.equals() is null-safe — this was already correct ✓
            List<SavedPassword> breached = all.stream()
                    .filter(p -> Boolean.TRUE.equals(p.getIsBreached()))
                    .toList();
            return ResponseEntity.ok(new ApiResponse("Breached passwords", breached));
        } catch (Exception e) {
            log.error("Error fetching breached passwords for user '{}': {}", principal.getName(), e.getMessage(), e);
            return serverOrBadRequest(e, "Failed to retrieve breached passwords.");
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /*
     * FIX: Was returning 400 Bad Request for ALL exceptions, including server errors
     * like DB outages or encryption failures. Those are 5xx errors, not client mistakes.
     * This helper picks the right status code based on the exception type.
     */
    private ResponseEntity<ApiResponse> serverOrBadRequest(Exception e, String clientMessage) {
        boolean isServerError = !(e instanceof IllegalArgumentException
                || e instanceof IllegalStateException
                || e instanceof SecurityException);

        HttpStatus status = isServerError ? HttpStatus.INTERNAL_SERVER_ERROR : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(new ApiResponse("Error", clientMessage));
    }
}