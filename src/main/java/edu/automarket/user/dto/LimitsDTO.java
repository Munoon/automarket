package edu.automarket.user.dto;

public record LimitsDTO(
        long listingRepublishCooldownMS,
        int listingsCountLimitPerAuthor
) {
}
