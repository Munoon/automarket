package edu.automarket.listing.dto;

import edu.automarket.listing.model.CarBrand;

public record PublicCarListingItemDTO(
        long id,
        String title,
        Long price,
        String description,
        CarBrand brand,
        String customBrandName,
        String model
) {
}
