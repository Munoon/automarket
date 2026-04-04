package edu.automarket.listing.controller;

import edu.automarket.AbstractIntegrationTest;
import edu.automarket.analytics.CarListingAnalyticsService;
import edu.automarket.common.PageDTO;
import edu.automarket.common.ProblemDTO;
import edu.automarket.listing.CarListingService;
import edu.automarket.listing.dto.AuthorPhoneDTO;
import edu.automarket.listing.dto.OwnCarListingListItemDTO;
import edu.automarket.listing.dto.PublicCarListingDTO;
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
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.test.web.reactive.server.WebTestClient;

import static edu.automarket.listing.CarListingTestUtils.UPDATE_CAR_LISTING_REQUEST_DTO;
import static org.assertj.core.api.Assertions.assertThat;

class PublicCarListingControllerTest extends AbstractIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private UserService userService;

    @Autowired
    private CarListingService carListingService;

    @Autowired
    private CarListingAnalyticsService analyticsService;

    private static final ParameterizedTypeReference<PageDTO<PublicCarListingItemDTO>> PAGE_TYPE =
            new ParameterizedTypeReference<>() {};

    @Test
    void getPublishedListingsWithoutTokenReturnsOk() {
        long now = System.currentTimeMillis();
        webTestClient.get()
                .uri("/api/listings/public?publishedBefore=" + now)
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void getPublishedListingsReturnsOnlyPublishedListings() {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        carListingService.create(userId).block(); // draft
        CarListing published = carListingService.create(userId).block();
        published = carListingService.update(published, UPDATE_CAR_LISTING_REQUEST_DTO).block();
        carListingService.updateStatus(published, ListingStatus.PUBLISHED).block();
        long publishedListingId = published.id();

        webTestClient.get()
                .uri("/api/listings/public")
                .exchange()
                .expectStatus().isOk()
                .expectBody(PAGE_TYPE)
                .value(page -> {
                    assertThat(page.totalElements()).isEqualTo(1);
                    assertThat(page.content()).hasSize(1);
                    assertThat(page.content().get(0).id()).isEqualTo(publishedListingId);
                });
    }

    @Test
    void getPublishedListingsFiltersOutListingsPublishedAfterAnchor() {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        CarListing listing = carListingService.create(userId).block();
        listing = carListingService.update(listing, UPDATE_CAR_LISTING_REQUEST_DTO).block();
        carListingService.updateStatus(listing, ListingStatus.PUBLISHED).block();

        long before = System.currentTimeMillis() - 1_000;
        webTestClient.get()
                .uri("/api/listings/public?publishedBefore=" + before)
                .exchange()
                .expectStatus().isOk()
                .expectBody(PAGE_TYPE)
                .value(page -> {
                    assertThat(page.totalElements()).isEqualTo(0);
                    assertThat(page.content()).isEmpty();
                });
    }

    @Test
    void getPublishedListingsReturnsCorrectFields() {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        CarListing listing = carListingService.create(userId).block();
        listing = carListingService.update(listing, UPDATE_CAR_LISTING_REQUEST_DTO).block();
        CarListing updatedListing = carListingService.update(listing, new UpdateCarListingRequestDTO(
                "Test Car", "A nice car", CarBrand.TOYOTA, null, "Camry",
                "AA2222BB", CarCondition.NEW, 1234, 500000L,
                City.KYIV, CarColor.RED, TransmissionType.MANUAL, FuelType.ELECTRIC, 25.4,
                DriveType.FWD, BodyType.SEDAN, 2024, 34.1, 4
        )).block();
        carListingService.updateStatus(updatedListing, ListingStatus.PUBLISHED).block();
        long listingId = listing.id();

        long now = System.currentTimeMillis();
        webTestClient.get()
                .uri("/api/listings/public?publishedBefore=" + now)
                .exchange()
                .expectStatus().isOk()
                .expectBody(PAGE_TYPE)
                .value(page -> {
                    assertThat(page.totalElements()).isEqualTo(1);
                    PublicCarListingItemDTO dto = page.content().get(0);
                    assertThat(dto.id()).isEqualTo(listingId);
                    assertThat(dto.title()).isEqualTo("Test Car");
                    assertThat(dto.price()).isEqualTo(500000L);
                    assertThat(dto.mileage()).isEqualTo(1234);
                    assertThat(dto.fuelType()).isEqualTo(FuelType.ELECTRIC);
                    assertThat(dto.transmission()).isEqualTo(TransmissionType.MANUAL);
                    assertThat(dto.city()).isEqualTo(City.KYIV);
                    assertThat(dto.year()).isEqualTo(2024);
                });
    }

    @Test
    void getPublishedListingsReturnsEmptyWhenNonePublished() {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        carListingService.create(userId).block();

        long now = System.currentTimeMillis();
        webTestClient.get()
                .uri("/api/listings/public?publishedBefore=" + now)
                .exchange()
                .expectStatus().isOk()
                .expectBody(PAGE_TYPE)
                .value(page -> {
                    assertThat(page.totalElements()).isEqualTo(0);
                    assertThat(page.content()).isEmpty();
                });
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

        long anchor = System.currentTimeMillis();

        webTestClient.get()
                .uri("/api/listings/public?publishedBefore=" + anchor + "&page=0&size=2")
                .exchange()
                .expectStatus().isOk()
                .expectBody(PAGE_TYPE)
                .value(page -> {
                    assertThat(page.totalElements()).isEqualTo(3);
                    assertThat(page.content()).hasSize(2);
                    assertThat(page.content().get(0).id()).isEqualTo(listing2Id);
                    assertThat(page.content().get(1).id()).isEqualTo(listing3Id);
                });

        webTestClient.get()
                .uri("/api/listings/public?publishedBefore=" + anchor + "&page=1&size=2")
                .exchange()
                .expectStatus().isOk()
                .expectBody(PAGE_TYPE)
                .value(page -> {
                    assertThat(page.totalElements()).isEqualTo(3);
                    assertThat(page.content()).hasSize(1);
                    assertThat(page.content().get(0).id()).isEqualTo(listing1Id);
                });
    }

    // --- GET /api/listings/public/{id} ---

    @Test
    void getByIdReturnsPublishedListingWithoutToken() {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        userService.updateDisplayName(userId, "Test User").block();
        CarListing listing = carListingService.create(userId).block();
        CarListing updatedListing = carListingService.update(listing, new UpdateCarListingRequestDTO(
                "Test Car", "Nice description", CarBrand.TOYOTA, null, "Camry",
                "AA2222BB", CarCondition.NEW, 1234, 200000L, City.KHARKIV, CarColor.RED,
                TransmissionType.MANUAL, FuelType.ELECTRIC, 24.3, DriveType.AWD, BodyType.SEDAN,
                2024, 34.5, 10
        )).block();
        carListingService.updateStatus(updatedListing, ListingStatus.PUBLISHED).block();

        webTestClient.get()
                .uri("/api/listings/public/" + listing.id())
                .exchange()
                .expectStatus().isOk()
                .expectBody(PublicCarListingDTO.class)
                .value(dto -> {
                    assertThat(dto.id()).isEqualTo(listing.id());
                    assertThat(dto.authorDisplayName()).isEqualTo("Test User");
                    assertThat(dto.title()).isEqualTo("Test Car");
                    assertThat(dto.description()).isEqualTo("Nice description");
                    assertThat(dto.brand()).isEqualTo(CarBrand.TOYOTA);
                    assertThat(dto.price()).isEqualTo(200000L);
                    assertThat(dto.model()).isEqualTo("Camry");
                    assertThat(dto.licensePlate()).isEqualTo("AA2222BB");
                    assertThat(dto.condition()).isEqualTo(CarCondition.NEW);
                    assertThat(dto.mileage()).isEqualTo(1234);
                    assertThat(dto.price()).isEqualTo(200000L);
                    assertThat(dto.city()).isEqualTo(City.KHARKIV);
                    assertThat(dto.color()).isEqualTo(CarColor.RED);
                    assertThat(dto.transmission()).isEqualTo(TransmissionType.MANUAL);
                    assertThat(dto.fuelType()).isEqualTo(FuelType.ELECTRIC);
                    assertThat(dto.tankVolume()).isEqualTo(24.3);
                    assertThat(dto.driveType()).isEqualTo(DriveType.AWD);
                    assertThat(dto.bodyType()).isEqualTo(BodyType.SEDAN);
                    assertThat(dto.year()).isEqualTo(2024);
                    assertThat(dto.engineVolume()).isEqualTo(34.5);
                    assertThat(dto.ownersCount()).isEqualTo(10);
                    assertThat(dto.publishedAt()).isPositive();
                });
    }

    @Test
    void getByIdReturns404ForDraftListing() {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        CarListing listing = carListingService.create(userId).block();

        webTestClient.get()
                .uri("/api/listings/public/" + listing.id())
                .exchange()
                .expectStatus().isNotFound()
                .expectHeader().contentType("application/problem+json")
                .expectBody(ProblemDTO.class)
                .value(problem -> {
                    assertThat(problem.type()).isEqualTo("/problems/listing-not-found");
                    assertThat(problem.title()).isEqualTo("Listing not found");
                    assertThat(problem.status()).isEqualTo(404);
                });
    }

    @Test
    void getByIdReturns404ForNonExistentListing() {
        webTestClient.get()
                .uri("/api/listings/public/9999")
                .exchange()
                .expectStatus().isNotFound()
                .expectHeader().contentType("application/problem+json")
                .expectBody(ProblemDTO.class)
                .value(problem -> {
                    assertThat(problem.type()).isEqualTo("/problems/listing-not-found");
                    assertThat(problem.title()).isEqualTo("Listing not found");
                    assertThat(problem.status()).isEqualTo(404);
                });
    }

    // --- GET /api/listings/public/{id}/phone ---

    @Test
    void getAuthorPhoneReturnsPhoneForPublishedListingWithoutToken() {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        CarListing listing = carListingService.create(userId).block();
        listing = carListingService.update(listing, UPDATE_CAR_LISTING_REQUEST_DTO).block();
        carListingService.updateStatus(listing, ListingStatus.PUBLISHED).block();

        webTestClient.get()
                .uri("/api/listings/public/" + listing.id() + "/phone")
                .exchange()
                .expectStatus().isOk()
                .expectBody(AuthorPhoneDTO.class)
                .value(dto -> assertThat(dto.phoneNumber()).isEqualTo("+380123456789"));
    }

    @Test
    void getAuthorPhoneReturns404ForDraftListing() {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        CarListing listing = carListingService.create(userId).block();

        webTestClient.get()
                .uri("/api/listings/public/" + listing.id() + "/phone")
                .exchange()
                .expectStatus().isNotFound()
                .expectHeader().contentType("application/problem+json")
                .expectBody(ProblemDTO.class)
                .value(problem -> {
                    assertThat(problem.type()).isEqualTo("/problems/listing-not-found");
                    assertThat(problem.title()).isEqualTo("Listing not found");
                    assertThat(problem.status()).isEqualTo(404);
                });
    }

    @Test
    void getAuthorPhoneReturns404ForNonExistentListing() {
        webTestClient.get()
                .uri("/api/listings/public/9999/phone")
                .exchange()
                .expectStatus().isNotFound()
                .expectHeader().contentType("application/problem+json")
                .expectBody(ProblemDTO.class)
                .value(problem -> {
                    assertThat(problem.type()).isEqualTo("/problems/listing-not-found");
                    assertThat(problem.title()).isEqualTo("Listing not found");
                    assertThat(problem.status()).isEqualTo(404);
                });
    }

    // --- Analytics recording ---

    @Test
    void getById_recordsView_whenListingIsPublished() {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        CarListing listing = carListingService.create(userId).block();
        listing = carListingService.update(listing, UPDATE_CAR_LISTING_REQUEST_DTO).block();
        carListingService.updateStatus(listing, ListingStatus.PUBLISHED).block();

        webTestClient.get()
                .uri("/api/listings/public/" + listing.id())
                .exchange()
                .expectStatus().isOk();

        analyticsService.saveListingAnalytics();

        OwnCarListingListItemDTO dto = queryAnalytics(userId, listing.id());
        assertThat(dto).isNotNull();
        assertThat(dto.viewsCount()).isEqualTo(1);
    }

    @Test
    void getAuthorPhone_recordsPhoneRequest_whenListingIsPublished() {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        CarListing listing = carListingService.create(userId).block();
        listing = carListingService.update(listing, UPDATE_CAR_LISTING_REQUEST_DTO).block();
        carListingService.updateStatus(listing, ListingStatus.PUBLISHED).block();

        webTestClient.get()
                .uri("/api/listings/public/" + listing.id() + "/phone")
                .exchange()
                .expectStatus().isOk();

        analyticsService.saveListingAnalytics();

        OwnCarListingListItemDTO dto = queryAnalytics(userId, listing.id());
        assertThat(dto).isNotNull();
        assertThat(dto.phoneRequestsCount()).isEqualTo(1);
    }

    @Test
    void getPublishedListings_recordsImpression_forEachReturnedListing() {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        CarListing listing1 = carListingService.create(userId).block();
        CarListing listing2 = carListingService.create(userId).block();
        listing1 = carListingService.update(listing1, UPDATE_CAR_LISTING_REQUEST_DTO).block();
        listing2 = carListingService.update(listing2, UPDATE_CAR_LISTING_REQUEST_DTO).block();
        carListingService.updateStatus(listing1, ListingStatus.PUBLISHED).block();
        carListingService.updateStatus(listing2, ListingStatus.PUBLISHED).block();

        webTestClient.get()
                .uri("/api/listings/public")
                .exchange()
                .expectStatus().isOk();

        analyticsService.saveListingAnalytics();

        assertThat(queryAnalytics(userId, listing1.id()).impressionsCount()).isEqualTo(1);
        assertThat(queryAnalytics(userId, listing2.id()).impressionsCount()).isEqualTo(1);
    }

    @Test
    void getById_doesNotRecordView_whenListingIsNotPublished() {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        CarListing listing = carListingService.create(userId).block();

        webTestClient.get()
                .uri("/api/listings/public/" + listing.id())
                .exchange()
                .expectStatus().isNotFound()
                .expectHeader().contentType("application/problem+json")
                .expectBody(ProblemDTO.class)
                .value(problem -> {
                    assertThat(problem.type()).isEqualTo("/problems/listing-not-found");
                    assertThat(problem.title()).isEqualTo("Listing not found");
                    assertThat(problem.status()).isEqualTo(404);
                });

        analyticsService.saveListingAnalytics();

        OwnCarListingListItemDTO dto = queryAnalytics(userId, listing.id());
        assertThat(dto).isNotNull();
        assertThat(dto.viewsCount()).isEqualTo(0);
    }

    @Test
    void getAuthorPhone_doesNotRecordPhoneRequest_whenListingIsNotPublished() {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        CarListing listing = carListingService.create(userId).block();

        webTestClient.get()
                .uri("/api/listings/public/" + listing.id() + "/phone")
                .exchange()
                .expectStatus().isNotFound()
                .expectHeader().contentType("application/problem+json")
                .expectBody(ProblemDTO.class)
                .value(problem -> {
                    assertThat(problem.type()).isEqualTo("/problems/listing-not-found");
                    assertThat(problem.title()).isEqualTo("Listing not found");
                    assertThat(problem.status()).isEqualTo(404);
                });

        analyticsService.saveListingAnalytics();

        OwnCarListingListItemDTO dto = queryAnalytics(userId, listing.id());
        assertThat(dto).isNotNull();
        assertThat(dto.phoneRequestsCount()).isEqualTo(0);
    }

    private OwnCarListingListItemDTO queryAnalytics(long userId, long listingId) {
        PageDTO<OwnCarListingListItemDTO> page = carListingService.getOwnListings(userId, null, 0, 100).block();
        for (OwnCarListingListItemDTO item : page.content()) {
            if (item.id() == listingId) {
                return item;
            }
        }
        return null;
    }
}
