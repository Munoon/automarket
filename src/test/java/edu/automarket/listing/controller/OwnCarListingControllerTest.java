package edu.automarket.listing.controller;

import edu.automarket.AbstractIntegrationTest;
import edu.automarket.analytics.CarListingAnalyticsCounter;
import edu.automarket.analytics.CarListingAnalyticsRepository;
import edu.automarket.analytics.dto.ListingAnalyticsDayDTO;
import edu.automarket.authentication.AuthenticationService;
import edu.automarket.common.ApiException;
import edu.automarket.common.PageDTO;
import edu.automarket.common.ProblemDTO;
import edu.automarket.listing.CarListingService;
import edu.automarket.listing.dto.OwnCarListingDTO;
import edu.automarket.listing.dto.OwnCarListingListItemDTO;
import edu.automarket.listing.dto.UpdateCarListingRequestDTO;
import edu.automarket.listing.dto.UpdateListingStatusRequestDTO;
import edu.automarket.listing.model.CarBrand;
import edu.automarket.listing.model.CarListing;
import edu.automarket.listing.model.ListingStatus;
import edu.automarket.user.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class OwnCarListingControllerTest extends AbstractIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private UserService userService;

    @Autowired
    private CarListingService carListingService;

    @Autowired
    private AuthenticationService authenticationService;

    @Autowired
    private CarListingAnalyticsRepository analyticsRepository;

    private static final ParameterizedTypeReference<PageDTO<OwnCarListingListItemDTO>> PAGE_TYPE =
            new ParameterizedTypeReference<>() {};

    private static final ParameterizedTypeReference<List<ListingAnalyticsDayDTO>> ANALYTICS_LIST_TYPE =
            new ParameterizedTypeReference<>() {};

    // --- POST /api/listings/own ---

    @Test
    void createWithValidTokenReturns201WithDraftListing() {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        String token = authenticationService.generateToken(userId);

        webTestClient.post()
                .uri("/api/listings/own")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(OwnCarListingDTO.class)
                .value(dto -> {
                    assertThat(dto.id()).isNotNull();
                    assertThat(dto.status()).isEqualTo(ListingStatus.DRAFT);
                    assertThat(dto.createdAt()).isPositive();
                });
    }

    @Test
    void createWithoutTokenReturns401() {
        webTestClient.post()
                .uri("/api/listings/own")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().contentType("application/problem+json")
                .expectBody(ProblemDTO.class)
                .value(problem -> {
                    assertThat(problem.type()).isEqualTo("/problems/unauthorized");
                    assertThat(problem.title()).isEqualTo("Unauthorized");
                    assertThat(problem.status()).isEqualTo(401);
                });
    }

    @Test
    void createCountLimit() {
        long userId1 = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        String token1 = authenticationService.generateToken(userId1);

        long userId2 = userService.getUserByPhoneNumberOrCreate("+380123456780").block().id();
        String token2 = authenticationService.generateToken(userId2);

        for (int i = 0; i < 30; i++) {
            webTestClient.post()
                         .uri("/api/listings/own")
                         .header("Authorization", "Bearer " + token1)
                         .exchange()
                         .expectStatus().isCreated()
                         .expectBody(OwnCarListingDTO.class)
                         .value(dto -> {
                             assertThat(dto.id()).isNotNull();
                             assertThat(dto.status()).isEqualTo(ListingStatus.DRAFT);
                             assertThat(dto.createdAt()).isPositive();
                         });
        }

        webTestClient.post()
                     .uri("/api/listings/own")
                     .header("Authorization", "Bearer " + token1)
                     .exchange()
                     .expectStatus().isForbidden()
                     .expectHeader().contentType("application/problem+json")
                     .expectBody(ProblemDTO.class)
                     .value(problem -> {
                         assertThat(problem.type()).isEqualTo("/problems/listing-limit-reached");
                         assertThat(problem.title()).isEqualTo("You have reached the limit of listings per user");
                         assertThat(problem.status()).isEqualTo(403);
                     });

        webTestClient.post()
                     .uri("/api/listings/own")
                     .header("Authorization", "Bearer " + token2)
                     .exchange()
                     .expectStatus().isCreated()
                     .expectBody(OwnCarListingDTO.class)
                     .value(dto -> {
                         assertThat(dto.id()).isNotNull();
                         assertThat(dto.status()).isEqualTo(ListingStatus.DRAFT);
                         assertThat(dto.createdAt()).isPositive();
                     });
    }

    // --- GET /api/listings/own ---

    @Test
    void getOwnListingsWithValidTokenReturnsPage() {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        String token = authenticationService.generateToken(userId);
        CarListing listing = carListingService.create(userId).block();

        webTestClient.get()
                .uri("/api/listings/own")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody(PAGE_TYPE)
                .value(page -> {
                    assertThat(page.totalElements()).isEqualTo(1);
                    assertThat(page.content()).hasSize(1);
                    assertThat(page.content().get(0).id()).isEqualTo(listing.id());
                    assertThat(page.content().get(0).status()).isEqualTo(ListingStatus.DRAFT);
                });
    }

    @Test
    void getOwnListingsWithoutTokenReturns401() {
        webTestClient.get()
                .uri("/api/listings/own")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().contentType("application/problem+json")
                .expectBody(ProblemDTO.class)
                .value(problem -> {
                    assertThat(problem.type()).isEqualTo("/problems/unauthorized");
                    assertThat(problem.title()).isEqualTo("Unauthorized");
                    assertThat(problem.status()).isEqualTo(401);
                });
    }

    @Test
    void getOwnListingsWithDraftStatusFilterReturnsDraftListings() {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        String token = authenticationService.generateToken(userId);
        CarListing draft = carListingService.create(userId).block();
        CarListing published = carListingService.create(userId).block();
        carListingService.updateStatus(published, ListingStatus.PUBLISHED).block();

        webTestClient.get()
                .uri("/api/listings/own?statuses=DRAFT")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody(PAGE_TYPE)
                .value(page -> {
                    assertThat(page.totalElements()).isEqualTo(1);
                    assertThat(page.content().get(0).id()).isEqualTo(draft.id());
                    assertThat(page.content().get(0).status()).isEqualTo(ListingStatus.DRAFT);
                });
    }

    @Test
    void getOwnListingsWithPublishedStatusFilterReturnsOnlyPublishedListings() {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        String token = authenticationService.generateToken(userId);
        carListingService.create(userId).block();
        CarListing published = carListingService.create(userId).block();
        carListingService.updateStatus(published, ListingStatus.PUBLISHED).block();

        webTestClient.get()
                .uri("/api/listings/own?statuses=PUBLISHED")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody(PAGE_TYPE)
                .value(page -> {
                    assertThat(page.totalElements()).isEqualTo(1);
                    assertThat(page.content().get(0).id()).isEqualTo(published.id());
                    assertThat(page.content().get(0).status()).isEqualTo(ListingStatus.PUBLISHED);
                });
    }

    @Test
    void getOwnListingsWithMultipleStatusFiltersReturnsMatchingListings() {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        String token = authenticationService.generateToken(userId);
        carListingService.create(userId).block();
        CarListing published = carListingService.create(userId).block();
        CarListing archived = carListingService.create(userId).block();
        carListingService.updateStatus(published, ListingStatus.PUBLISHED).block();
        carListingService.updateStatus(archived, ListingStatus.ARCHIVED).block();

        webTestClient.get()
                .uri("/api/listings/own?statuses=DRAFT&statuses=PUBLISHED")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody(PAGE_TYPE)
                .value(page -> {
                    assertThat(page.totalElements()).isEqualTo(2);
                    assertThat(page.content()).extracting(OwnCarListingListItemDTO::status)
                            .containsExactlyInAnyOrder(ListingStatus.DRAFT, ListingStatus.PUBLISHED);
                });
    }

    @Test
    void getOwnListingsPaginatesCorrectly() {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        String token = authenticationService.generateToken(userId);
        carListingService.create(userId).block();
        carListingService.create(userId).block();
        carListingService.create(userId).block();

        webTestClient.get()
                .uri("/api/listings/own?page=0&size=2")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody(PAGE_TYPE)
                .value(page -> {
                    assertThat(page.totalElements()).isEqualTo(3);
                    assertThat(page.content()).hasSize(2);
                });
    }

    @Test
    void getOwnListingsDoesNotReturnOtherUsersListings() {
        long userId1 = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        long userId2 = userService.getUserByPhoneNumberOrCreate("+380123456780").block().id();
        String token2 = authenticationService.generateToken(userId2);
        carListingService.create(userId1).block();

        webTestClient.get()
                .uri("/api/listings/own")
                .header("Authorization", "Bearer " + token2)
                .exchange()
                .expectStatus().isOk()
                .expectBody(PAGE_TYPE)
                .value(page -> assertThat(page.totalElements()).isEqualTo(0));
    }

    // --- GET /api/listings/own/{id} ---

    @Test
    void getByIdWithValidTokenReturnsListing() {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        String token = authenticationService.generateToken(userId);
        CarListing listing = carListingService.create(userId).block();

        webTestClient.get()
                .uri("/api/listings/own/" + listing.id())
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody(OwnCarListingDTO.class)
                .value(dto -> assertThat(dto.id()).isEqualTo(listing.id()));
    }

    @Test
    void getByIdWithoutTokenReturns401() {
        webTestClient.get()
                .uri("/api/listings/own/1")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().contentType("application/problem+json")
                .expectBody(ProblemDTO.class)
                .value(problem -> {
                    assertThat(problem.type()).isEqualTo("/problems/unauthorized");
                    assertThat(problem.title()).isEqualTo("Unauthorized");
                    assertThat(problem.status()).isEqualTo(401);
                });
    }

    @Test
    void getByIdNonExistentReturns404() {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        String token = authenticationService.generateToken(userId);

        webTestClient.get()
                .uri("/api/listings/own/9999")
                .header("Authorization", "Bearer " + token)
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
    void getByIdOtherUsersListingReturns403() {
        long userId1 = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        long userId2 = userService.getUserByPhoneNumberOrCreate("+380123456780").block().id();
        String token2 = authenticationService.generateToken(userId2);
        CarListing listing = carListingService.create(userId1).block();

        webTestClient.get()
                .uri("/api/listings/own/" + listing.id())
                .header("Authorization", "Bearer " + token2)
                .exchange()
                .expectStatus().isForbidden()
                .expectHeader().contentType("application/problem+json")
                .expectBody(ProblemDTO.class)
                .value(problem -> {
                    assertThat(problem.type()).isEqualTo("/problems/access-denied");
                    assertThat(problem.title()).isEqualTo("Access denied");
                    assertThat(problem.status()).isEqualTo(403);
                });
    }

    // --- PATCH /api/listings/own/{id}/status ---

    @Test
    void updateStatusWithValidTokenReturns204AndPersists() {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        String token = authenticationService.generateToken(userId);
        CarListing listing = carListingService.create(userId).block();

        webTestClient.patch()
                .uri("/api/listings/own/" + listing.id() + "/status")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new UpdateListingStatusRequestDTO(ListingStatus.PUBLISHED))
                .exchange()
                .expectStatus().isNoContent()
                .expectBody().isEmpty();

        StepVerifier.create(carListingService.getListingByIdOrThrow(listing.id()))
                .assertNext(updated -> assertThat(updated.status()).isEqualTo(ListingStatus.PUBLISHED))
                .verifyComplete();
    }

    @Test
    void updateStatusToTheSameReturns400() {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        String token = authenticationService.generateToken(userId);
        CarListing listing = carListingService.create(userId).block();

        webTestClient.patch()
                     .uri("/api/listings/own/" + listing.id() + "/status")
                     .header("Authorization", "Bearer " + token)
                     .contentType(MediaType.APPLICATION_JSON)
                     .bodyValue(new UpdateListingStatusRequestDTO(ListingStatus.DRAFT))
                     .exchange()
                     .expectStatus().isBadRequest()
                     .expectHeader().contentType("application/problem+json")
                     .expectBody(ProblemDTO.class)
                     .value(problem -> {
                         assertThat(problem.type()).isEqualTo("/problems/status-already-set");
                         assertThat(problem.title()).isEqualTo("Status already set");
                         assertThat(problem.status()).isEqualTo(400);
                     });
    }

    @Test
    void updateStatusWithoutTokenReturns401() {
        webTestClient.patch()
                .uri("/api/listings/own/1/status")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new UpdateListingStatusRequestDTO(ListingStatus.PUBLISHED))
                .exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().contentType("application/problem+json")
                .expectBody(ProblemDTO.class)
                .value(problem -> {
                    assertThat(problem.type()).isEqualTo("/problems/unauthorized");
                    assertThat(problem.title()).isEqualTo("Unauthorized");
                    assertThat(problem.status()).isEqualTo(401);
                });
    }

    @Test
    void updateStatusNonExistentListingReturns404() {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        String token = authenticationService.generateToken(userId);

        webTestClient.patch()
                .uri("/api/listings/own/9999/status")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new UpdateListingStatusRequestDTO(ListingStatus.PUBLISHED))
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
    void updateStatusOtherUsersListingReturns403() {
        long userId1 = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        long userId2 = userService.getUserByPhoneNumberOrCreate("+380123456780").block().id();
        String token2 = authenticationService.generateToken(userId2);
        CarListing listing = carListingService.create(userId1).block();

        webTestClient.patch()
                .uri("/api/listings/own/" + listing.id() + "/status")
                .header("Authorization", "Bearer " + token2)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new UpdateListingStatusRequestDTO(ListingStatus.PUBLISHED))
                .exchange()
                .expectStatus().isForbidden()
                .expectHeader().contentType("application/problem+json")
                .expectBody(ProblemDTO.class)
                .value(problem -> {
                    assertThat(problem.type()).isEqualTo("/problems/access-denied");
                    assertThat(problem.title()).isEqualTo("Access denied");
                    assertThat(problem.status()).isEqualTo(403);
                });
    }

    @Test
    void updateStatusCooldownOnRepublish() throws InterruptedException {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        String token = authenticationService.generateToken(userId);
        CarListing listing = carListingService.create(userId).block();

        webTestClient.patch()
                     .uri("/api/listings/own/" + listing.id() + "/status")
                     .header("Authorization", "Bearer " + token)
                     .contentType(MediaType.APPLICATION_JSON)
                     .bodyValue(new UpdateListingStatusRequestDTO(ListingStatus.PUBLISHED))
                     .exchange()
                     .expectStatus().isNoContent()
                     .expectBody().isEmpty();

        listing = carListingService.getListingByIdOrThrow(listing.id()).block();
        assertThat(listing).isNotNull();
        assertThat(listing.status()).isEqualTo(ListingStatus.PUBLISHED);

        webTestClient.patch()
                     .uri("/api/listings/own/" + listing.id() + "/status")
                     .header("Authorization", "Bearer " + token)
                     .contentType(MediaType.APPLICATION_JSON)
                     .bodyValue(new UpdateListingStatusRequestDTO(ListingStatus.ARCHIVED))
                     .exchange()
                     .expectStatus().isNoContent()
                     .expectBody().isEmpty();

        listing = carListingService.getListingByIdOrThrow(listing.id()).block();
        assertThat(listing).isNotNull();
        assertThat(listing.status()).isEqualTo(ListingStatus.ARCHIVED);

        // re-publish before the cooldown passed
        webTestClient.patch()
                     .uri("/api/listings/own/" + listing.id() + "/status")
                     .header("Authorization", "Bearer " + token)
                     .contentType(MediaType.APPLICATION_JSON)
                     .bodyValue(new UpdateListingStatusRequestDTO(ListingStatus.PUBLISHED))
                     .exchange()
                     .expectStatus().isBadRequest()
                     .expectHeader().contentType("application/problem+json")
                     .expectBody(ProblemDTO.class)
                     .value(problem -> {
                         assertThat(problem.type()).isEqualTo("/problems/listing-publish-cooldown");
                         assertThat(problem.title()).isEqualTo("Listing publishing cooldown in progress");
                         assertThat(problem.status()).isEqualTo(400);
                     });

        listing = carListingService.getListingByIdOrThrow(listing.id()).block();
        assertThat(listing).isNotNull();
        assertThat(listing.status()).isEqualTo(ListingStatus.ARCHIVED);

        // wait for cooldown to pass
        Thread.sleep(2_000);

        webTestClient.patch()
                     .uri("/api/listings/own/" + listing.id() + "/status")
                     .header("Authorization", "Bearer " + token)
                     .contentType(MediaType.APPLICATION_JSON)
                     .bodyValue(new UpdateListingStatusRequestDTO(ListingStatus.PUBLISHED))
                     .exchange()
                     .expectStatus().isNoContent()
                     .expectBody().isEmpty();

        listing = carListingService.getListingByIdOrThrow(listing.id()).block();
        assertThat(listing).isNotNull();
        assertThat(listing.status()).isEqualTo(ListingStatus.PUBLISHED);
    }

    // --- PATCH /api/listings/own/{id} ---

    @Test
    void updateWithValidTokenReturns204() {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        String token = authenticationService.generateToken(userId);
        CarListing listing = carListingService.create(userId).block();

        var request = new UpdateCarListingRequestDTO(
                "My Car", null, CarBrand.CUSTOM, "Custom brand name", "Camry", null,
                null, null, null, null, null, null, null, null, null, null, null, null, null
        );

        webTestClient.patch()
                .uri("/api/listings/own/" + listing.id())
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isNoContent()
                .expectBody().isEmpty();

        StepVerifier.create(carListingService.getListingByIdOrThrow(listing.id()))
                .assertNext(updated -> {
                    assertThat(updated.title()).isEqualTo("My Car");
                    assertThat(updated.brand()).isEqualTo(CarBrand.CUSTOM);
                    assertThat(updated.customBrandName()).isEqualTo("Custom brand name");
                    assertThat(updated.model()).isEqualTo("Camry");
                })
                .verifyComplete();
    }

    @Test
    void updateWithoutTokenReturns401() {
        webTestClient.patch()
                .uri("/api/listings/own/1")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new UpdateCarListingRequestDTO(
                        null, null, null, null, null, null,
                        null, null, null, null, null, null, null, null, null, null, null, null, null
                ))
                .exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().contentType("application/problem+json")
                .expectBody(ProblemDTO.class)
                .value(problem -> {
                    assertThat(problem.type()).isEqualTo("/problems/unauthorized");
                    assertThat(problem.title()).isEqualTo("Unauthorized");
                    assertThat(problem.status()).isEqualTo(401);
                });
    }

    @Test
    void updateNonExistentListingReturns404() {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        String token = authenticationService.generateToken(userId);

        webTestClient.patch()
                .uri("/api/listings/own/9999")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new UpdateCarListingRequestDTO(
                        null, null, null, null, null, null,
                        null, null, null, null, null, null, null, null, null, null, null, null, null
                ))
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
    void updateOtherUsersListingReturns403() {
        long userId1 = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        long userId2 = userService.getUserByPhoneNumberOrCreate("+380123456780").block().id();
        String token2 = authenticationService.generateToken(userId2);
        CarListing listing = carListingService.create(userId1).block();

        webTestClient.patch()
                .uri("/api/listings/own/" + listing.id())
                .header("Authorization", "Bearer " + token2)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new UpdateCarListingRequestDTO(
                        null, null, null, null, null, null,
                        null, null, null, null, null, null, null, null, null, null, null, null, null
                ))
                .exchange()
                .expectStatus().isForbidden()
                .expectHeader().contentType("application/problem+json")
                .expectBody(ProblemDTO.class)
                .value(problem -> {
                    assertThat(problem.type()).isEqualTo("/problems/access-denied");
                    assertThat(problem.title()).isEqualTo("Access denied");
                    assertThat(problem.status()).isEqualTo(403);
                });
    }

    @Test
    void updateWithInvalidFieldReturns400() {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        String token = authenticationService.generateToken(userId);
        CarListing listing = carListingService.create(userId).block();

        webTestClient.patch()
                .uri("/api/listings/own/" + listing.id())
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new UpdateCarListingRequestDTO(
                        "A".repeat(201), null, null, null, null, null,
                        null, null, null, null, null, null, null, null, null, null, null, null, null
                ))
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentType("application/problem+json")
                .expectBody(ProblemDTO.class)
                .value(problem -> {
                    assertThat(problem.type()).isEqualTo("/problems/validation-error");
                    assertThat(problem.title()).isEqualTo("Validation Error");
                    assertThat(problem.status()).isEqualTo(400);
                });
    }

    @Test
    void updateWithCustomBrandNameForNonCustomBrandReturns400() {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        String token = authenticationService.generateToken(userId);
        CarListing listing = carListingService.create(userId).block();

        webTestClient.patch()
                .uri("/api/listings/own/" + listing.id())
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new UpdateCarListingRequestDTO(
                        null, null, CarBrand.TOYOTA, "My Brand", null, null,
                        null, null, null, null, null, null, null, null, null, null, null, null, null
                ))
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentType("application/problem+json")
                .expectBody(ProblemDTO.class)
                .value(problem -> {
                    assertThat(problem.type()).isEqualTo("/problems/invalid-custom-brand-name");
                    assertThat(problem.title()).isEqualTo("customBrandName can only be set when brand is CUSTOM");
                    assertThat(problem.status()).isEqualTo(400);
                });
    }

    // --- DELETE /api/listings/own/{id} ---

    @Test
    void deleteWithValidTokenReturns204AndRemovesListing() {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        String token = authenticationService.generateToken(userId);
        CarListing listing = carListingService.create(userId).block();

        webTestClient.delete()
                .uri("/api/listings/own/" + listing.id())
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isNoContent()
                .expectBody().isEmpty();

        StepVerifier.create(carListingService.getListingByIdOrThrow(listing.id()))
                .expectErrorMatches(e -> e instanceof ApiException ex
                        && ex.getStatus() == HttpStatus.NOT_FOUND)
                .verify();
    }

    @Test
    void deleteWithoutTokenReturns401() {
        webTestClient.delete()
                .uri("/api/listings/own/1")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().contentType("application/problem+json")
                .expectBody(ProblemDTO.class)
                .value(problem -> {
                    assertThat(problem.type()).isEqualTo("/problems/unauthorized");
                    assertThat(problem.title()).isEqualTo("Unauthorized");
                    assertThat(problem.status()).isEqualTo(401);
                });
    }

    @Test
    void deleteNonExistentListingReturns404() {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        String token = authenticationService.generateToken(userId);

        webTestClient.delete()
                .uri("/api/listings/own/9999")
                .header("Authorization", "Bearer " + token)
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
    void deleteOtherUsersListingReturns403() {
        long userId1 = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        long userId2 = userService.getUserByPhoneNumberOrCreate("+380123456780").block().id();
        String token2 = authenticationService.generateToken(userId2);
        CarListing listing = carListingService.create(userId1).block();

        webTestClient.delete()
                .uri("/api/listings/own/" + listing.id())
                .header("Authorization", "Bearer " + token2)
                .exchange()
                .expectStatus().isForbidden()
                .expectHeader().contentType("application/problem+json")
                .expectBody(ProblemDTO.class)
                .value(problem -> {
                    assertThat(problem.type()).isEqualTo("/problems/access-denied");
                    assertThat(problem.title()).isEqualTo("Access denied");
                    assertThat(problem.status()).isEqualTo(403);
                });
    }

    // --- GET /api/listings/own/{id}/analytics ---

    @Test
    void getAnalyticsReturnsEmptyWhenNoData() {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        String token = authenticationService.generateToken(userId);
        CarListing listing = carListingService.create(userId).block();

        webTestClient.get()
                .uri("/api/listings/own/" + listing.id() + "/analytics")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody(ANALYTICS_LIST_TYPE)
                .value(list -> assertThat(list).isEmpty());
    }

    @Test
    void getAnalyticsReturnsDailyAggregatedData() {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        String token = authenticationService.generateToken(userId);
        CarListing listing = carListingService.create(userId).block();

        OffsetDateTime today = OffsetDateTime.now(ZoneOffset.UTC).withHour(0).withMinute(0).withSecond(0).withNano(0);
        long now = System.currentTimeMillis();
        long sameDay = now - 60_000;
        long yesterday = now - Duration.ofDays(1).toMillis();

        CarListingAnalyticsCounter counter1 = new CarListingAnalyticsCounter();
        counter1.impressionsCount().add(3);
        counter1.viewsCount().add(1);
        counter1.phoneRequestsCount().add(1);
        counter1.favouritesCount().add(1);

        CarListingAnalyticsCounter counter2 = new CarListingAnalyticsCounter();
        counter2.impressionsCount().add(2);
        counter2.viewsCount().add(2);

        CarListingAnalyticsCounter counter3 = new CarListingAnalyticsCounter();
        counter3.impressionsCount().add(10);
        counter3.viewsCount().add(10);

        analyticsRepository.saveAnalytics(now, List.of(Map.entry(listing.id(), counter1))).block();
        analyticsRepository.saveAnalytics(sameDay, List.of(Map.entry(listing.id(), counter2))).block();
        analyticsRepository.saveAnalytics(yesterday, List.of(Map.entry(listing.id(), counter3))).block();

        webTestClient.get()
                .uri("/api/listings/own/" + listing.id() + "/analytics")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody(ANALYTICS_LIST_TYPE)
                .value(list -> {
                    assertThat(list).hasSize(2);

                    ListingAnalyticsDayDTO day = list.get(0);
                    assertThat(day.ts()).isEqualTo(today.minusDays(1).toEpochSecond());
                    assertThat(day.impressionsCount()).isEqualTo(10);
                    assertThat(day.viewsCount()).isEqualTo(10);
                    assertThat(day.phoneRequestsCount()).isEqualTo(0);
                    assertThat(day.favouritesCount()).isEqualTo(0);

                    day = list.get(1);
                    assertThat(day.ts()).isEqualTo(today.toEpochSecond());
                    assertThat(day.impressionsCount()).isEqualTo(5);
                    assertThat(day.viewsCount()).isEqualTo(3);
                    assertThat(day.phoneRequestsCount()).isEqualTo(1);
                    assertThat(day.favouritesCount()).isEqualTo(1);
                });
    }

    @Test
    void getAnalyticsReturnsDailyAggregatedDataWithCustomTimeZone() {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        String token = authenticationService.generateToken(userId);
        CarListing listing = carListingService.create(userId).block();

        OffsetDateTime today = OffsetDateTime.now(ZoneId.of("Europe/Kyiv")).withHour(0).withMinute(0).withSecond(0).withNano(0);
        long now = System.currentTimeMillis();
        long sameDay = now - 60_000;
        long yesterday = now - Duration.ofDays(1).toMillis();

        CarListingAnalyticsCounter counter1 = new CarListingAnalyticsCounter();
        counter1.impressionsCount().add(3);
        counter1.viewsCount().add(1);
        counter1.phoneRequestsCount().add(1);
        counter1.favouritesCount().add(1);

        CarListingAnalyticsCounter counter2 = new CarListingAnalyticsCounter();
        counter2.impressionsCount().add(2);
        counter2.viewsCount().add(2);

        CarListingAnalyticsCounter counter3 = new CarListingAnalyticsCounter();
        counter3.impressionsCount().add(10);
        counter3.viewsCount().add(10);

        analyticsRepository.saveAnalytics(now, List.of(Map.entry(listing.id(), counter1))).block();
        analyticsRepository.saveAnalytics(sameDay, List.of(Map.entry(listing.id(), counter2))).block();
        analyticsRepository.saveAnalytics(yesterday, List.of(Map.entry(listing.id(), counter3))).block();

        webTestClient.get()
                .uri("/api/listings/own/" + listing.id() + "/analytics?timezone=Europe/Kyiv")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody(ANALYTICS_LIST_TYPE)
                .value(list -> {
                    assertThat(list).hasSize(2);

                    ListingAnalyticsDayDTO day = list.get(0);
                    assertThat(day.ts()).isEqualTo(today.minusDays(1).toEpochSecond());
                    assertThat(day.impressionsCount()).isEqualTo(10);
                    assertThat(day.viewsCount()).isEqualTo(10);
                    assertThat(day.phoneRequestsCount()).isEqualTo(0);
                    assertThat(day.favouritesCount()).isEqualTo(0);

                    day = list.get(1);
                    assertThat(day.ts()).isEqualTo(today.toEpochSecond());
                    assertThat(day.impressionsCount()).isEqualTo(5);
                    assertThat(day.viewsCount()).isEqualTo(3);
                    assertThat(day.phoneRequestsCount()).isEqualTo(1);
                    assertThat(day.favouritesCount()).isEqualTo(1);
                });
    }

    @Test
    void getAnalyticsExcludesDataOlderThan365Days() {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        String token = authenticationService.generateToken(userId);
        CarListing listing = carListingService.create(userId).block();

        long now = System.currentTimeMillis();
        long tooOld = now - Duration.ofDays(366).toMillis();

        CarListingAnalyticsCounter recent = new CarListingAnalyticsCounter();
        recent.impressionsCount().add(5);

        CarListingAnalyticsCounter old = new CarListingAnalyticsCounter();
        old.impressionsCount().add(99);

        analyticsRepository.saveAnalytics(now, List.of(Map.entry(listing.id(), recent))).block();
        analyticsRepository.saveAnalytics(tooOld, List.of(Map.entry(listing.id(), old))).block();

        webTestClient.get()
                .uri("/api/listings/own/" + listing.id() + "/analytics")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody(ANALYTICS_LIST_TYPE)
                .value(list -> {
                    assertThat(list).hasSize(1);
                    assertThat(list.get(0).impressionsCount()).isEqualTo(5);
                });
    }

    @Test
    void getAnalyticsWithoutTokenReturns401() {
        webTestClient.get()
                .uri("/api/listings/own/1/analytics")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().contentType("application/problem+json")
                .expectBody(ProblemDTO.class)
                .value(problem -> {
                    assertThat(problem.type()).isEqualTo("/problems/unauthorized");
                    assertThat(problem.title()).isEqualTo("Unauthorized");
                    assertThat(problem.status()).isEqualTo(401);
                });
    }

    @Test
    void getAnalyticsNonExistentListingReturns404() {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        String token = authenticationService.generateToken(userId);

        webTestClient.get()
                .uri("/api/listings/own/9999/analytics")
                .header("Authorization", "Bearer " + token)
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
    void getAnalyticsOtherUsersListingReturns403() {
        long userId1 = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        long userId2 = userService.getUserByPhoneNumberOrCreate("+380123456780").block().id();
        String token2 = authenticationService.generateToken(userId2);
        CarListing listing = carListingService.create(userId1).block();

        webTestClient.get()
                .uri("/api/listings/own/" + listing.id() + "/analytics")
                .header("Authorization", "Bearer " + token2)
                .exchange()
                .expectStatus().isForbidden()
                .expectHeader().contentType("application/problem+json")
                .expectBody(ProblemDTO.class)
                .value(problem -> {
                    assertThat(problem.type()).isEqualTo("/problems/access-denied");
                    assertThat(problem.title()).isEqualTo("Access denied");
                    assertThat(problem.status()).isEqualTo(403);
                });
    }
}
