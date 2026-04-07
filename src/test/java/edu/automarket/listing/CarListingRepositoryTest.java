package edu.automarket.listing;

import edu.automarket.AbstractIntegrationTest;
import edu.automarket.listing.dto.GetPublishedListingsRequestDTO;
import edu.automarket.listing.model.BodyType;
import edu.automarket.listing.model.CarBrand;
import edu.automarket.listing.model.CarColor;
import edu.automarket.listing.model.CarCondition;
import edu.automarket.listing.model.CarListing;
import edu.automarket.listing.model.City;
import edu.automarket.listing.model.DriveType;
import edu.automarket.listing.model.FuelType;
import edu.automarket.listing.model.ListingStatus;
import edu.automarket.listing.model.TransmissionType;
import edu.automarket.user.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

class CarListingRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private CarListingRepository carListingRepository;

    @Autowired
    private UserService userService;

    @Test
    void createPersistsListingAndAssignsId() {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        long before = System.currentTimeMillis();

        StepVerifier.create(carListingRepository.create(userId, System.currentTimeMillis()))
                .assertNext(listing -> {
                    assertThat(listing.id()).isNotNull();
                    assertThat(listing.authorUserId()).isEqualTo(userId);
                    assertThat(listing.status()).isEqualTo(ListingStatus.DRAFT);
                    assertThat(listing.createdAt()).isGreaterThanOrEqualTo(before);
                    assertThat(listing.updatedAt()).isEqualTo(listing.createdAt());
                    assertThat(listing.title()).isNull();
                })
                .verifyComplete();
    }

    @Test
    void findByUserIdAndStatusesReturnsDraftListing() {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        CarListing draft = carListingRepository.create(userId, System.currentTimeMillis()).block();
        CarListing published = carListingRepository.create(userId, System.currentTimeMillis() + 1).block();
        carListingRepository.update(published.withStatus(ListingStatus.PUBLISHED, System.currentTimeMillis())).block();

        StepVerifier.create(carListingRepository.findByUserIdAndStatuses(
                        userId, new String[]{"DRAFT"}, 0, 20))
                .assertNext(listing -> {
                    assertThat(listing.id()).isEqualTo(draft.id());
                    assertThat(listing.status()).isEqualTo(ListingStatus.DRAFT);
                })
                .verifyComplete();
    }

    @Test
    void findByUserIdAndStatusesFiltersOutNonMatchingStatus() {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        carListingRepository.create(userId, System.currentTimeMillis()).block();

        StepVerifier.create(carListingRepository.findByUserIdAndStatuses(
                        userId, new String[]{"PUBLISHED"}, 0, 20))
                .verifyComplete();
    }

    @Test
    void findByUserIdAndStatusesWithMultipleStatusesReturnsMatchingListings() {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        long now = System.currentTimeMillis();
        CarListing draft = carListingRepository.create(userId, now).block();
        CarListing published = carListingRepository.create(userId, now + 1).block();
        CarListing archived = carListingRepository.create(userId, now + 2).block();
        long publishedAt = System.currentTimeMillis();
        carListingRepository.update(published.withStatus(ListingStatus.PUBLISHED, publishedAt)).block();
        carListingRepository.update(archived.withStatus(ListingStatus.ARCHIVED, publishedAt)).block();

        StepVerifier.create(carListingRepository.findByUserIdAndStatuses(
                        userId, new String[]{"DRAFT", "PUBLISHED"}, 0, 20))
                .assertNext(listing -> {
                    assertThat(listing.id()).isEqualTo(published.id());
                    assertThat(listing.publishedAt()).isEqualTo(publishedAt);
                })
                .assertNext(listing -> assertThat(listing.id()).isEqualTo(draft.id()))
                .verifyComplete();
    }

    @Test
    void findByUserIdAndStatusesDoesNotReturnOtherUsersListings() {
        long userId1 = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        long userId2 = userService.getUserByPhoneNumberOrCreate("+380123456780").block().id();
        carListingRepository.create(userId1, System.currentTimeMillis()).block();
        CarListing secondUserListing = carListingRepository.create(userId2, System.currentTimeMillis()).block();

        StepVerifier.create(carListingRepository.findByUserIdAndStatuses(
                        userId2, new String[]{"DRAFT"}, 0, 20))
                    .assertNext(listing -> assertThat(listing.id()).isEqualTo(secondUserListing.id()))
                    .verifyComplete();
    }

    @Test
    void findByUserIdAndStatusesPaginatesCorrectly() {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        long now = System.currentTimeMillis();
        CarListing listing1 = carListingRepository.create(userId, now).block();
        CarListing listing2 = carListingRepository.create(userId, now + 1).block();
        CarListing listing3 = carListingRepository.create(userId, now + 2).block();

        StepVerifier.create(carListingRepository.findByUserIdAndStatuses(
                        userId, new String[]{"DRAFT"}, 0, 2))
                    .assertNext(listing -> assertThat(listing.id()).isEqualTo(listing3.id()))
                    .assertNext(listing -> assertThat(listing.id()).isEqualTo(listing2.id()))
                    .verifyComplete();

        StepVerifier.create(carListingRepository.findByUserIdAndStatuses(
                        userId, new String[]{"DRAFT"}, 2, 2))
                    .assertNext(listing -> assertThat(listing.id()).isEqualTo(listing1.id()))
                    .verifyComplete();
    }

    @Test
    void countByUserIdAndStatusesCountsCorrectly() {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        carListingRepository.create(userId, System.currentTimeMillis()).block();
        carListingRepository.create(userId, System.currentTimeMillis()).block();

        StepVerifier.create(carListingRepository.countByUserIdAndStatuses(
                        userId, new String[]{"DRAFT"}))
                .expectNext(2L)
                .verifyComplete();
    }

    @Test
    void countByUserIdAndStatusesReturnsZeroForNonMatchingStatus() {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        carListingRepository.create(userId, System.currentTimeMillis()).block();

        StepVerifier.create(carListingRepository.countByUserIdAndStatuses(
                        userId, new String[]{"PUBLISHED"}))
                .expectNext(0L)
                .verifyComplete();
    }

    @Test
    void deleteByIdRemovesListing() {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        CarListing listing = carListingRepository.create(userId, System.currentTimeMillis()).block();

        carListingRepository.deleteById(listing.id()).block();

        StepVerifier.create(carListingRepository.findById(listing.id()))
                .verifyComplete();
    }

    @Test
    void findByIdReturnsCorrectListing() {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        CarListing created = carListingRepository.create(userId, System.currentTimeMillis()).block();

        StepVerifier.create(carListingRepository.findById(created.id()))
                .assertNext(listing -> {
                    assertThat(listing.id()).isEqualTo(created.id());
                    assertThat(listing.authorUserId()).isEqualTo(userId);
                    assertThat(listing.status()).isEqualTo(ListingStatus.DRAFT);
                    assertThat(listing.createdAt()).isEqualTo(created.createdAt());
                    assertThat(listing.publishedAt()).isEqualTo(0);
                })
                .verifyComplete();
    }

    @Test
    void findByIdReturnsEmptyForNonExistentId() {
        StepVerifier.create(carListingRepository.findById(9999))
                .verifyComplete();
    }

    @Test
    void updatePersistsAllFields() {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        CarListing created = carListingRepository.create(userId, System.currentTimeMillis()).block();

        long updatedAt = System.currentTimeMillis();
        long publishedAt = System.currentTimeMillis() + 1_000;
        var request = new CarListing(
                created.id(), userId, ListingStatus.ARCHIVED,
                "My Car Title", "Great description", CarBrand.TOYOTA, null,
                "Camry", "AA1234BB", CarCondition.USED, 50000, 15000L,
                City.KYIV, CarColor.WHITE, TransmissionType.AUTOMATIC, FuelType.PETROL,
                50.0, DriveType.FWD, BodyType.SEDAN, 2020, 2.5, 1,
                created.createdAt(), updatedAt, publishedAt
        );

        carListingRepository.update(request).block();
        StepVerifier.create(carListingRepository.findById(created.id()))
                .assertNext(listing -> {
                    assertThat(listing.id()).isEqualTo(created.id());
                    assertThat(listing.status()).isEqualTo(ListingStatus.ARCHIVED);
                    assertThat(listing.title()).isEqualTo("My Car Title");
                    assertThat(listing.description()).isEqualTo("Great description");
                    assertThat(listing.brand()).isEqualTo(CarBrand.TOYOTA);
                    assertThat(listing.customBrandName()).isNull();
                    assertThat(listing.model()).isEqualTo("Camry");
                    assertThat(listing.licensePlate()).isEqualTo("AA1234BB");
                    assertThat(listing.condition()).isEqualTo(CarCondition.USED);
                    assertThat(listing.mileage()).isEqualTo(50000);
                    assertThat(listing.price()).isEqualTo(15000L);
                    assertThat(listing.city()).isEqualTo(City.KYIV);
                    assertThat(listing.color()).isEqualTo(CarColor.WHITE);
                    assertThat(listing.transmission()).isEqualTo(TransmissionType.AUTOMATIC);
                    assertThat(listing.fuelType()).isEqualTo(FuelType.PETROL);
                    assertThat(listing.tankVolume()).isEqualTo(50.0);
                    assertThat(listing.driveType()).isEqualTo(DriveType.FWD);
                    assertThat(listing.bodyType()).isEqualTo(BodyType.SEDAN);
                    assertThat(listing.year()).isEqualTo(2020);
                    assertThat(listing.engineVolume()).isEqualTo(2.5);
                    assertThat(listing.ownersCount()).isEqualTo(1);
                    assertThat(listing.updatedAt()).isEqualTo(updatedAt);
                    assertThat(listing.publishedAt()).isEqualTo(publishedAt);
                })
                .verifyComplete();
    }

    @Test
    void findPublishedReturnsOnlyPublishedListings() {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        carListingRepository.create(userId, System.currentTimeMillis()).block(); // draft
        CarListing published = carListingRepository.create(userId, System.currentTimeMillis() + 1).block();
        carListingRepository.update(published.withStatus(ListingStatus.PUBLISHED, System.currentTimeMillis())).block();

        StepVerifier.create(carListingRepository.findPublished(new GetPublishedListingsRequestDTO()))
                .assertNext(dto -> assertThat(dto.id()).isEqualTo(published.id()))
                .verifyComplete();
    }

    @Test
    void findPublishedFiltersOutListingsPublishedAfterAnchor() {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        CarListing listing = carListingRepository.create(userId, System.currentTimeMillis()).block();
        carListingRepository.update(listing.withStatus(ListingStatus.PUBLISHED, System.currentTimeMillis())).block();

        GetPublishedListingsRequestDTO request = new GetPublishedListingsRequestDTO();
        request.setPublishedBefore(System.currentTimeMillis() - 1_000);
        StepVerifier.create(carListingRepository.findPublished(request))
                .verifyComplete();
    }

    @Test
    void findPublishedReturnsCorrectFields() {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        CarListing listing = carListingRepository.create(userId, System.currentTimeMillis()).block();
        carListingRepository.update(new CarListing(
                listing.id(), userId, ListingStatus.PUBLISHED,
                "My Car", "Nice car", CarBrand.TOYOTA, null, "Camry",
                null, null, 100, 300000L, City.KYIV, null, TransmissionType.MANUAL, FuelType.ELECTRIC, null, null, null, 2025, null, null,
                listing.createdAt(), System.currentTimeMillis(), System.currentTimeMillis()
        )).block();

        StepVerifier.create(carListingRepository.findPublished(new GetPublishedListingsRequestDTO()))
                .assertNext(dto -> {
                    assertThat(dto.id()).isEqualTo(listing.id());
                    assertThat(dto.title()).isEqualTo("My Car");
                    assertThat(dto.price()).isEqualTo(300000L);
                    assertThat(dto.mileage()).isEqualTo(100);
                    assertThat(dto.fuelType()).isEqualTo(FuelType.ELECTRIC);
                    assertThat(dto.transmission()).isEqualTo(TransmissionType.MANUAL);
                    assertThat(dto.city()).isEqualTo(City.KYIV);
                    assertThat(dto.year()).isEqualTo(2025);
                })
                .verifyComplete();
    }

    @Test
    void findPublishedPaginatesCorrectly() {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        long now = System.currentTimeMillis();
        CarListing listing1 = carListingRepository.create(userId, now).block();
        CarListing listing2 = carListingRepository.create(userId, now + 1).block();
        CarListing listing3 = carListingRepository.create(userId, now + 2).block();
        carListingRepository.update(listing1.withStatus(ListingStatus.PUBLISHED, System.currentTimeMillis())).block();
        carListingRepository.update(listing3.withStatus(ListingStatus.PUBLISHED, System.currentTimeMillis())).block();
        carListingRepository.update(listing2.withStatus(ListingStatus.PUBLISHED, System.currentTimeMillis())).block();

        GetPublishedListingsRequestDTO request = new GetPublishedListingsRequestDTO();
        request.setOffset(0);
        request.setSize(2);
        StepVerifier.create(carListingRepository.findPublished(request))
                .assertNext(dto -> assertThat(dto.id()).isEqualTo(listing2.id()))
                .assertNext(dto -> assertThat(dto.id()).isEqualTo(listing3.id()))
                .verifyComplete();

        request = new GetPublishedListingsRequestDTO();
        request.setOffset(2);
        request.setSize(2);
        StepVerifier.create(carListingRepository.findPublished(request))
                .assertNext(dto -> assertThat(dto.id()).isEqualTo(listing1.id()))
                .verifyComplete();
    }

    @Test
    void countPublishedCountsOnlyPublishedListings() {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        CarListing listing1 = carListingRepository.create(userId, System.currentTimeMillis()).block();
        carListingRepository.create(userId, System.currentTimeMillis()).block(); // draft
        carListingRepository.update(listing1.withStatus(ListingStatus.PUBLISHED, System.currentTimeMillis())).block();

        StepVerifier.create(carListingRepository.countPublished(new GetPublishedListingsRequestDTO()))
                .expectNext(1L)
                .verifyComplete();
    }

    @Test
    void countPublishedFiltersOutListingsPublishedAfterAnchor() {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        CarListing listing = carListingRepository.create(userId, System.currentTimeMillis()).block();
        carListingRepository.update(listing.withStatus(ListingStatus.PUBLISHED, System.currentTimeMillis())).block();

        GetPublishedListingsRequestDTO request = new GetPublishedListingsRequestDTO();
        request.setPublishedBefore(System.currentTimeMillis() - 1_000);
        StepVerifier.create(carListingRepository.countPublished(request))
                .expectNext(0L)
                .verifyComplete();
    }

    @Test
    void countPublishedReturnsZeroWhenNonePublished() {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        carListingRepository.create(userId, System.currentTimeMillis()).block();

        StepVerifier.create(carListingRepository.countPublished(new GetPublishedListingsRequestDTO()))
                .expectNext(0L)
                .verifyComplete();
    }

    @Test
    void findPublishedByIdReturnsCorrectFields() {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        userService.updateDisplayName(userId, "Test User").block();
        CarListing listing = carListingRepository.create(userId, System.currentTimeMillis()).block();
        long publishedAt = System.currentTimeMillis();
        carListingRepository.update(new CarListing(
                listing.id(), userId, ListingStatus.PUBLISHED,
                "Test Car", "Nice car", CarBrand.TOYOTA, null, "Camry",
                "AA1234BB", CarCondition.USED, 50000, 300000L, City.KYIV,
                CarColor.WHITE, TransmissionType.AUTOMATIC, FuelType.PETROL,
                50.0, DriveType.FWD, BodyType.SEDAN, 2020, 2.5, 1,
                listing.createdAt(), System.currentTimeMillis(), publishedAt
        )).block();

        StepVerifier.create(carListingRepository.findPublishedById(listing.id()))
                .assertNext(dto -> {
                    assertThat(dto.id()).isEqualTo(listing.id());
                    assertThat(dto.authorDisplayName()).isEqualTo("Test User");
                    assertThat(dto.title()).isEqualTo("Test Car");
                    assertThat(dto.description()).isEqualTo("Nice car");
                    assertThat(dto.brand()).isEqualTo(CarBrand.TOYOTA);
                    assertThat(dto.customBrandName()).isNull();
                    assertThat(dto.model()).isEqualTo("Camry");
                    assertThat(dto.licensePlate()).isEqualTo("AA1234BB");
                    assertThat(dto.condition()).isEqualTo(CarCondition.USED);
                    assertThat(dto.mileage()).isEqualTo(50000);
                    assertThat(dto.price()).isEqualTo(300000L);
                    assertThat(dto.city()).isEqualTo(City.KYIV);
                    assertThat(dto.color()).isEqualTo(CarColor.WHITE);
                    assertThat(dto.transmission()).isEqualTo(TransmissionType.AUTOMATIC);
                    assertThat(dto.fuelType()).isEqualTo(FuelType.PETROL);
                    assertThat(dto.tankVolume()).isEqualTo(50.0);
                    assertThat(dto.driveType()).isEqualTo(DriveType.FWD);
                    assertThat(dto.bodyType()).isEqualTo(BodyType.SEDAN);
                    assertThat(dto.year()).isEqualTo(2020);
                    assertThat(dto.engineVolume()).isEqualTo(2.5);
                    assertThat(dto.ownersCount()).isEqualTo(1);
                    assertThat(dto.publishedAt()).isEqualTo(publishedAt);
                })
                .verifyComplete();
    }

    @Test
    void findPublishedByIdReturnsEmptyForDraftListing() {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        CarListing listing = carListingRepository.create(userId, System.currentTimeMillis()).block();

        StepVerifier.create(carListingRepository.findPublishedById(listing.id()))
                .verifyComplete();
    }

    @Test
    void findPublishedByIdReturnsEmptyForNonExistentId() {
        StepVerifier.create(carListingRepository.findPublishedById(9999))
                .verifyComplete();
    }

    @Test
    void findAuthorPhoneByPublishedIdReturnsPhoneNumber() {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        CarListing listing = carListingRepository.create(userId, System.currentTimeMillis()).block();
        carListingRepository.update(listing.withStatus(ListingStatus.PUBLISHED, System.currentTimeMillis())).block();

        StepVerifier.create(carListingRepository.findAuthorPhoneByPublishedId(listing.id()))
                .assertNext(dto -> assertThat(dto.phoneNumber()).isEqualTo("+380123456789"))
                .verifyComplete();
    }

    @Test
    void findAuthorPhoneByPublishedIdReturnsEmptyForDraftListing() {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        CarListing listing = carListingRepository.create(userId, System.currentTimeMillis()).block();

        StepVerifier.create(carListingRepository.findAuthorPhoneByPublishedId(listing.id()))
                .verifyComplete();
    }

    @Test
    void findAuthorPhoneByPublishedIdReturnsEmptyForNonExistentId() {
        StepVerifier.create(carListingRepository.findAuthorPhoneByPublishedId(9999))
                .verifyComplete();
    }

    @Test
    void updateWithNullFieldsClearsPreviousValues() {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        CarListing created = carListingRepository.create(userId, System.currentTimeMillis()).block();

        carListingRepository.update(new CarListing(
                created.id(), userId, ListingStatus.DRAFT,
                "Title", null, CarBrand.TOYOTA, null, "Camry", null,
                null, null, null, null, null, null, null, null, null, null, null, null, null,
                created.createdAt(), System.currentTimeMillis(), created.publishedAt()
        )).block();

        var clearRequest = new CarListing(
                created.id(), userId, ListingStatus.DRAFT,
                null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, null,
                created.createdAt(), System.currentTimeMillis(), created.publishedAt()
        );
        carListingRepository.update(clearRequest).block();
        StepVerifier.create(carListingRepository.findById(created.id()))
                .assertNext(listing -> {
                    assertThat(listing.title()).isNull();
                    assertThat(listing.brand()).isNull();
                    assertThat(listing.model()).isNull();
                })
                .verifyComplete();
    }
}
