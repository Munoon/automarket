package edu.automarket.user.dto;

import edu.automarket.user.User;

public record UserDTO(
        Long id,
        String phoneNumber,
        String displayName,
        long createdAt,
        boolean active
) {
    public UserDTO(User user) {
        this(user.id(), user.phoneNumber(), user.displayName(), user.createdAt(), user.active());
    }
}
