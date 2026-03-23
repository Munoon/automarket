package edu.automarket.listing.model;

import edu.automarket.listing.dto.UpdateCarListingRequestDTO;

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
        long updatedAt,
        long publishedAt
) {
    public CarListing update(UpdateCarListingRequestDTO request) {
        return new CarListing(
                id, authorUserId, status,
                request.title(), request.description(),
                request.brand(), request.customBrandName(),
                request.model(), request.licensePlate(),
                request.condition(), request.mileage(),
                request.price(), request.city(), request.color(),
                request.transmission(), request.fuelType(),
                request.tankVolume(), request.driveType(),
                request.bodyType(), request.year(), request.engineVolume(),
                request.ownersCount(),
                createdAt,
                System.currentTimeMillis(),
                publishedAt
        );
    }

    public CarListing withStatus(ListingStatus status, long publishedAt) {
        return new CarListing(
                id, authorUserId,
                status,
                title, description, brand, customBrandName, model, licensePlate, condition, mileage, price,
                city, color, transmission, fuelType, tankVolume, driveType, bodyType, year, engineVolume, ownersCount,
                createdAt,
                System.currentTimeMillis(),
                publishedAt
        );
    }
}
