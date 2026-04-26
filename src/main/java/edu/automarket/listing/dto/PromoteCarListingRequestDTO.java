package edu.automarket.listing.dto;

import jakarta.validation.constraints.NotNull;

public record PromoteCarListingRequestDTO(
        @NotNull
        CarListingPromotionPeriod period
) {
}
