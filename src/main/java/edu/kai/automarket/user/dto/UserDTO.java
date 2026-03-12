package edu.kai.automarket.user.dto;

import edu.kai.automarket.user.User;

public record UserDTO(
        Long id,
        String email,
        String displayName,
        long createdAt,
        boolean active
) {
    public UserDTO(User user) {
        this(user.id(), user.email(), user.displayName(), user.createdAt(), user.active());
    }
}
