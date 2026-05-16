package edu.automarket.favourites;

import edu.automarket.AbstractIntegrationTest;
import edu.automarket.common.ApiException;
import edu.automarket.listing.CarListingRepository;
import edu.automarket.listing.CarListingService;
import edu.automarket.listing.model.CarListing;
import edu.automarket.listing.model.ListingStatus;
import edu.automarket.user.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static edu.automarket.listing.CarListingTestUtils.UPDATE_CAR_LISTING_REQUEST_DTO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;

class FavouritesServiceTest extends AbstractIntegrationTest {

    @Autowired
    private FavouritesService favouritesService;

    @Autowired
    private UserService userService;

    @Autowired
    private CarListingService carListingService;

    @Test
    void addFavouritePersistsEntry() {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        CarListing listing = carListingService.create(userId).block();

        StepVerifier.create(favouritesService.addFavourite(userId, listing.id()))
                .verifyComplete();

        StepVerifier.create(favouritesService.countFavouritesByUser(userId))
                .assertNext(count -> assertThat(count).isEqualTo(1))
                .verifyComplete();
    }

    @Test
    void addFavouriteThrowsForbiddenWhenLimitReached() {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();

        // generating 100 listing and adding them to the favourite
        for (int i = 0; i < 100; i++) {
            long listingUserId = userService.getUserByPhoneNumberOrCreate("+380111111" + String.format("%03d", i)).block().id();
            CarListing listing = carListingService.create(listingUserId).block();
            listing = carListingService.update(listing, UPDATE_CAR_LISTING_REQUEST_DTO).block();
            carListingService.updateStatus(listing, ListingStatus.PUBLISHED).block();
            favouritesService.addFavourite(userId, listing.id()).block();
        }

        CarListing listing = carListingService.create(userId).block();

        StepVerifier.create(favouritesService.addFavourite(userId, listing.id()))
                .expectErrorMatches(e -> e instanceof ApiException ex
                        && ex.getStatus() == HttpStatus.FORBIDDEN)
                .verify();
    }

    @Test
    void removeFavouriteDeletesEntry() {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        CarListing listing = carListingService.create(userId).block();

        favouritesService.addFavourite(userId, listing.id()).block();

        StepVerifier.create(favouritesService.removeFavourite(userId, listing.id()))
                .verifyComplete();

        StepVerifier.create(favouritesService.countFavouritesByUser(userId))
                .assertNext(count -> assertThat(count).isEqualTo(0))
                .verifyComplete();
    }

    @Test
    void removeFavouriteByListingIdDeletesEntry() {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        CarListing listing = carListingService.create(userId).block();

        favouritesService.addFavourite(userId, listing.id()).block();

        StepVerifier.create(favouritesService.removeFavouritesByListingId(listing.id()))
                .verifyComplete();

        StepVerifier.create(favouritesService.countFavouritesByUser(userId))
                .assertNext(count -> assertThat(count).isEqualTo(0))
                .verifyComplete();
    }

    @Test
    void countFavouritesByUserReturnsCorrectCount() {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        CarListing listing1 = carListingService.create(userId).block();
        CarListing listing2 = carListingService.create(userId).block();

        favouritesService.addFavourite(userId, listing1.id()).block();
        favouritesService.addFavourite(userId, listing2.id()).block();

        StepVerifier.create(favouritesService.countFavouritesByUser(userId))
                .assertNext(count -> assertThat(count).isEqualTo(2))
                .verifyComplete();
    }
}
