package edu.automarket.listing.model;

public record CarListing(
        long id,
        long authorUserId,
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
) {}
