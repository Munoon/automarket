package edu.automarket.listing;

import edu.automarket.listing.dto.UpdateCarListingRequestDTO;
import edu.automarket.listing.model.BodyType;
import edu.automarket.listing.model.CarBrand;
import edu.automarket.listing.model.CarColor;
import edu.automarket.listing.model.CarCondition;
import edu.automarket.listing.model.City;
import edu.automarket.listing.model.DriveType;
import edu.automarket.listing.model.FuelType;
import edu.automarket.listing.model.TransmissionType;

public class CarListingTestUtils {
    public static final UpdateCarListingRequestDTO UPDATE_CAR_LISTING_REQUEST_DTO = new UpdateCarListingRequestDTO(
            "My Car Listing", "Some descriprion", CarBrand.TOYOTA,
            null, "Model", "AA1111AA", CarCondition.NEW,
            100, 100L, City.KYIV, CarColor.WHITE, TransmissionType.AUTOMATIC,
            FuelType.PETROL, 10.4, DriveType.FWD, BodyType.SEDAN, 2025, 20.0, 0
    );
}
