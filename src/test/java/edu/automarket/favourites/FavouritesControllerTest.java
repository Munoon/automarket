package edu.automarket.favourites;

import edu.automarket.AbstractIntegrationTest;
import edu.automarket.authentication.AuthenticationService;
import edu.automarket.common.ProblemDTO;
import edu.automarket.favourites.dto.FavouriteRequestDTO;
import edu.automarket.listing.CarListingRepository;
import edu.automarket.listing.CarListingService;
import edu.automarket.listing.model.CarListing;
import edu.automarket.listing.model.ListingStatus;
import edu.automarket.user.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import static edu.automarket.listing.CarListingTestUtils.UPDATE_CAR_LISTING_REQUEST_DTO;
import static org.assertj.core.api.Assertions.assertThat;

class FavouritesControllerTest extends AbstractIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private UserService userService;

    @Autowired
    private AuthenticationService authenticationService;

    @Autowired
    private CarListingRepository carListingRepository;

    @Autowired
    private CarListingService carListingService;

    @Autowired
    private FavouritesService favouritesService;

    // --- POST /api/favourites ---

    @Test
    void addFavouriteRequiresAuthentication() {
        webTestClient.post()
                .uri("/api/favourites")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new FavouriteRequestDTO(1L))
                .exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().contentType("application/problem+json")
                .expectBody(ProblemDTO.class)
                .value(problem -> {
                    assertThat(problem.type()).isEqualTo("/problems/unauthorized");
                    assertThat(problem.status()).isEqualTo(401);
                });
    }

    @Test
    void addFavouriteSuccessfullyAdds() {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        String token = authenticationService.generateToken(userId);
        CarListing listing = carListingRepository.create(userId, System.currentTimeMillis()).block();

        webTestClient.post()
                .uri("/api/favourites")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new FavouriteRequestDTO(listing.id()))
                .exchange()
                .expectStatus().isOk();

        assertThat(favouritesService.countFavouritesByUser(userId).block()).isEqualTo(1);
    }

    @Test
    void addFavouriteReturnsForbiddenWhenLimitReached() {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        String token = authenticationService.generateToken(userId);

        // generating 100 listing and adding them to the favourites
        for (int i = 0; i < 100; i++) {
            long listingUserId = userService.getUserByPhoneNumberOrCreate("+380111111" + String.format("%03d", i)).block().id();
            CarListing listing = carListingService.create(listingUserId).block();
            listing = carListingService.update(listing, UPDATE_CAR_LISTING_REQUEST_DTO).block();
            carListingService.updateStatus(listing, ListingStatus.PUBLISHED).block();
            favouritesService.addFavourite(userId, listing.id()).block();
        }

        webTestClient.post()
                .uri("/api/favourites")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new FavouriteRequestDTO(1L))
                .exchange()
                .expectStatus().isForbidden()
                .expectHeader().contentType("application/problem+json")
                .expectBody(ProblemDTO.class)
                .value(problem -> {
                    assertThat(problem.type()).isEqualTo("/problems/favourites-limit-reached");
                    assertThat(problem.status()).isEqualTo(403);
                });
    }

    // --- DELETE /api/favourites ---

    @Test
    void removeFavouriteRequiresAuthentication() {
        webTestClient.method(HttpMethod.DELETE)
                .uri("/api/favourites")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new FavouriteRequestDTO(1L))
                .exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().contentType("application/problem+json")
                .expectBody(ProblemDTO.class)
                .value(problem -> {
                    assertThat(problem.type()).isEqualTo("/problems/unauthorized");
                    assertThat(problem.status()).isEqualTo(401);
                });
    }

    @Test
    void removeFavouriteSuccessfullyRemoves() {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        String token = authenticationService.generateToken(userId);
        CarListing listing = carListingRepository.create(userId, System.currentTimeMillis()).block();

        favouritesService.addFavourite(userId, listing.id()).block();

        webTestClient.method(HttpMethod.DELETE)
                .uri("/api/favourites")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new FavouriteRequestDTO(listing.id()))
                .exchange()
                .expectStatus().isOk();

        assertThat(favouritesService.countFavouritesByUser(userId).block()).isEqualTo(0);
    }
}
