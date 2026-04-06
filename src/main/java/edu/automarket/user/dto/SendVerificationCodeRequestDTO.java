package edu.automarket.user.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SendVerificationCodeRequestDTO(
        @NotNull
        @Pattern(regexp = "^\\+\\d{12}$", message = "Phone number must be in format +XXXXXXXXXXXX")
        String phoneNumber,

        @Size(max = 1024)
        String captchaToken
) {
}
