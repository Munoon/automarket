package edu.automarket.listing.controller;

import edu.automarket.AbstractIntegrationTest;
import edu.automarket.TestUtils;
import edu.automarket.authentication.AuthenticationService;
import edu.automarket.common.PageDTO;
import edu.automarket.listing.model.CarListing;
import edu.automarket.listing.CarListingService;
import edu.automarket.listing.dto.OwnCarListingListItemDTO;
import edu.automarket.listing.dto.OwnCarListingDTO;
import edu.automarket.listing.dto.UpdateCarListingRequestDTO;
import edu.automarket.listing.dto.UpdateListingStatusRequestDTO;
import edu.automarket.listing.model.CarBrand;
import edu.automarket.listing.model.ListingStatus;
import edu.automarket.user.UserRepository;
import edu.automarket.user.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.test.StepVerifier;

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

    private static final ParameterizedTypeReference<PageDTO<OwnCarListingListItemDTO>> PAGE_TYPE =
            new ParameterizedTypeReference<>() {};

    // --- POST /api/listings/own ---

    @Test
    void createWithValidTokenReturns201WithDraftListing() {
        long userId = userService.register(TestUtils.testUser("ctrluser1")).block().id();
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
                .expectStatus().isUnauthorized();
    }

    @Test
    void createCountLimit() {
        long userId1 = userService.register(TestUtils.testUser("ctrluser1")).block().id();
        String token1 = authenticationService.generateToken(userId1);

        long userId2 = userService.register(TestUtils.testUser("ctrluser2")).block().id();
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
                     .expectStatus().isForbidden();

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
        long userId = userService.register(TestUtils.testUser("ctrluser2")).block().id();
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
                .expectStatus().isUnauthorized();
    }

    @Test
    void getOwnListingsWithDraftStatusFilterReturnsDraftListings() {
        long userId = userService.register(TestUtils.testUser("ctrluser3")).block().id();
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
        long userId = userService.register(TestUtils.testUser("ctrluser4")).block().id();
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
        long userId = userService.register(TestUtils.testUser("ctrluser14")).block().id();
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
        long userId = userService.register(TestUtils.testUser("ctrluser5")).block().id();
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
        long userId1 = userService.register(TestUtils.testUser("ctrluser6")).block().id();
        long userId2 = userService.register(TestUtils.testUser("ctrluser7")).block().id();
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
        long userId = userService.register(TestUtils.testUser("ctrluser8a")).block().id();
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
                .expectStatus().isUnauthorized();
    }

    @Test
    void getByIdNonExistentReturns404() {
        long userId = userService.register(TestUtils.testUser("ctrluser8b")).block().id();
        String token = authenticationService.generateToken(userId);

        webTestClient.get()
                .uri("/api/listings/own/9999")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void getByIdOtherUsersListingReturns403() {
        long userId1 = userService.register(TestUtils.testUser("ctrluser8c")).block().id();
        long userId2 = userService.register(TestUtils.testUser("ctrluser8d")).block().id();
        String token2 = authenticationService.generateToken(userId2);
        CarListing listing = carListingService.create(userId1).block();

        webTestClient.get()
                .uri("/api/listings/own/" + listing.id())
                .header("Authorization", "Bearer " + token2)
                .exchange()
                .expectStatus().isForbidden();
    }

    // --- PATCH /api/listings/own/{id}/status ---

    @Test
    void updateStatusWithValidTokenReturns204AndPersists() {
        long userId = userService.register(TestUtils.testUser("ctrluser8e")).block().id();
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
        long userId = userService.register(TestUtils.testUser("ctrluser8e")).block().id();
        String token = authenticationService.generateToken(userId);
        CarListing listing = carListingService.create(userId).block();

        webTestClient.patch()
                     .uri("/api/listings/own/" + listing.id() + "/status")
                     .header("Authorization", "Bearer " + token)
                     .contentType(MediaType.APPLICATION_JSON)
                     .bodyValue(new UpdateListingStatusRequestDTO(ListingStatus.DRAFT))
                     .exchange()
                     .expectStatus().isBadRequest();
    }

    @Test
    void updateStatusWithoutTokenReturns401() {
        webTestClient.patch()
                .uri("/api/listings/own/1/status")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new UpdateListingStatusRequestDTO(ListingStatus.PUBLISHED))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void updateStatusNonExistentListingReturns404() {
        long userId = userService.register(TestUtils.testUser("ctrluser8f")).block().id();
        String token = authenticationService.generateToken(userId);

        webTestClient.patch()
                .uri("/api/listings/own/9999/status")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new UpdateListingStatusRequestDTO(ListingStatus.PUBLISHED))
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void updateStatusOtherUsersListingReturns403() {
        long userId1 = userService.register(TestUtils.testUser("ctrluser8g")).block().id();
        long userId2 = userService.register(TestUtils.testUser("ctrluser8h")).block().id();
        String token2 = authenticationService.generateToken(userId2);
        CarListing listing = carListingService.create(userId1).block();

        webTestClient.patch()
                .uri("/api/listings/own/" + listing.id() + "/status")
                .header("Authorization", "Bearer " + token2)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new UpdateListingStatusRequestDTO(ListingStatus.PUBLISHED))
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void updateStatusCooldownOnRepublish() throws InterruptedException {
        long userId = userService.register(TestUtils.testUser("ctrluser8e")).block().id();
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
                     .expectStatus().isBadRequest();

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
        long userId = userService.register(TestUtils.testUser("ctrluser8")).block().id();
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
                .expectStatus().isUnauthorized();
    }

    @Test
    void updateNonExistentListingReturns404() {
        long userId = userService.register(TestUtils.testUser("ctrluser9")).block().id();
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
                .expectStatus().isNotFound();
    }

    @Test
    void updateOtherUsersListingReturns403() {
        long userId1 = userService.register(TestUtils.testUser("ctrluser10")).block().id();
        long userId2 = userService.register(TestUtils.testUser("ctrluser11")).block().id();
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
                .expectStatus().isForbidden();
    }

    @Test
    void updateWithInvalidFieldReturns400() {
        long userId = userService.register(TestUtils.testUser("ctrluser12")).block().id();
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
                .expectStatus().isBadRequest();
    }

    @Test
    void updateWithCustomBrandNameForNonCustomBrandReturns400() {
        long userId = userService.register(TestUtils.testUser("ctrluser13")).block().id();
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
                .expectStatus().isBadRequest();
    }

    // --- DELETE /api/listings/own/{id} ---

    @Test
    void deleteWithValidTokenReturns204AndRemovesListing() {
        long userId = userService.register(TestUtils.testUser("ctrluser15")).block().id();
        String token = authenticationService.generateToken(userId);
        CarListing listing = carListingService.create(userId).block();

        webTestClient.delete()
                .uri("/api/listings/own/" + listing.id())
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isNoContent()
                .expectBody().isEmpty();

        StepVerifier.create(carListingService.getListingByIdOrThrow(listing.id()))
                .expectErrorMatches(e -> e instanceof ResponseStatusException ex
                        && ex.getStatusCode() == HttpStatus.NOT_FOUND)
                .verify();
    }

    @Test
    void deleteWithoutTokenReturns401() {
        webTestClient.delete()
                .uri("/api/listings/own/1")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void deleteNonExistentListingReturns404() {
        long userId = userService.register(TestUtils.testUser("ctrluser16")).block().id();
        String token = authenticationService.generateToken(userId);

        webTestClient.delete()
                .uri("/api/listings/own/9999")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void deleteOtherUsersListingReturns403() {
        long userId1 = userService.register(TestUtils.testUser("ctrluser17")).block().id();
        long userId2 = userService.register(TestUtils.testUser("ctrluser18")).block().id();
        String token2 = authenticationService.generateToken(userId2);
        CarListing listing = carListingService.create(userId1).block();

        webTestClient.delete()
                .uri("/api/listings/own/" + listing.id())
                .header("Authorization", "Bearer " + token2)
                .exchange()
                .expectStatus().isForbidden();
    }
}
