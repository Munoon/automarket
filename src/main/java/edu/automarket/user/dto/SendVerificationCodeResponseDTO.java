package edu.automarket.user.dto;

public record SendVerificationCodeResponseDTO(
        int codeTimeToLiveSeconds
) {
}
