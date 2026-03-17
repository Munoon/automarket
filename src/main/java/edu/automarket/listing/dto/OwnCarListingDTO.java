package edu.automarket.listing.dto;

import edu.automarket.listing.model.CarListing;
import edu.automarket.listing.model.BodyType;
import edu.automarket.listing.model.CarBrand;
import edu.automarket.listing.model.CarColor;
import edu.automarket.listing.model.CarCondition;
import edu.automarket.listing.model.City;
import edu.automarket.listing.model.DriveType;
import edu.automarket.listing.model.FuelType;
import edu.automarket.listing.model.ListingStatus;
import edu.automarket.listing.model.TransmissionType;

public record OwnCarListingDTO(
        long id,
        ListingStatus status,
        String title,
        String description,
        CarBrand brand,
        String customBrandName,
        String model,
        String licensePlate,
        CarCondition condition,
        Integer mileage,
        Long price,
        City city,
        CarColor color,
        TransmissionType transmission,
        FuelType fuelType,
        Double tankVolume,
        DriveType driveType,
        BodyType bodyType,
        Integer year,
        Double engineVolume,
        Integer ownersCount,
        long createdAt,
        long updatedAt
) {
    public OwnCarListingDTO(CarListing listing) {
        this(
                listing.id(),
                listing.status(),
                listing.title(),
                listing.description(),
                listing.brand(),
                listing.customBrandName(),
                listing.model(),
                listing.licensePlate(),
                listing.condition(),
                listing.mileage(),
                listing.price(),
                listing.city(),
                listing.color(),
                listing.transmission(),
                listing.fuelType(),
                listing.tankVolume(),
                listing.driveType(),
                listing.bodyType(),
                listing.year(),
                listing.engineVolume(),
                listing.ownersCount(),
                listing.createdAt(),
                listing.updatedAt()
        );
    }
}
