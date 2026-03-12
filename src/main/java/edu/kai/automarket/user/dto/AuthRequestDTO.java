package edu.kai.automarket.user.dto;

public record AuthRequestDTO(
        String email,
        String passwordHash
) {
}
