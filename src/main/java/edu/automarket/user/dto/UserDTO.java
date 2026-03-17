package edu.automarket.user.dto;

import edu.automarket.user.User;

public record UserDTO(
        Long id,
        String username,
        String phoneNumber,
        String displayName,
        long createdAt,
        boolean active
) {
    public UserDTO(User user) {
        this(user.id(), user.username(), user.phoneNumber(), user.displayName(), user.createdAt(), user.active());
    }
}
