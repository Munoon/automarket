package edu.kai.automarket.user;

public record User(
        Long id,
        String email,
        String passwordHash,
        String displayName,
        long createdAt,
        boolean active
) {
}
