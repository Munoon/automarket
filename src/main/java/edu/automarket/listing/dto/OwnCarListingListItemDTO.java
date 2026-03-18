package edu.automarket.listing.dto;

import edu.automarket.listing.model.ListingStatus;

public record OwnCarListingListItemDTO(
        long id,
        ListingStatus status,
        String title,
        long publishedAt
) {
}
