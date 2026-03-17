package edu.kai.automarket.user;

public record User(
        Long id,
        String username,
        String phoneNumber,
        String passwordHash,
        String displayName,
        long createdAt,
        boolean active
) {
}
