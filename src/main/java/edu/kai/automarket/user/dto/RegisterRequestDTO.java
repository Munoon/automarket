package edu.kai.automarket.user.dto;

public record RegisterRequestDTO(
        String username,
        String phoneNumber,
        String passwordHash,
        String displayName
) {
}
