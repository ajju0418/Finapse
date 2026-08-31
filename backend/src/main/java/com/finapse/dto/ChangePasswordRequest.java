package com.finapse.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(

        @NotBlank(message = "Your current password is required")
        String currentPassword,

        @NotBlank(message = "A new password is required")
        @Size(min = 10, max = 128, message = "Password must be between 10 and 128 characters")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
                message = "Password must include an uppercase letter, a lowercase letter and a number"
        )
        String newPassword
) {}
