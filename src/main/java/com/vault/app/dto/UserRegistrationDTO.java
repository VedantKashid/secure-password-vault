package com.vault.app.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.Length;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRegistrationDTO {
    @NotBlank(message = "Username required")
    private String username;

    @NotBlank(message = "Password required")
    @Length(min = 8, message = "Password must be at least 8 chars")
    private String password;

    @Email(message = "Valid email required")
    private String email;
}