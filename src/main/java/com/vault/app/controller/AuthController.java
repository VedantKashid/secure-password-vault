package com.vault.app.controller;

import com.vault.app.dto.ApiResponse;
import com.vault.app.dto.UserLoginDTO;
import com.vault.app.dto.UserRegistrationDTO;
import com.vault.app.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j                              // FIX: Added proper logger — was using no logging at all
@RestController
@RequestMapping("/api/auth")
/*
 * FIX (SECURITY): @CrossOrigin(origins = "*") allows ANY website to call your API
 * from a browser. For a demo this is fine; before deployment restrict to your
 * actual frontend origin, e.g. @CrossOrigin(origins = "http://localhost:5500")
 */
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse> register(@Valid @RequestBody UserRegistrationDTO registrationDTO) {
        /*
         * FIX: Was catching RuntimeException — too narrow.
         * If registerUser() throws a checked exception (e.g. encryption error,
         * DB connectivity issue), it was not caught here and Spring would
         * return a 500 with a raw stack trace visible to the client.
         * Changed to Exception so all failures are handled gracefully.
         *
         * NOTE on @Valid: Spring throws MethodArgumentNotValidException BEFORE
         * the method body is entered, so the try-catch never sees it.
         * Validation errors are handled by GlobalExceptionHandler (see that class).
         */
        try {
            userService.registerUser(registrationDTO);
            // FIX: Was returning 200 OK. REST convention for resource creation is 201 CREATED.
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ApiResponse("User registered successfully!", null));
        } catch (Exception e) {
            // FIX: Log the error server-side instead of printing stack trace to console
            log.error("Registration failed for user '{}': {}", registrationDTO.getUsername(), e.getMessage(), e);
            /*
             * FIX (SECURITY): Was returning e.getMessage() directly to the client.
             * This can leak internal details (DB column names, class names, etc.).
             * Return a generic message; log the real one server-side.
             */
            return ResponseEntity.badRequest()
                    .body(new ApiResponse("Registration failed", "Username or email may already be in use."));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse> login(@Valid @RequestBody UserLoginDTO loginDTO) {
        /*
         * FIX: Same as above — was catching only RuntimeException.
         * Changed to Exception.
         *
         * SECURITY NOTE: This endpoint has no rate limiting. A simple
         * brute-force attack can try unlimited passwords. Consider adding
         * Spring Security's built-in lockout, or a library like Bucket4j.
         */
        try {
            String token = userService.login(loginDTO);
            return ResponseEntity.ok(new ApiResponse("Login successful", token));
        } catch (Exception e) {
            log.warn("Login attempt failed for user '{}': {}", loginDTO.getUsername(), e.getMessage());
            /*
             * FIX (SECURITY): Return a fixed generic message for auth failures.
             * Never tell the caller whether the username or the password was wrong —
             * that leaks user enumeration info to an attacker.
             */
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse("Error", "Invalid username or password."));
        }
    }
}