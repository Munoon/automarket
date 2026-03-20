package edu.automarket.user.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record SendVerificationCodeRequestDTO(
        @NotNull
        @Pattern(regexp = "^\\+\\d{12}$", message = "Phone number must be in format +XXXXXXXXXXXX")
        String phoneNumber
) {
}
