package edu.automarket.listing;

import edu.automarket.AbstractIntegrationTest;
import edu.automarket.common.ApiException;
import edu.automarket.listing.dto.GetPublishedListingsRequestDTO;
import edu.automarket.listing.dto.OwnCarListingListItemDTO;
import edu.automarket.listing.dto.PublicCarListingItemDTO;
import edu.automarket.listing.dto.UpdateCarListingRequestDTO;
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
import org.springframework.http.HttpStatus;
import reactor.test.StepVerifier;

import static edu.automarket.listing.CarListingTestUtils.UPDATE_CAR_LISTING_REQUEST_DTO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.data.Offset.offset;

class CarListingServiceTest extends AbstractIntegrationTest {

    @Autowired
    private CarListingService carListingService;

    @Autowired
    private UserService userService;

    @Test
    void createReturnsDraftListingWithCorrectUserId() {
        long createdAt = System.currentTimeMillis();
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();

        StepVerifier.create(carListingService.create(userId))
                .assertNext(listing -> {
                    assertThat(listing.id()).isGreaterThan(0);
                    assertThat(listing.authorUserId()).isEqualTo(userId);
                    assertThat(listing.status()).isEqualTo(ListingStatus.DRAFT);
                    assertThat(listing.createdAt()).isCloseTo(createdAt, offset(1000L));
                    assertThat(listing.updatedAt()).isEqualTo(listing.createdAt());
                })
                .verifyComplete();
    }

    @Test
    void createIsBeingLimittedByUser() {
        long userId1 = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        long userId2 = userService.getUserByPhoneNumberOrCreate("+380123456780").block().id();

        for (int i = 0; i < 30; i++) {
            carListingService.create(userId1).block();
        }

        assertThatThrownBy(() -> carListingService.create(userId1).block())
                .isInstanceOf(ApiException.class)
                .hasMessage("You have reached the limit of listings per user");

        CarListing user2Listing = carListingService.create(userId2).block();
        user2Listing = carListingService.getListingByIdOrThrow(user2Listing.id()).block();
        assertThat(user2Listing).isNotNull();
        assertThat(user2Listing.id()).isGreaterThan(0);
        assertThat(user2Listing.authorUserId()).isEqualTo(userId2);
        assertThat(user2Listing.status()).isEqualTo(ListingStatus.DRAFT);
    }

    @Test
    void getOwnListingsReturnsTotalElementsAndContent() {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        CarListing listing1 = carListingService.create(userId).block();
        CarListing listing2 = carListingService.create(userId).block();

        StepVerifier.create(carListingService.getOwnListings(userId, 0, 20))
                .assertNext(page -> {
                    assertThat(page.totalElements()).isEqualTo(2);
                    assertThat(page.content()).hasSize(2);
                    assertThat(page.content().get(0).id()).isEqualTo(listing2.id());
                    assertThat(page.content().get(1).id()).isEqualTo(listing1.id());
                })
                .verifyComplete();
    }

    @Test
    void getOwnListingsWithNullStatusesReturnsAllListings() {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        carListingService.create(userId).block();

        StepVerifier.create(carListingService.getOwnListings(userId, 0, 20))
                .assertNext(page -> assertThat(page.totalElements()).isEqualTo(1))
                .verifyComplete();
    }

    @Test
    void getOwnListingsPaginatesCorrectly() {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        CarListing listing1 = carListingService.create(userId).block();
        CarListing listing2 = carListingService.create(userId).block();
        CarListing listing3 = carListingService.create(userId).block();

        StepVerifier.create(carListingService.getOwnListings(userId, 0, 2))
                .assertNext(page -> {
                    assertThat(page.totalElements()).isEqualTo(3);
                    assertThat(page.content()).hasSize(2);
                    assertThat(page.content().get(0).id()).isEqualTo(listing3.id());
                    assertThat(page.content().get(1).id()).isEqualTo(listing2.id());
                })
                .verifyComplete();

        StepVerifier.create(carListingService.getOwnListings(userId, 2, 2))
                .assertNext(page -> {
                    assertThat(page.totalElements()).isEqualTo(3);
                    assertThat(page.content()).hasSize(1);
                    assertThat(page.content().get(0).id()).isEqualTo(listing1.id());
                })
                .verifyComplete();
    }

    @Test
    void getOwnListingsCount() {
        long userId1 = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        long userId2 = userService.getUserByPhoneNumberOrCreate("+380987654321").block().id();
        carListingService.create(userId1).block();
        carListingService.create(userId2).block();
        carListingService.create(userId1).block();

        StepVerifier.create(carListingService.getOwnListingsCount(userId1))
                .assertNext(count -> assertThat(count).isEqualTo(2))
                .verifyComplete();

        carListingService.create(userId1).block();

        StepVerifier.create(carListingService.getOwnListingsCount(userId1))
                .assertNext(count -> assertThat(count).isEqualTo(3))
                .verifyComplete();
    }

    @Test
    void deleteRemovesListing() {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        CarListing listing = carListingService.create(userId).block();

        carListingService.delete(listing.id()).block();

        StepVerifier.create(carListingService.getListingByIdOrThrow(listing.id()))
                .expectErrorMatches(e -> e instanceof ApiException ex
                        && ex.getStatus() == HttpStatus.NOT_FOUND)
                .verify();
    }

    @Test
    void updateStatusPersistsNewStatus() {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        CarListing created = carListingService.create(userId).block();

        carListingService.updateStatus(created, ListingStatus.ARCHIVED).block();

        StepVerifier.create(carListingService.getListingByIdOrThrow(created.id()))
                .assertNext(listing -> {
                    assertThat(listing.status()).isEqualTo(ListingStatus.ARCHIVED);
                    assertThat(listing.publishedAt()).isEqualTo(0);
                })
                .verifyComplete();
    }

    @Test
    void updateStatusSetPublishedAt() {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        CarListing created = carListingService.create(userId).block();
        created = carListingService.update(created, UPDATE_CAR_LISTING_REQUEST_DTO).block();

        carListingService.updateStatus(created, ListingStatus.PUBLISHED).block();
        long publishedAt = System.currentTimeMillis();

        StepVerifier.create(carListingService.getListingByIdOrThrow(created.id()))
                    .assertNext(listing -> {
                        assertThat(listing.status()).isEqualTo(ListingStatus.PUBLISHED);
                        assertThat(listing.publishedAt()).isCloseTo(publishedAt, offset(1_000L));
                    })
                    .verifyComplete();
    }

    @Test
    void updateStatusPreservesPublishedAtWhenArchiving() {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        CarListing created = carListingService.create(userId).block();
        created = carListingService.update(created, UPDATE_CAR_LISTING_REQUEST_DTO).block();

        carListingService.updateStatus(created, ListingStatus.PUBLISHED).block();
        CarListing published = carListingService.getListingByIdOrThrow(created.id()).block();

        carListingService.updateStatus(published, ListingStatus.ARCHIVED).block();

        StepVerifier.create(carListingService.getListingByIdOrThrow(created.id()))
                .assertNext(listing -> {
                    assertThat(listing.status()).isEqualTo(ListingStatus.ARCHIVED);
                    assertThat(listing.publishedAt()).isEqualTo(published.publishedAt());
                })
                .verifyComplete();
    }

    @Test
    void updateStatusCooldownOnRepublish() throws InterruptedException {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        CarListing listing = carListingService.create(userId).block();
        listing = carListingService.update(listing, UPDATE_CAR_LISTING_REQUEST_DTO).block();

        carListingService.updateStatus(listing, ListingStatus.PUBLISHED).block();
        CarListing publishedListing = carListingService.getListingByIdOrThrow(listing.id()).block();
        assertThat(publishedListing).isNotNull();
        assertThat(publishedListing.status()).isEqualTo(ListingStatus.PUBLISHED);

        carListingService.updateStatus(publishedListing, ListingStatus.ARCHIVED).block();
        CarListing archivedListing = carListingService.getListingByIdOrThrow(listing.id()).block();
        assertThat(archivedListing).isNotNull();
        assertThat(archivedListing.status()).isEqualTo(ListingStatus.ARCHIVED);

        assertThatThrownBy(() -> carListingService.updateStatus(archivedListing, ListingStatus.PUBLISHED).block())
                .isInstanceOf(ApiException.class)
                .hasMessage("Listing publishing cooldown in progress");

        listing = carListingService.getListingByIdOrThrow(listing.id()).block();
        assertThat(listing).isNotNull();
        assertThat(listing.status()).isEqualTo(ListingStatus.ARCHIVED);

        Thread.sleep(2_000); // wait for the cooldown

        carListingService.updateStatus(archivedListing, ListingStatus.PUBLISHED).block();
        publishedListing = carListingService.getListingByIdOrThrow(listing.id()).block();
        assertThat(publishedListing).isNotNull();
        assertThat(publishedListing.status()).isEqualTo(ListingStatus.PUBLISHED);
    }

    @Test
    void updateStatusToPublishedFailsWhenRequiredFieldsAreMissing() {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        CarListing listing = carListingService.create(userId).block();

        assertThatThrownBy(() -> carListingService.updateStatus(listing, ListingStatus.PUBLISHED).block())
                .isInstanceOf(ApiException.class)
                .hasMessage("Listing field is required for publishing: title");

        StepVerifier.create(carListingService.getListingByIdOrThrow(listing.id()))
                .assertNext(updated -> {
                    assertThat(updated.status()).isEqualTo(ListingStatus.DRAFT);
                    assertThat(updated.publishedAt()).isEqualTo(0);
                })
                .verifyComplete();
    }

    @Test
    void getPublishedListingByIdOrThrowReturnsPublishedListing() {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        CarListing listing = carListingService.create(userId).block();
        listing = carListingService.update(listing, UPDATE_CAR_LISTING_REQUEST_DTO).block();
        carListingService.updateStatus(listing, ListingStatus.PUBLISHED).block();

        long listingId = listing.id();

        StepVerifier.create(carListingService.getPublishedListingByIdOrThrow(listing.id()))
                .assertNext(dto -> assertThat(dto.id()).isEqualTo(listingId))
                .verifyComplete();
    }

    @Test
    void getPublishedListingByIdOrThrowThrowsNotFoundForDraftListing() {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        CarListing listing = carListingService.create(userId).block();

        StepVerifier.create(carListingService.getPublishedListingByIdOrThrow(listing.id()))
                .expectErrorMatches(e -> e instanceof ApiException ex
                        && ex.getStatus() == HttpStatus.NOT_FOUND)
                .verify();
    }

    @Test
    void getPublishedListingByIdOrThrowThrowsNotFoundForNonExistentId() {
        StepVerifier.create(carListingService.getPublishedListingByIdOrThrow(9999))
                .expectErrorMatches(e -> e instanceof ApiException ex
                        && ex.getStatus() == HttpStatus.NOT_FOUND)
                .verify();
    }

    @Test
    void getListingByIdOrThrowReturnsListing() {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        CarListing created = carListingService.create(userId).block();

        StepVerifier.create(carListingService.getListingByIdOrThrow(created.id()))
                .assertNext(listing -> assertThat(listing.id()).isEqualTo(created.id()))
                .verifyComplete();
    }

    @Test
    void getListingByIdOrThrowThrowsNotFoundForMissingId() {
        StepVerifier.create(carListingService.getListingByIdOrThrow(9999))
                .expectErrorMatches(e -> e instanceof ApiException ex
                        && ex.getStatus() == HttpStatus.NOT_FOUND)
                .verify();
    }

    @Test
    void getPublishedListingsReturnsOnlyPublishedListings() {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        carListingService.create(userId).block(); // draft
        CarListing published = carListingService.create(userId).block();
        published = carListingService.update(published, UPDATE_CAR_LISTING_REQUEST_DTO).block();
        carListingService.updateStatus(published, ListingStatus.PUBLISHED).block();

        long publishedListingId = published.id();

        StepVerifier.create(carListingService.getPublishedListings(new GetPublishedListingsRequestDTO()))
                    .assertNext(page -> {
                    assertThat(page.totalElements()).isEqualTo(1);
                    assertThat(page.content()).hasSize(1);
                    assertThat(page.content().get(0).id()).isEqualTo(publishedListingId);
                })
                    .verifyComplete();
    }

    @Test
    void getPublishedListingsFiltersOutListingsPublishedAfterAnchor() {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        CarListing listing = carListingService.create(userId).block();
        listing = carListingService.update(listing, UPDATE_CAR_LISTING_REQUEST_DTO).block();
        carListingService.updateStatus(listing, ListingStatus.PUBLISHED).block();

        GetPublishedListingsRequestDTO request = new GetPublishedListingsRequestDTO();
        request.setPublishedBefore(System.currentTimeMillis() - 1_000);
        StepVerifier.create(carListingService.getPublishedListings(request))
                    .assertNext(page -> {
                        assertThat(page.totalElements()).isEqualTo(0);
                        assertThat(page.content()).isEmpty();
                    })
                    .verifyComplete();
    }

    @Test
    void getPublishedListingsReturnsCorrectFields() {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        CarListing listing = carListingService.create(userId).block();
        CarListing updatedListing = carListingService.update(listing, new UpdateCarListingRequestDTO(
                "Sport Car", "Fast and furious", null, CarBrand.CUSTOM, "Batmobile", "Dark Knight",
                "AA2222BB", CarCondition.NEW, 1234, 999999L,
                City.KYIV, CarColor.RED, TransmissionType.MANUAL, FuelType.ELECTRIC, 25.4,
                DriveType.FWD, BodyType.SEDAN, 2024, 34.1, 4
        )).block();
        carListingService.updateStatus(updatedListing, ListingStatus.PUBLISHED).block();

        StepVerifier.create(carListingService.getPublishedListings(new GetPublishedListingsRequestDTO()))
                .assertNext(page -> {
                    assertThat(page.totalElements()).isEqualTo(1);
                    PublicCarListingItemDTO dto = page.content().get(0);
                    assertThat(dto.id()).isEqualTo(listing.id());
                    assertThat(dto.title()).isEqualTo("Sport Car");
                    assertThat(dto.price()).isEqualTo(999999L);
                    assertThat(dto.mileage()).isEqualTo(1234);
                    assertThat(dto.fuelType()).isEqualTo(FuelType.ELECTRIC);
                    assertThat(dto.transmission()).isEqualTo(TransmissionType.MANUAL);
                    assertThat(dto.city()).isEqualTo(City.KYIV);
                    assertThat(dto.year()).isEqualTo(2024);
                })
                .verifyComplete();
    }

    @Test
    void getPublishedListingsReturnsEmptyWhenNonePublished() {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        carListingService.create(userId).block();

        StepVerifier.create(carListingService.getPublishedListings(new GetPublishedListingsRequestDTO()))
                .assertNext(page -> {
                    assertThat(page.totalElements()).isEqualTo(0);
                    assertThat(page.content()).isEmpty();
                })
                .verifyComplete();
    }

    @Test
    void getPublishedListingsPaginatesCorrectly() {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        CarListing listing1 = carListingService.create(userId).block();
        CarListing listing2 = carListingService.create(userId).block();
        CarListing listing3 = carListingService.create(userId).block();
        listing1 = carListingService.update(listing1, UPDATE_CAR_LISTING_REQUEST_DTO).block();
        listing2 = carListingService.update(listing2, UPDATE_CAR_LISTING_REQUEST_DTO).block();
        listing3 = carListingService.update(listing3, UPDATE_CAR_LISTING_REQUEST_DTO).block();
        carListingService.updateStatus(listing1, ListingStatus.PUBLISHED).block();
        carListingService.updateStatus(listing3, ListingStatus.PUBLISHED).block();
        carListingService.updateStatus(listing2, ListingStatus.PUBLISHED).block();

        long listing1Id = listing1.id();
        long listing2Id = listing2.id();
        long listing3Id = listing3.id();

        GetPublishedListingsRequestDTO request = new GetPublishedListingsRequestDTO();
        request.setOffset(0);
        request.setSize(2);
        StepVerifier.create(carListingService.getPublishedListings(request))
                    .assertNext(page -> {
                        assertThat(page.totalElements()).isEqualTo(3);
                        assertThat(page.content()).hasSize(2);
                        assertThat(page.content().get(0).id()).isEqualTo(listing2Id);
                        assertThat(page.content().get(1).id()).isEqualTo(listing3Id);
                    })
                    .verifyComplete();

        request = new GetPublishedListingsRequestDTO();
        request.setOffset(2);
        request.setSize(2);
        StepVerifier.create(carListingService.getPublishedListings(request))
                .assertNext(page -> {
                    assertThat(page.totalElements()).isEqualTo(3);
                    assertThat(page.content()).hasSize(1);
                    assertThat(page.content().get(0).id()).isEqualTo(listing1Id);
                })
                .verifyComplete();
    }

    @Test
    void getPublishedListingAuthorPhoneOrThrowReturnsPhone() {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        CarListing listing = carListingService.create(userId).block();
        listing = carListingService.update(listing, UPDATE_CAR_LISTING_REQUEST_DTO).block();
        carListingService.updateStatus(listing, ListingStatus.PUBLISHED).block();

        StepVerifier.create(carListingService.getPublishedListingAuthorPhoneOrThrow(listing.id()))
                .assertNext(dto -> assertThat(dto.phoneNumber()).isEqualTo("+380123456789"))
                .verifyComplete();
    }

    @Test
    void getPublishedListingAuthorPhoneOrThrowThrowsNotFoundForDraftListing() {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        CarListing listing = carListingService.create(userId).block();

        StepVerifier.create(carListingService.getPublishedListingAuthorPhoneOrThrow(listing.id()))
                .expectErrorMatches(e -> e instanceof ApiException ex
                        && ex.getStatus() == HttpStatus.NOT_FOUND)
                .verify();
    }

    @Test
    void getPublishedListingAuthorPhoneOrThrowThrowsNotFoundForNonExistentId() {
        StepVerifier.create(carListingService.getPublishedListingAuthorPhoneOrThrow(9999))
                .expectErrorMatches(e -> e instanceof ApiException ex
                        && ex.getStatus() == HttpStatus.NOT_FOUND)
                .verify();
    }

    @Test
    void updateReturnsUpdatedListing() {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        CarListing created = carListingService.create(userId).block();

        var request = new UpdateCarListingRequestDTO(
                "Updated Title", null, null, CarBrand.TOYOTA, null, "Corolla", null,
                null, null, null, null, null, null, null, null, null, null, null, null, null
        );

        StepVerifier.create(carListingService.update(created, request))
                    .assertNext(listing -> {
                        assertThat(listing.id()).isEqualTo(created.id());
                        assertThat(listing.title()).isEqualTo("Updated Title");
                        assertThat(listing.brand()).isEqualTo(CarBrand.TOYOTA);
                        assertThat(listing.model()).isEqualTo("Corolla");
                    })
                    .verifyComplete();

        StepVerifier.create(carListingService.getListingByIdOrThrow(created.id()))
                .assertNext(listing -> {
                    assertThat(listing.id()).isEqualTo(created.id());
                    assertThat(listing.title()).isEqualTo("Updated Title");
                    assertThat(listing.brand()).isEqualTo(CarBrand.TOYOTA);
                    assertThat(listing.model()).isEqualTo("Corolla");
                })
                .verifyComplete();
    }

    @Test
    void updatePublishedListingFailsWhenRequiredFieldsBecomeMissing() {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        CarListing listing = carListingService.create(userId).block();
        listing = carListingService.update(listing, UPDATE_CAR_LISTING_REQUEST_DTO).block();
        carListingService.updateStatus(listing, ListingStatus.PUBLISHED).block();

        CarListing published = carListingService.getListingByIdOrThrow(listing.id()).block();

        var invalidUpdate = new UpdateCarListingRequestDTO(
                null, null, null, CarBrand.TOYOTA, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, null
        );

        assertThatThrownBy(() -> carListingService.update(published, invalidUpdate).block())
                .isInstanceOf(ApiException.class)
                .hasMessage("Listing field is required for publishing: title");

        StepVerifier.create(carListingService.getListingByIdOrThrow(listing.id()))
                .assertNext(updated -> {
                    assertThat(updated.status()).isEqualTo(ListingStatus.PUBLISHED);
                    assertThat(updated.title()).isNotNull();
                })
                .verifyComplete();
    }
}
