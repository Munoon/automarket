package edu.automarket.favourites;

import edu.automarket.AbstractIntegrationTest;
import edu.automarket.listing.CarListingRepository;
import edu.automarket.listing.CarListingService;
import edu.automarket.listing.model.CarListing;
import edu.automarket.user.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

class FavouritesRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private FavouritesRepository favouritesRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private CarListingService carListingService;

    @Test
    void addFavouriteInsertsEntry() {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        CarListing listing = carListingService.create(userId).block();

        StepVerifier.create(favouritesRepository.addFavourite(userId, listing.id()))
                .verifyComplete();

        StepVerifier.create(favouritesRepository.countFavourites(userId))
                .assertNext(count -> assertThat(count).isEqualTo(1))
                .verifyComplete();
    }

    @Test
    void addFavouriteTwiceDoesNotThrow() {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        CarListing listing = carListingService.create(userId).block();

        StepVerifier.create(favouritesRepository.addFavourite(userId, listing.id()))
                .verifyComplete();
        StepVerifier.create(favouritesRepository.addFavourite(userId, listing.id()))
                .verifyComplete();

        StepVerifier.create(favouritesRepository.countFavourites(userId))
                .assertNext(count -> assertThat(count).isEqualTo(1))
                .verifyComplete();
    }

    @Test
    void removeFavouriteDeletesEntry() {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        CarListing listing = carListingService.create(userId).block();

        favouritesRepository.addFavourite(userId, listing.id()).block();

        StepVerifier.create(favouritesRepository.removeFavourite(userId, listing.id()))
                .verifyComplete();

        StepVerifier.create(favouritesRepository.countFavourites(userId))
                .assertNext(count -> assertThat(count).isEqualTo(0))
                .verifyComplete();
    }

    @Test
    void removeFavouriteCompletesEvenWhenEntryDoesNotExist() {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();

        StepVerifier.create(favouritesRepository.removeFavourite(userId, 9999L))
                .verifyComplete();
    }

    @Test
    void removeFavouritesByListingIdDeletesEntry() {
        long userId1 = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        long userId2 = userService.getUserByPhoneNumberOrCreate("+380123456780").block().id();
        CarListing listing1 = carListingService.create(userId1).block();
        CarListing listing2 = carListingService.create(userId1).block();

        favouritesRepository.addFavourite(userId1, listing1.id()).block();
        favouritesRepository.addFavourite(userId1, listing2.id()).block();
        favouritesRepository.addFavourite(userId2, listing1.id()).block();

        StepVerifier.create(favouritesRepository.removeFavouritesByListingId(listing1.id()))
                .verifyComplete();

        StepVerifier.create(favouritesRepository.countFavourites(userId1))
                .assertNext(count -> assertThat(count).isEqualTo(1))
                .verifyComplete();
        StepVerifier.create(favouritesRepository.countFavourites(userId2))
                .assertNext(count -> assertThat(count).isEqualTo(0))
                .verifyComplete();

        StepVerifier.create(favouritesRepository.removeFavouritesByListingId(listing2.id()))
                .verifyComplete();

        StepVerifier.create(favouritesRepository.countFavourites(userId1))
                .assertNext(count -> assertThat(count).isEqualTo(0))
                .verifyComplete();
        StepVerifier.create(favouritesRepository.countFavourites(userId2))
                .assertNext(count -> assertThat(count).isEqualTo(0))
                .verifyComplete();
    }

    @Test
    void removeFavouritesByListingIdCompletesEvenWhenEntryDoesNotExist() {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        CarListing listing = carListingService.create(userId).block();

        StepVerifier.create(favouritesRepository.removeFavouritesByListingId(listing.id()))
                .verifyComplete();
    }

    @Test
    void countFavouritesReturnsZeroForUserWithNoFavourites() {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();

        StepVerifier.create(favouritesRepository.countFavourites(userId))
                .assertNext(count -> assertThat(count).isEqualTo(0))
                .verifyComplete();
    }

    @Test
    void countFavouritesOnlyCountsForSpecificUser() {
        long userId1 = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        long userId2 = userService.getUserByPhoneNumberOrCreate("+380123456780").block().id();
        CarListing listing1 = carListingService.create(userId1).block();
        CarListing listing2 = carListingService.create(userId1).block();

        favouritesRepository.addFavourite(userId1, listing1.id()).block();
        favouritesRepository.addFavourite(userId1, listing2.id()).block();
        favouritesRepository.addFavourite(userId2, listing1.id()).block();

        StepVerifier.create(favouritesRepository.countFavourites(userId1))
                .assertNext(count -> assertThat(count).isEqualTo(2))
                .verifyComplete();

        StepVerifier.create(favouritesRepository.countFavourites(userId2))
                .assertNext(count -> assertThat(count).isEqualTo(1))
                .verifyComplete();
    }
}
