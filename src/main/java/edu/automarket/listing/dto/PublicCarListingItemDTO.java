package edu.automarket.listing.dto;

import edu.automarket.listing.model.CarBrand;
import edu.automarket.listing.model.CarListing;

public record PublicCarListingItemDTO(
        long id,
        String title,
        Long price,
        String description,
        CarBrand brand,
        String customBrandName,
        String model
) {
    public PublicCarListingItemDTO(CarListing listing) {
        this(listing.id(), listing.title(), listing.price(), listing.description(),
                listing.brand(), listing.customBrandName(), listing.model());
    }
}
