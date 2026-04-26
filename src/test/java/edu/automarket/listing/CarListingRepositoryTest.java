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
    void findByUserId() {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        long now = System.currentTimeMillis();
        CarListing draft = carListingRepository.create(userId, now).block();
        CarListing published = carListingRepository.create(userId, now + 1).block();
        CarListing archived = carListingRepository.create(userId, now + 2).block();
        long publishedAt = System.currentTimeMillis();
        carListingRepository.update(published.withStatus(ListingStatus.PUBLISHED, publishedAt)).block();
        carListingRepository.update(archived.withStatus(ListingStatus.ARCHIVED, publishedAt)).block();

        StepVerifier.create(carListingRepository.findByUserId(userId, 0, 20))
                .assertNext(listing -> assertThat(listing.id()).isEqualTo(draft.id()))
                .assertNext(listing -> assertThat(listing.id()).isEqualTo(published.id()))
                .assertNext(listing -> assertThat(listing.id()).isEqualTo(archived.id()))
                .verifyComplete();
    }

    @Test
    void findByUserIdDoesNotReturnOtherUsersListings() {
        long userId1 = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        long userId2 = userService.getUserByPhoneNumberOrCreate("+380123456780").block().id();
        carListingRepository.create(userId1, System.currentTimeMillis()).block();
        CarListing secondUserListing = carListingRepository.create(userId2, System.currentTimeMillis()).block();

        StepVerifier.create(carListingRepository.findByUserId(userId2, 0, 20))
                    .assertNext(listing -> assertThat(listing.id()).isEqualTo(secondUserListing.id()))
                    .verifyComplete();
    }

    @Test
    void findByUserIdPaginatesCorrectly() {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        long now = System.currentTimeMillis();
        CarListing listing1 = carListingRepository.create(userId, now).block();
        CarListing listing2 = carListingRepository.create(userId, now + 1).block();
        CarListing listing3 = carListingRepository.create(userId, now + 2).block();

        StepVerifier.create(carListingRepository.findByUserId(userId, 0, 2))
                    .assertNext(listing -> assertThat(listing.id()).isEqualTo(listing3.id()))
                    .assertNext(listing -> assertThat(listing.id()).isEqualTo(listing2.id()))
                    .verifyComplete();

        StepVerifier.create(carListingRepository.findByUserId(userId, 2, 2))
                    .assertNext(listing -> assertThat(listing.id()).isEqualTo(listing1.id()))
                    .verifyComplete();
    }

    @Test
    void countByUserIdAndStatusesCountsCorrectly() {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        carListingRepository.create(userId, System.currentTimeMillis()).block();
        carListingRepository.create(userId, System.currentTimeMillis()).block();

        StepVerifier.create(carListingRepository.countByUserIdAndStatuses(userId))
                .expectNext(2L)
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
        long promotedUntil = System.currentTimeMillis() + 2_000;
        var request = new CarListing(
                created.id(), userId, ListingStatus.ARCHIVED,
                "My Car Title", "Great description", null, CarBrand.TOYOTA, null,
                "Camry", "AA1234BB", CarCondition.USED, 50000, 15000L,
                City.KYIV, CarColor.WHITE, TransmissionType.AUTOMATIC, FuelType.PETROL,
                50.0, DriveType.FWD, BodyType.SEDAN, 2020, 2.5, 1,
                created.createdAt(), updatedAt, publishedAt, promotedUntil
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
                    assertThat(listing.promotedUntil()).isEqualTo(promotedUntil);
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
                "My Car", "Nice car", null, CarBrand.TOYOTA, null, "Camry",
                null, null, 100, 300000L, City.KYIV, null, TransmissionType.MANUAL, FuelType.ELECTRIC, null, null, null, 2025, null, null,
                listing.createdAt(), System.currentTimeMillis(), System.currentTimeMillis(), 0
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
                "Test Car", "Nice car", null, CarBrand.TOYOTA, null, "Camry",
                "AA1234BB", CarCondition.USED, 50000, 300000L, City.KYIV,
                CarColor.WHITE, TransmissionType.AUTOMATIC, FuelType.PETROL,
                50.0, DriveType.FWD, BodyType.SEDAN, 2020, 2.5, 1,
                listing.createdAt(), System.currentTimeMillis(), publishedAt, 0
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
    void findPublishedByIdReturnsCorrectIsPromotedStatus() {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        long now = System.currentTimeMillis();

        CarListing listing1 = carListingRepository.create(userId, now).block();
        CarListing listing2 = carListingRepository.create(userId, now + 1).block();
        CarListing listing3 = carListingRepository.create(userId, now + 1).block();
        carListingRepository.update(listing1.withStatus(ListingStatus.PUBLISHED, now)).block();
        carListingRepository.update(listing2.withStatus(ListingStatus.PUBLISHED, now)).block();
        carListingRepository.update(listing3.withStatus(ListingStatus.PUBLISHED, now)).block();

        // Promote listing1: set promotedUntil to a future timestamp
        long promotedUntil = now + 7L * 24 * 60 * 60 * 1000;
        carListingRepository.update(new CarListing(
                listing1.id(), userId, ListingStatus.PUBLISHED,
                null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, null,
                now, now, now, promotedUntil
        )).block();
        carListingRepository.update(new CarListing(
                listing2.id(), userId, ListingStatus.PUBLISHED,
                null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, null,
                now, now, now, now - 1_000
        )).block();

        StepVerifier.create(carListingRepository.findPublishedById(listing1.id()))
                .assertNext(dto -> assertThat(dto.isPromoted()).isTrue())
                .verifyComplete();
        StepVerifier.create(carListingRepository.findPublishedById(listing2.id()))
                .assertNext(dto -> assertThat(dto.isPromoted()).isFalse())
                .verifyComplete();
        StepVerifier.create(carListingRepository.findPublishedById(listing3.id()))
                .assertNext(dto -> assertThat(dto.isPromoted()).isFalse())
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

    // Full-text search

    private GetPublishedListingsRequestDTO queryRequest(String query) {
        var req = new GetPublishedListingsRequestDTO();
        req.setQuery(query);
        return req;
    }

    @Test
    void findPublished_searchByTitle_matchesAndExcludes() {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        long now = System.currentTimeMillis();
        CarListing listing = carListingRepository.create(userId, now).block();
        carListingRepository.update(new CarListing(
                listing.id(), userId, ListingStatus.PUBLISHED,
                "Crimson Falcon", null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, null,
                now, now, now, 0
        )).block();

        StepVerifier.create(carListingRepository.findPublished(queryRequest("Crimson")))
                .assertNext(dto -> assertThat(dto.id()).isEqualTo(listing.id()))
                .verifyComplete();

        StepVerifier.create(carListingRepository.findPublished(queryRequest("crim")))
                .assertNext(dto -> assertThat(dto.id()).isEqualTo(listing.id()))
                .verifyComplete();

        StepVerifier.create(carListingRepository.findPublished(queryRequest("Falcon")))
                .assertNext(dto -> assertThat(dto.id()).isEqualTo(listing.id()))
                .verifyComplete();

        StepVerifier.create(carListingRepository.findPublished(queryRequest("Volkswagen")))
                .verifyComplete();
    }

    @Test
    void findPublished_searchByDescription_matchesAndExcludes() {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        long now = System.currentTimeMillis();
        CarListing listing = carListingRepository.create(userId, now).block();
        carListingRepository.update(new CarListing(
                listing.id(), userId, ListingStatus.PUBLISHED,
                null, "Excellent condition, one owner, garage kept", null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, null,
                now, now, now, 0
        )).block();

        StepVerifier.create(carListingRepository.findPublished(queryRequest("garage")))
                .assertNext(dto -> assertThat(dto.id()).isEqualTo(listing.id()))
                .verifyComplete();

        StepVerifier.create(carListingRepository.findPublished(queryRequest("rusty")))
                .verifyComplete();
    }

    @Test
    void findPublished_searchByBrand_matchesLatinAndUkrainian() {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        long now = System.currentTimeMillis();
        CarListing toyota = carListingRepository.create(userId, now).block();
        carListingRepository.update(new CarListing(
                toyota.id(), userId, ListingStatus.PUBLISHED,
                null, null, null, CarBrand.TOYOTA, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, null,
                now, now, now, 0
        )).block();

        CarListing mercedes = carListingRepository.create(userId, now + 1).block();
        carListingRepository.update(new CarListing(
                mercedes.id(), userId, ListingStatus.PUBLISHED,
                null, null, null, CarBrand.MERCEDES_BENZ, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, null,
                now + 1, now + 1, now + 1, 0
        )).block();

        // Latin match
        StepVerifier.create(carListingRepository.findPublished(queryRequest("Toyota")))
                .assertNext(dto -> assertThat(dto.id()).isEqualTo(toyota.id()))
                .verifyComplete();

        // Ukrainian match
        StepVerifier.create(carListingRepository.findPublished(queryRequest("Тойота")))
                .assertNext(dto -> assertThat(dto.id()).isEqualTo(toyota.id()))
                .verifyComplete();

        // Ukrainian match for another brand
        StepVerifier.create(carListingRepository.findPublished(queryRequest("Мерседес")))
                .assertNext(dto -> assertThat(dto.id()).isEqualTo(mercedes.id()))
                .verifyComplete();

        // No match
        StepVerifier.create(carListingRepository.findPublished(queryRequest("Honda")))
                .verifyComplete();
    }

    @Test
    void findPublished_searchByCustomBrandName_matches() {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        long now = System.currentTimeMillis();
        CarListing listing = carListingRepository.create(userId, now).block();
        carListingRepository.update(new CarListing(
                listing.id(), userId, ListingStatus.PUBLISHED,
                null, null, null, CarBrand.CUSTOM, "Zastava", null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, null,
                now, now, now, 0
        )).block();

        StepVerifier.create(carListingRepository.findPublished(queryRequest("Zastava")))
                .assertNext(dto -> assertThat(dto.id()).isEqualTo(listing.id()))
                .verifyComplete();

        StepVerifier.create(carListingRepository.findPublished(queryRequest("Toyota")))
                .verifyComplete();
    }

    @Test
    void findPublished_searchByModel_matches() {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        long now = System.currentTimeMillis();
        CarListing listing = carListingRepository.create(userId, now).block();
        carListingRepository.update(new CarListing(
                listing.id(), userId, ListingStatus.PUBLISHED,
                null, null, null, CarBrand.BMW, null, "X5", null,
                null, null, null, null, null, null, null, null, null, null, null, null, null,
                now, now, now, 0
        )).block();

        StepVerifier.create(carListingRepository.findPublished(queryRequest("X5")))
                .assertNext(dto -> assertThat(dto.id()).isEqualTo(listing.id()))
                .verifyComplete();

        StepVerifier.create(carListingRepository.findPublished(queryRequest("X3")))
                .verifyComplete();
    }

    @Test
    void findPublished_searchByCondition_matchesUkrainian() {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        long now = System.currentTimeMillis();

        CarListing newCar = carListingRepository.create(userId, now).block();
        carListingRepository.update(new CarListing(
                newCar.id(), userId, ListingStatus.PUBLISHED,
                null, null, null, null, null, null, null,
                CarCondition.NEW, null, null, null, null, null, null, null, null, null, null, null, null,
                now, now, now, 0
        )).block();

        CarListing usedCar = carListingRepository.create(userId, now + 1).block();
        carListingRepository.update(new CarListing(
                usedCar.id(), userId, ListingStatus.PUBLISHED,
                null, null, null, null, null, null, null,
                CarCondition.USED, null, null, null, null, null, null, null, null, null, null, null, null,
                now + 1, now + 1, now + 1, 0
        )).block();

        StepVerifier.create(carListingRepository.findPublished(queryRequest("новий")))
                .assertNext(dto -> assertThat(dto.id()).isEqualTo(newCar.id()))
                .verifyComplete();

        StepVerifier.create(carListingRepository.findPublished(queryRequest("вживаний")))
                .assertNext(dto -> assertThat(dto.id()).isEqualTo(usedCar.id()))
                .verifyComplete();

        // "б/у" is another alias for USED
        StepVerifier.create(carListingRepository.findPublished(queryRequest("б/у")))
                .assertNext(dto -> assertThat(dto.id()).isEqualTo(usedCar.id()))
                .verifyComplete();
    }

    @Test
    void findPublished_searchByCity_matchesUkrainian() {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        long now = System.currentTimeMillis();

        CarListing kyivListing = carListingRepository.create(userId, now).block();
        carListingRepository.update(new CarListing(
                kyivListing.id(), userId, ListingStatus.PUBLISHED,
                null, null, null, null, null, null, null,
                null, null, null, City.KYIV, null, null, null, null, null, null, null, null, null,
                now, now, now, 0
        )).block();

        CarListing kharkivListing = carListingRepository.create(userId, now + 1).block();
        carListingRepository.update(new CarListing(
                kharkivListing.id(), userId, ListingStatus.PUBLISHED,
                null, null, null, null, null, null, null,
                null, null, null, City.KHARKIV, null, null, null, null, null, null, null, null, null,
                now + 1, now + 1, now + 1, 0
        )).block();

        StepVerifier.create(carListingRepository.findPublished(queryRequest("Київ")))
                .assertNext(dto -> assertThat(dto.id()).isEqualTo(kyivListing.id()))
                .verifyComplete();

        StepVerifier.create(carListingRepository.findPublished(queryRequest("Харків")))
                .assertNext(dto -> assertThat(dto.id()).isEqualTo(kharkivListing.id()))
                .verifyComplete();

        StepVerifier.create(carListingRepository.findPublished(queryRequest("Одеса")))
                .verifyComplete();
    }

    @Test
    void findPublished_searchByColor_matchesUkrainian() {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        long now = System.currentTimeMillis();

        CarListing whiteCar = carListingRepository.create(userId, now).block();
        carListingRepository.update(new CarListing(
                whiteCar.id(), userId, ListingStatus.PUBLISHED,
                null, null, null, null, null, null, null,
                null, null, null, null, CarColor.WHITE, null, null, null, null, null, null, null, null,
                now, now, now, 0
        )).block();

        CarListing blackCar = carListingRepository.create(userId, now + 1).block();
        carListingRepository.update(new CarListing(
                blackCar.id(), userId, ListingStatus.PUBLISHED,
                null, null, null, null, null, null, null,
                null, null, null, null, CarColor.BLACK, null, null, null, null, null, null, null, null,
                now + 1, now + 1, now + 1, 0
        )).block();

        StepVerifier.create(carListingRepository.findPublished(queryRequest("білий")))
                .assertNext(dto -> assertThat(dto.id()).isEqualTo(whiteCar.id()))
                .verifyComplete();

        StepVerifier.create(carListingRepository.findPublished(queryRequest("чорний")))
                .assertNext(dto -> assertThat(dto.id()).isEqualTo(blackCar.id()))
                .verifyComplete();
    }

    @Test
    void findPublished_searchByTransmission_matchesLatinAndUkrainian() {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        long now = System.currentTimeMillis();

        CarListing automaticCar = carListingRepository.create(userId, now).block();
        carListingRepository.update(new CarListing(
                automaticCar.id(), userId, ListingStatus.PUBLISHED,
                null, null, null, null, null, null, null,
                null, null, null, null, null, TransmissionType.AUTOMATIC, null, null, null, null, null, null, null,
                now, now, now, 0
        )).block();

        CarListing manualCar = carListingRepository.create(userId, now + 1).block();
        carListingRepository.update(new CarListing(
                manualCar.id(), userId, ListingStatus.PUBLISHED,
                null, null, null, null, null, null, null,
                null, null, null, null, null, TransmissionType.MANUAL, null, null, null, null, null, null, null,
                now + 1, now + 1, now + 1, 0
        )).block();

        // Ukrainian
        StepVerifier.create(carListingRepository.findPublished(queryRequest("автомат")))
                .assertNext(dto -> assertThat(dto.id()).isEqualTo(automaticCar.id()))
                .verifyComplete();

        // Latin abbreviation
        StepVerifier.create(carListingRepository.findPublished(queryRequest("АКПП")))
                .assertNext(dto -> assertThat(dto.id()).isEqualTo(automaticCar.id()))
                .verifyComplete();

        StepVerifier.create(carListingRepository.findPublished(queryRequest("механіка")))
                .assertNext(dto -> assertThat(dto.id()).isEqualTo(manualCar.id()))
                .verifyComplete();

        StepVerifier.create(carListingRepository.findPublished(queryRequest("МКПП")))
                .assertNext(dto -> assertThat(dto.id()).isEqualTo(manualCar.id()))
                .verifyComplete();
    }

    @Test
    void findPublished_searchByFuelType_matchesUkrainian() {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        long now = System.currentTimeMillis();

        CarListing petrolCar = carListingRepository.create(userId, now).block();
        carListingRepository.update(new CarListing(
                petrolCar.id(), userId, ListingStatus.PUBLISHED,
                null, null, null, null, null, null, null,
                null, null, null, null, null, null, FuelType.PETROL, null, null, null, null, null, null,
                now, now, now, 0
        )).block();

        CarListing dieselCar = carListingRepository.create(userId, now + 1).block();
        carListingRepository.update(new CarListing(
                dieselCar.id(), userId, ListingStatus.PUBLISHED,
                null, null, null, null, null, null, null,
                null, null, null, null, null, null, FuelType.DIESEL, null, null, null, null, null, null,
                now + 1, now + 1, now + 1, 0
        )).block();

        CarListing electricCar = carListingRepository.create(userId, now + 2).block();
        carListingRepository.update(new CarListing(
                electricCar.id(), userId, ListingStatus.PUBLISHED,
                null, null, null, null, null, null, null,
                null, null, null, null, null, null, FuelType.ELECTRIC, null, null, null, null, null, null,
                now + 2, now + 2, now + 2, 0
        )).block();

        StepVerifier.create(carListingRepository.findPublished(queryRequest("бензин")))
                .assertNext(dto -> assertThat(dto.id()).isEqualTo(petrolCar.id()))
                .verifyComplete();

        StepVerifier.create(carListingRepository.findPublished(queryRequest("дизель")))
                .assertNext(dto -> assertThat(dto.id()).isEqualTo(dieselCar.id()))
                .verifyComplete();

        StepVerifier.create(carListingRepository.findPublished(queryRequest("електромобіль")))
                .assertNext(dto -> assertThat(dto.id()).isEqualTo(electricCar.id()))
                .verifyComplete();
    }

    @Test
    void findPublished_searchByDriveType_matchesLatinAndUkrainian() {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        long now = System.currentTimeMillis();

        CarListing fwdCar = carListingRepository.create(userId, now).block();
        carListingRepository.update(new CarListing(
                fwdCar.id(), userId, ListingStatus.PUBLISHED,
                null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, DriveType.FWD, null, null, null, null,
                now, now, now, 0
        )).block();

        CarListing awdCar = carListingRepository.create(userId, now + 1).block();
        carListingRepository.update(new CarListing(
                awdCar.id(), userId, ListingStatus.PUBLISHED,
                null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, DriveType.AWD, null, null, null, null,
                now + 1, now + 1, now + 1, 0
        )).block();

        // Ukrainian
        StepVerifier.create(carListingRepository.findPublished(queryRequest("передній")))
                .assertNext(dto -> assertThat(dto.id()).isEqualTo(fwdCar.id()))
                .verifyComplete();

        // Latin abbreviation
        StepVerifier.create(carListingRepository.findPublished(queryRequest("FWD")))
                .assertNext(dto -> assertThat(dto.id()).isEqualTo(fwdCar.id()))
                .verifyComplete();

        StepVerifier.create(carListingRepository.findPublished(queryRequest("AWD")))
                .assertNext(dto -> assertThat(dto.id()).isEqualTo(awdCar.id()))
                .verifyComplete();
    }

    @Test
    void findPublished_searchByBodyType_matchesLatinAndUkrainian() {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        long now = System.currentTimeMillis();

        CarListing sedanCar = carListingRepository.create(userId, now).block();
        carListingRepository.update(new CarListing(
                sedanCar.id(), userId, ListingStatus.PUBLISHED,
                null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, BodyType.SEDAN, null, null, null,
                now, now, now, 0
        )).block();

        CarListing suvCar = carListingRepository.create(userId, now + 1).block();
        carListingRepository.update(new CarListing(
                suvCar.id(), userId, ListingStatus.PUBLISHED,
                null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, BodyType.SUV, null, null, null,
                now + 1, now + 1, now + 1, 0
        )).block();

        StepVerifier.create(carListingRepository.findPublished(queryRequest("седан")))
                .assertNext(dto -> assertThat(dto.id()).isEqualTo(sedanCar.id()))
                .verifyComplete();

        // Ukrainian and Latin abbreviation both in translation
        StepVerifier.create(carListingRepository.findPublished(queryRequest("позашляховик")))
                .assertNext(dto -> assertThat(dto.id()).isEqualTo(suvCar.id()))
                .verifyComplete();

        StepVerifier.create(carListingRepository.findPublished(queryRequest("SUV")))
                .assertNext(dto -> assertThat(dto.id()).isEqualTo(suvCar.id()))
                .verifyComplete();
    }

    @Test
    void findPublished_searchRanksTitleMatchHigherThanDescriptionMatch() {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        long now = System.currentTimeMillis();

        // Published earlier — without ranking would appear second (ORDER BY published_at DESC)
        CarListing titleMatch = carListingRepository.create(userId, now).block();
        carListingRepository.update(new CarListing(
                titleMatch.id(), userId, ListingStatus.PUBLISHED,
                "Porsche 911", null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, null,
                now, now, now, 0
        )).block();

        // Published later — without ranking would appear first (ORDER BY published_at DESC)
        CarListing descMatch = carListingRepository.create(userId, now + 1).block();
        carListingRepository.update(new CarListing(
                descMatch.id(), userId, ListingStatus.PUBLISHED,
                "Sports car for sale", "This Porsche 911 replica is a great deal", null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, null,
                now + 1, now + 1, now + 1, 0
        )).block();

        // With ranking, the title match (weight A) must outrank the description match (weight D)
        StepVerifier.create(carListingRepository.findPublished(queryRequest("Porsche")))
                .assertNext(dto -> assertThat(dto.id()).isEqualTo(titleMatch.id()))
                .assertNext(dto -> assertThat(dto.id()).isEqualTo(descMatch.id()))
                .verifyComplete();
    }

    @Test
    void findPublished_promotedListingsReturnedFirst() {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        long now = System.currentTimeMillis();

        // listing1 published first, listing2 published later — without promotion, listing2 would come first
        CarListing listing1 = carListingRepository.create(userId, now).block();
        CarListing listing2 = carListingRepository.create(userId, now + 1).block();
        carListingRepository.update(listing1.withStatus(ListingStatus.PUBLISHED, now)).block();
        carListingRepository.update(listing2.withStatus(ListingStatus.PUBLISHED, now + 1)).block();

        StepVerifier.create(carListingRepository.findPublished(new GetPublishedListingsRequestDTO()))
                .assertNext(dto -> {
                    assertThat(dto.id()).isEqualTo(listing2.id());
                    assertThat(dto.isPromoted()).isFalse();
                })
                .assertNext(dto -> {
                    assertThat(dto.id()).isEqualTo(listing1.id());
                    assertThat(dto.isPromoted()).isFalse();
                })
                .verifyComplete();

        // Promote listing1: set promotedUntil to a future timestamp
        long promotedUntil = now + 7L * 24 * 60 * 60 * 1000;
        carListingRepository.update(new CarListing(
                listing1.id(), userId, ListingStatus.PUBLISHED,
                null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, null,
                now, now, now, promotedUntil
        )).block();

        StepVerifier.create(carListingRepository.findPublished(new GetPublishedListingsRequestDTO()))
                .assertNext(dto -> {
                    assertThat(dto.id()).isEqualTo(listing1.id());
                    assertThat(dto.isPromoted()).isTrue();
                })
                .assertNext(dto -> {
                    assertThat(dto.id()).isEqualTo(listing2.id());
                    assertThat(dto.isPromoted()).isFalse();
                })
                .verifyComplete();
    }

    @Test
    void findPublished_expiredPromotionNotBoosted() {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        long now = System.currentTimeMillis();

        CarListing listing1 = carListingRepository.create(userId, now).block();
        CarListing listing2 = carListingRepository.create(userId, now + 1).block();
        carListingRepository.update(listing1.withStatus(ListingStatus.PUBLISHED, now)).block();
        carListingRepository.update(listing2.withStatus(ListingStatus.PUBLISHED, now + 1)).block();

        // Promote listing1 with an already-expired timestamp
        long expiredPromotedUntil = now - 1_000;
        carListingRepository.update(new CarListing(
                listing1.id(), userId, ListingStatus.PUBLISHED,
                null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, null,
                now, now, now, expiredPromotedUntil
        )).block();

        // listing2 published later, so it still comes first (no boost for expired promotion)
        StepVerifier.create(carListingRepository.findPublished(new GetPublishedListingsRequestDTO()))
                .assertNext(dto -> assertThat(dto.id()).isEqualTo(listing2.id()))
                .assertNext(dto -> assertThat(dto.id()).isEqualTo(listing1.id()))
                .verifyComplete();
    }

    @Test
    void findPublished_promotedListingsReturnedFirstInSearchResults() {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        long now = System.currentTimeMillis();

        // listing1 published first (would rank second by published_at), listing2 published later
        CarListing listing1 = carListingRepository.create(userId, now).block();
        CarListing listing2 = carListingRepository.create(userId, now + 1).block();
        carListingRepository.update(new CarListing(
                listing1.id(), userId, ListingStatus.PUBLISHED,
                "Toyota Camry", null, null, CarBrand.TOYOTA, null, "Camry", null,
                null, null, null, null, null, null, null, null, null, null, null, null, null,
                now, now, now, now + 7L * 24 * 60 * 60 * 1000
        )).block();
        carListingRepository.update(new CarListing(
                listing2.id(), userId, ListingStatus.PUBLISHED,
                "Toyota Corolla", null, null, CarBrand.TOYOTA, null, "Corolla", null,
                null, null, null, null, null, null, null, null, null, null, null, null, null,
                now + 1, now + 1, now + 1, 0
        )).block();

        StepVerifier.create(carListingRepository.findPublished(queryRequest("Toyota")))
                .assertNext(dto -> assertThat(dto.id()).isEqualTo(listing1.id()))
                .assertNext(dto -> assertThat(dto.id()).isEqualTo(listing2.id()))
                .verifyComplete();
    }

    @Test
    void findPublished_returnsCorrectPromotionStatus() {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        long now = System.currentTimeMillis();

        // listing1 published first, listing2 published later — without promotion, listing2 would come first
        CarListing listing1 = carListingRepository.create(userId, now).block();
        CarListing listing2 = carListingRepository.create(userId, now + 1).block();
        CarListing listing3 = carListingRepository.create(userId, now + 2).block();
        carListingRepository.update(listing1.withStatus(ListingStatus.PUBLISHED, now)).block();
        carListingRepository.update(listing2.withStatus(ListingStatus.PUBLISHED, now + 1)).block();
        carListingRepository.update(listing3.withStatus(ListingStatus.PUBLISHED, now + 2)).block();

        long promotedUntil = now + 7L * 24 * 60 * 60 * 1000;
        carListingRepository.update(new CarListing(
                listing1.id(), userId, ListingStatus.PUBLISHED,
                null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, null,
                now, now, now, promotedUntil
        )).block();
        carListingRepository.update(new CarListing(
                listing2.id(), userId, ListingStatus.PUBLISHED,
                null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, null,
                now, now, now, now - 1_000
        )).block();

        StepVerifier.create(carListingRepository.findPublished(new GetPublishedListingsRequestDTO()))
                .assertNext(dto -> {
                    assertThat(dto.id()).isEqualTo(listing1.id());
                    assertThat(dto.isPromoted()).isTrue();
                })
                .assertNext(dto -> {
                    assertThat(dto.id()).isEqualTo(listing3.id());
                    assertThat(dto.isPromoted()).isFalse();
                })
                .assertNext(dto -> {
                    assertThat(dto.id()).isEqualTo(listing2.id());
                    assertThat(dto.isPromoted()).isFalse();
                })
                .verifyComplete();
    }

    @Test
    void updateWithNullFieldsClearsPreviousValues() {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        CarListing created = carListingRepository.create(userId, System.currentTimeMillis()).block();

        carListingRepository.update(new CarListing(
                created.id(), userId, ListingStatus.DRAFT,
                "Title", null, null, CarBrand.TOYOTA, null, "Camry", null,
                null, null, null, null, null, null, null, null, null, null, null, null, null,
                created.createdAt(), System.currentTimeMillis(), created.publishedAt(), 0
        )).block();

        var clearRequest = new CarListing(
                created.id(), userId, ListingStatus.DRAFT,
                null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, null,
                created.createdAt(), System.currentTimeMillis(), created.publishedAt(), 0
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
