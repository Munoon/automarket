package edu.automarket.listing.dto;

import edu.automarket.listing.model.BodyType;
import edu.automarket.listing.model.CarBrand;
import edu.automarket.listing.model.CarColor;
import edu.automarket.listing.model.CarCondition;
import edu.automarket.listing.model.City;
import edu.automarket.listing.model.DriveType;
import edu.automarket.listing.model.FuelType;
import edu.automarket.listing.model.TransmissionType;

public record PublicCarListingDTO(
        long id,
        String authorDisplayName,
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
        long publishedAt
) {}
