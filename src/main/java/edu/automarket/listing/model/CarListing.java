package edu.automarket.listing.model;

import edu.automarket.common.ApiException;
import edu.automarket.listing.dto.UpdateCarListingRequestDTO;
import org.springframework.http.HttpStatus;

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

    public void validatePublishedListingFields() {
        if (title == null) throw incomplete("title");
        if (description == null) throw incomplete("description");
        if (brand == null) throw incomplete("brand");
        if (brand == CarBrand.CUSTOM && customBrandName == null) throw incomplete("customBrandName");
        if (model == null) throw incomplete("model");
        if (licensePlate == null) throw incomplete("licensePlate");
        if (condition == null) throw incomplete("condition");
        if (mileage == null) throw incomplete("mileage");
        if (price == null) throw incomplete("price");
        if (city == null) throw incomplete("city");
        if (color == null) throw incomplete("color");
        if (transmission == null) throw incomplete("transmission");
        if (fuelType == null) throw incomplete("fuelType");
        if (tankVolume == null) throw incomplete("tankVolume");
        if (driveType == null) throw incomplete("driveType");
        if (bodyType == null) throw incomplete("bodyType");
        if (year == null) throw incomplete("year");
        if (engineVolume == null) throw incomplete("engineVolume");
        if (ownersCount == null) throw incomplete("ownersCount");
    }

    private static ApiException incomplete(String field) {
        return new ApiException(HttpStatus.BAD_REQUEST, "/problems/listing-incomplete",
                "Listing field is required for publishing: " + field);
    }
}
