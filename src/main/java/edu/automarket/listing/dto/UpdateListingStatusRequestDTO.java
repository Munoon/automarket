package edu.automarket.listing.dto;

import edu.automarket.listing.model.ListingStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateListingStatusRequestDTO(@NotNull ListingStatus status) {}
