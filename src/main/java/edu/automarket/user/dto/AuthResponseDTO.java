package edu.automarket.user.dto;

import edu.automarket.user.User;

public record AuthResponseDTO(
        String token,
        long tokenExpiresInSeconds,
        Long id,
        String username,
        String phoneNumber,
        String displayName,
        long createdAt,
        boolean active
) {
    public AuthResponseDTO(String token, long tokenExpiresInSeconds, User user) {
        this(token, tokenExpiresInSeconds, user.id(), user.username(), user.phoneNumber(), user.displayName(), user.createdAt(), user.active());
    }
}
