package edu.automarket.listing.dto;

import edu.automarket.common.ApiException;
import edu.automarket.common.validation.AllowedCharacters;
import edu.automarket.common.validation.CharacterType;
import edu.automarket.listing.model.BodyType;
import edu.automarket.listing.model.CarBrand;
import edu.automarket.listing.model.CarColor;
import edu.automarket.listing.model.CarCondition;
import edu.automarket.listing.model.City;
import edu.automarket.listing.model.DriveType;
import edu.automarket.listing.model.FuelType;
import edu.automarket.listing.model.TransmissionType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;

public record UpdateCarListingRequestDTO(
        @Size(min = 5, max = 200)
        @AllowedCharacters({CharacterType.ALPHABETICAL, CharacterType.DIGIT, CharacterType.SPECIAL_SYMBOL})
        String title,

        @Size(min = 5, max = 5000)
        @AllowedCharacters({CharacterType.ALPHABETICAL, CharacterType.DIGIT, CharacterType.SPECIAL_SYMBOL})
        String description,

        @Size(max = 10)
        String @Size(max = 200)[] imageKeys,

        CarBrand brand,

        @Size(min = 1, max = 100)
        @AllowedCharacters({CharacterType.ALPHABETICAL, CharacterType.DIGIT, CharacterType.SPECIAL_SYMBOL})
        String customBrandName,

        @Size(min = 1, max = 100)
        @AllowedCharacters({CharacterType.ALPHABETICAL, CharacterType.DIGIT, CharacterType.SPECIAL_SYMBOL})
        String model,

        @Size(min = 1, max = 20)
        @AllowedCharacters({CharacterType.ALPHABETICAL, CharacterType.DIGIT, CharacterType.SPECIAL_SYMBOL})
        String licensePlate,

        CarCondition condition,

        @PositiveOrZero
        Integer mileage,

        @PositiveOrZero
        Long price,

        City city,
        CarColor color,
        TransmissionType transmission,
        FuelType fuelType,

        @Positive
        Double tankVolume,

        DriveType driveType,
        BodyType bodyType,

        @Min(1900) @Max(2030)
        Integer year,

        @Positive
        Double engineVolume,

        @PositiveOrZero
        Integer ownersCount
) {
    public void validate() {
        if (brand == CarBrand.CUSTOM) {
            if (customBrandName == null) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "/problems/missing-custom-brand-name", "customBrandName is required when brand is CUSTOM");
            }
        } else {
            if (customBrandName != null) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "/problems/invalid-custom-brand-name", "customBrandName can only be set when brand is CUSTOM");
            }
        }
    }
}
