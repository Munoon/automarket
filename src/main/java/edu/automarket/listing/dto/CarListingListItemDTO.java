package edu.automarket.listing.dto;

import edu.automarket.listing.model.ListingStatus;

public record CarListingListItemDTO(long id, ListingStatus status, String title) {}
