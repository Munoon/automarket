package edu.automarket.listing.controller;

import edu.automarket.AbstractIntegrationTest;
import edu.automarket.TestUtils;
import edu.automarket.common.PageDTO;
import edu.automarket.listing.CarListingService;
import edu.automarket.listing.dto.PublicCarListingItemDTO;
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
        webTestClient.get()
                .uri("/api/listings/public")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void getPublishedListingsReturnsOnlyPublishedListings() {
        long userId = userRepository.save(TestUtils.testUser("pubctrl1")).block().id();
        CarListing draft = carListingService.create(userId).block();
        CarListing published = carListingService.create(userId).block();
        carListingService.updateStatus(published.id(), ListingStatus.PUBLISHED).block();

        webTestClient.get()
                .uri("/api/listings/public")
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
    void getPublishedListingsReturnsCorrectFields() {
        long userId = userRepository.save(TestUtils.testUser("pubctrl2")).block().id();
        CarListing listing = carListingService.create(userId).block();
        carListingService.update(listing.id(), new edu.automarket.listing.dto.UpdateCarListingRequestDTO(
                "Test Car", "A nice car", CarBrand.TOYOTA, null, "Camry",
                null, null, null, 500000L, null, null, null, null, null, null, null, null, null, null
        )).block();
        carListingService.updateStatus(listing.id(), ListingStatus.PUBLISHED).block();

        webTestClient.get()
                .uri("/api/listings/public")
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

        webTestClient.get()
                .uri("/api/listings/public")
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
        carListingService.updateStatus(listing1.id(), ListingStatus.PUBLISHED).block();

        CarListing listing2 = carListingService.create(userId).block();
        carListingService.updateStatus(listing2.id(), ListingStatus.PUBLISHED).block();

        CarListing listing3 = carListingService.create(userId).block();
        carListingService.updateStatus(listing3.id(), ListingStatus.PUBLISHED).block();

        webTestClient.get()
                .uri("/api/listings/public?page=0&size=2")
                .exchange()
                .expectStatus().isOk()
                .expectBody(PAGE_TYPE)
                .value(page -> {
                    assertThat(page.totalElements()).isEqualTo(3);
                    assertThat(page.content()).hasSize(2);
                    assertThat(page.content().get(0).id()).isEqualTo(listing3.id());
                    assertThat(page.content().get(1).id()).isEqualTo(listing2.id());
                });

        webTestClient.get()
                .uri("/api/listings/public?page=1&size=2")
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
