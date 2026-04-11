package edu.automarket.user.dto;

import edu.automarket.user.User;

public record AuthResponseDTO(
        String token,
        long tokenExpiresInSeconds,
        UserDTO profile,
        LimitsDTO limits,
        long ownListingsCount
) {
    public AuthResponseDTO(String token, long tokenExpiresInSeconds, User user, LimitsDTO limits, long ownListingsCount) {
        this(token, tokenExpiresInSeconds, new UserDTO(user), limits, ownListingsCount);
    }
}
