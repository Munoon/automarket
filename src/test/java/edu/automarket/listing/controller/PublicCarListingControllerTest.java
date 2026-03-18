package edu.automarket.listing.controller;

import edu.automarket.AbstractIntegrationTest;
import edu.automarket.TestUtils;
import edu.automarket.common.PageDTO;
import edu.automarket.listing.CarListingService;
import edu.automarket.listing.dto.PublicCarListingItemDTO;
import edu.automarket.listing.dto.UpdateCarListingRequestDTO;
import edu.automarket.listing.model.CarBrand;
import edu.automarket.listing.model.CarListing;
import edu.automarket.listing.model.ListingStatus;
import edu.automarket.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

class PublicCarListingControllerTest extends AbstractIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CarListingService carListingService;

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
    void getPublishedListingsWithoutPublishedBeforeReturns400() {
        webTestClient.get()
                .uri("/api/listings/public")
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void getPublishedListingsReturnsOnlyPublishedListings() {
        long userId = userRepository.save(TestUtils.testUser("pubctrl1")).block().id();
        carListingService.create(userId).block(); // draft
        CarListing published = carListingService.create(userId).block();
        carListingService.updateStatus(published, ListingStatus.PUBLISHED).block();

        long now = System.currentTimeMillis();
        webTestClient.get()
                .uri("/api/listings/public?publishedBefore=" + now)
                .exchange()
                .expectStatus().isOk()
                .expectBody(PAGE_TYPE)
                .value(page -> {
                    assertThat(page.totalElements()).isEqualTo(1);
                    assertThat(page.content()).hasSize(1);
                    assertThat(page.content().get(0).id()).isEqualTo(published.id());
                });
    }

    @Test
    void getPublishedListingsFiltersOutListingsPublishedAfterAnchor() {
        long userId = userRepository.save(TestUtils.testUser("pubctrl5")).block().id();
        CarListing listing = carListingService.create(userId).block();
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
        long userId = userRepository.save(TestUtils.testUser("pubctrl2")).block().id();
        CarListing listing = carListingService.create(userId).block();
        carListingService.update(listing.id(), new UpdateCarListingRequestDTO(
                "Test Car", "A nice car", CarBrand.TOYOTA, null, "Camry",
                null, null, null, 500000L, null, null, null, null, null, null, null, null, null, null
        )).block();
        carListingService.updateStatus(listing, ListingStatus.PUBLISHED).block();

        long now = System.currentTimeMillis();
        webTestClient.get()
                .uri("/api/listings/public?publishedBefore=" + now)
                .exchange()
                .expectStatus().isOk()
                .expectBody(PAGE_TYPE)
                .value(page -> {
                    assertThat(page.totalElements()).isEqualTo(1);
                    PublicCarListingItemDTO dto = page.content().get(0);
                    assertThat(dto.id()).isEqualTo(listing.id());
                    assertThat(dto.title()).isEqualTo("Test Car");
                    assertThat(dto.description()).isEqualTo("A nice car");
                    assertThat(dto.price()).isEqualTo(500000L);
                    assertThat(dto.brand()).isEqualTo(CarBrand.TOYOTA);
                    assertThat(dto.customBrandName()).isNull();
                    assertThat(dto.model()).isEqualTo("Camry");
                });
    }

    @Test
    void getPublishedListingsReturnsEmptyWhenNonePublished() {
        long userId = userRepository.save(TestUtils.testUser("pubctrl3")).block().id();
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
        long userId = userRepository.save(TestUtils.testUser("pubctrl4")).block().id();

        CarListing listing1 = carListingService.create(userId).block();
        CarListing listing2 = carListingService.create(userId).block();
        CarListing listing3 = carListingService.create(userId).block();

        carListingService.updateStatus(listing1, ListingStatus.PUBLISHED).block();
        carListingService.updateStatus(listing3, ListingStatus.PUBLISHED).block();
        carListingService.updateStatus(listing2, ListingStatus.PUBLISHED).block();

        long anchor = System.currentTimeMillis();

        webTestClient.get()
                .uri("/api/listings/public?publishedBefore=" + anchor + "&page=0&size=2")
                .exchange()
                .expectStatus().isOk()
                .expectBody(PAGE_TYPE)
                .value(page -> {
                    assertThat(page.totalElements()).isEqualTo(3);
                    assertThat(page.content()).hasSize(2);
                    assertThat(page.content().get(0).id()).isEqualTo(listing2.id());
                    assertThat(page.content().get(1).id()).isEqualTo(listing3.id());
                });

        webTestClient.get()
                .uri("/api/listings/public?publishedBefore=" + anchor + "&page=1&size=2")
                .exchange()
                .expectStatus().isOk()
                .expectBody(PAGE_TYPE)
                .value(page -> {
                    assertThat(page.totalElements()).isEqualTo(3);
                    assertThat(page.content()).hasSize(1);
                    assertThat(page.content().get(0).id()).isEqualTo(listing1.id());
                });
    }
}
