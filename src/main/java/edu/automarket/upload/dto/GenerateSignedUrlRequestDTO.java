package edu.automarket.upload.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record GenerateSignedUrlRequestDTO(
        @Positive
        int listingId,

        @Positive
        @Max(10 * 1024 * 1024)
        long contentLength,

        @NotBlank
        @Size(min = 24, max = 24)
        String md5,

        @NotNull
        @Pattern(regexp = "image/(jpeg|png|webp)")
        String contentType
) {
}
