package edu.automarket.user;

public record User(
        long id,
        String phoneNumber,
        String displayName,
        long createdAt,
        boolean active
) {
}
