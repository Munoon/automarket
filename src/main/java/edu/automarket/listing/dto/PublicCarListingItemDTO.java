package edu.automarket.listing.dto;

import edu.automarket.listing.model.City;
import edu.automarket.listing.model.FuelType;
import edu.automarket.listing.model.TransmissionType;

public record PublicCarListingItemDTO(
        long id,
        String title,
        String[] imageUrls,
        Long price,
        Integer mileage,
        FuelType fuelType,
        TransmissionType transmission,
        City city,
        Integer year,
        boolean isPromoted
) {
}
