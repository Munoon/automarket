package edu.kai.automarket.user.dto;

public record RegisterRequestDTO(
        String email,
        String passwordHash,
        String displayName
) {
}
