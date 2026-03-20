package edu.automarket.user.dto;

import edu.automarket.user.User;

public record AuthResponseDTO(
        String token,
        long tokenExpiresInSeconds,
        UserDTO profile
) {
    public AuthResponseDTO(String token, long tokenExpiresInSeconds, User user) {
        this(token, tokenExpiresInSeconds, new UserDTO(user));
    }
}
