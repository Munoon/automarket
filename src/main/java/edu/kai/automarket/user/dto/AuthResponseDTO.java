package edu.kai.automarket.user.dto;

import edu.kai.automarket.user.User;

public record AuthResponseDTO(
        String token,
        long tokenExpiresInSeconds,
        Long id,
        String email,
        String displayName,
        long createdAt,
        boolean active
) {
    public AuthResponseDTO(String token, long tokenExpiresInSeconds, User user) {
        this(token, tokenExpiresInSeconds, user.id(), user.email(), user.displayName(), user.createdAt(), user.active());
    }
}
