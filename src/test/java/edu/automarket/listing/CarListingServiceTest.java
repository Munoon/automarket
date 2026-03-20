package edu.automarket.listing;

import edu.automarket.AbstractIntegrationTest;
import edu.automarket.TestUtils;
import edu.automarket.listing.dto.GetPublishedListingsRequestDTO;
import edu.automarket.listing.dto.OwnCarListingListItemDTO;
import edu.automarket.listing.dto.PublicCarListingDTO;
import edu.automarket.listing.dto.PublicCarListingItemDTO;
import edu.automarket.listing.dto.UpdateCarListingRequestDTO;
import edu.automarket.listing.model.CarBrand;
import edu.automarket.listing.model.CarListing;
import edu.automarket.listing.model.ListingStatus;
import edu.automarket.user.UserRepository;
import edu.automarket.user.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import reactor.test.StepVerifier;

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
        long userId = userService.register(TestUtils.testUser("svcuser1")).block().id();

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
        long userId1 = userService.register(TestUtils.testUser("svcuser1")).block().id();
        long userId2 = userService.register(TestUtils.testUser("svcuser2")).block().id();

        for (int i = 0; i < 30; i++) {
            carListingService.create(userId1).block();
        }

        assertThatThrownBy(() -> carListingService.create(userId1).block())
                .isInstanceOf(ResponseStatusException.class)
                .hasMessage("403 FORBIDDEN \"You have reached the limit of listings per user\"");

        CarListing user2Listing = carListingService.create(userId2).block();
        user2Listing = carListingService.getListingByIdOrThrow(user2Listing.id()).block();
        assertThat(user2Listing).isNotNull();
        assertThat(user2Listing.id()).isGreaterThan(0);
        assertThat(user2Listing.authorUserId()).isEqualTo(userId2);
        assertThat(user2Listing.status()).isEqualTo(ListingStatus.DRAFT);
    }

    @Test
    void getOwnListingsReturnsTotalElementsAndContent() {
        long userId = userService.register(TestUtils.testUser("svcuser2")).block().id();
        CarListing listing1 = carListingService.create(userId).block();
        CarListing listing2 = carListingService.create(userId).block();

        StepVerifier.create(carListingService.getOwnListings(userId, null, 0, 20))
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
        long userId = userService.register(TestUtils.testUser("svcuser3")).block().id();
        carListingService.create(userId).block();

        StepVerifier.create(carListingService.getOwnListings(userId, null, 0, 20))
                .assertNext(page -> assertThat(page.totalElements()).isEqualTo(1))
                .verifyComplete();
    }

    @Test
    void getOwnListingsWithEmptyStatusesReturnsAllListings() {
        long userId = userService.register(TestUtils.testUser("svcuser4")).block().id();
        carListingService.create(userId).block();

        StepVerifier.create(carListingService.getOwnListings(userId, new ListingStatus[]{}, 0, 20))
                .assertNext(page -> assertThat(page.totalElements()).isEqualTo(1))
                .verifyComplete();
    }

    @Test
    void getOwnListingsWithDraftFilterReturnsDraftListings() {
        long userId = userService.register(TestUtils.testUser("svcuser5")).block().id();
        CarListing draft = carListingService.create(userId).block();
        CarListing published = carListingService.create(userId).block();
        carListingService.updateStatus(published, ListingStatus.PUBLISHED).block();

        StepVerifier.create(carListingService.getOwnListings(
                        userId, new ListingStatus[]{ListingStatus.DRAFT}, 0, 20))
                .assertNext(page -> {
                    assertThat(page.totalElements()).isEqualTo(1);
                    assertThat(page.content().get(0).id()).isEqualTo(draft.id());
                    assertThat(page.content().get(0).status()).isEqualTo(ListingStatus.DRAFT);
                })
                .verifyComplete();
    }

    @Test
    void getOwnListingsWithPublishedFilterReturnsOnlyPublishedListings() {
        long userId = userService.register(TestUtils.testUser("svcuser6")).block().id();
        carListingService.create(userId).block();
        CarListing published = carListingService.create(userId).block();
        carListingService.updateStatus(published, ListingStatus.PUBLISHED).block();

        StepVerifier.create(carListingService.getOwnListings(
                        userId, new ListingStatus[]{ListingStatus.PUBLISHED}, 0, 20))
                .assertNext(page -> {
                    assertThat(page.totalElements()).isEqualTo(1);
                    assertThat(page.content().get(0).id()).isEqualTo(published.id());
                    assertThat(page.content().get(0).status()).isEqualTo(ListingStatus.PUBLISHED);
                })
                .verifyComplete();
    }

    @Test
    void getOwnListingsWithMultipleStatusesReturnsMatchingListings() {
        long userId = userService.register(TestUtils.testUser("svcuser10")).block().id();
        CarListing draft = carListingService.create(userId).block();
        CarListing published = carListingService.create(userId).block();
        CarListing archived = carListingService.create(userId).block();
        carListingService.updateStatus(published, ListingStatus.PUBLISHED).block();
        carListingService.updateStatus(archived, ListingStatus.ARCHIVED).block();

        StepVerifier.create(carListingService.getOwnListings(
                        userId, new ListingStatus[]{ListingStatus.DRAFT, ListingStatus.PUBLISHED}, 0, 20))
                .assertNext(page -> {
                    assertThat(page.totalElements()).isEqualTo(2);
                    assertThat(page.content()).extracting(OwnCarListingListItemDTO::status)
                            .containsExactlyInAnyOrder(ListingStatus.DRAFT, ListingStatus.PUBLISHED);
                })
                .verifyComplete();
    }

    @Test
    void getOwnListingsPaginatesCorrectly() {
        long userId = userService.register(TestUtils.testUser("svcuser7")).block().id();
        CarListing listing1 = carListingService.create(userId).block();
        CarListing listing2 = carListingService.create(userId).block();
        CarListing listing3 = carListingService.create(userId).block();

        StepVerifier.create(carListingService.getOwnListings(userId, null, 0, 2))
                .assertNext(page -> {
                    assertThat(page.totalElements()).isEqualTo(3);
                    assertThat(page.content()).hasSize(2);
                    assertThat(page.content().get(0).id()).isEqualTo(listing3.id());
                    assertThat(page.content().get(1).id()).isEqualTo(listing2.id());
                })
                .verifyComplete();

        StepVerifier.create(carListingService.getOwnListings(userId, null, 1, 2))
                .assertNext(page -> {
                    assertThat(page.totalElements()).isEqualTo(3);
                    assertThat(page.content()).hasSize(1);
                    assertThat(page.content().get(0).id()).isEqualTo(listing1.id());
                })
                .verifyComplete();
    }

    @Test
    void deleteRemovesListing() {
        long userId = userService.register(TestUtils.testUser("svcuser11")).block().id();
        CarListing listing = carListingService.create(userId).block();

        carListingService.delete(listing.id()).block();

        StepVerifier.create(carListingService.getListingByIdOrThrow(listing.id()))
                .expectErrorMatches(e -> e instanceof ResponseStatusException ex
                        && ex.getStatusCode() == HttpStatus.NOT_FOUND)
                .verify();
    }

    @Test
    void updateStatusPersistsNewStatus() {
        long userId = userService.register(TestUtils.testUser("svcuser8a")).block().id();
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
        long userId = userService.register(TestUtils.testUser("svcuser8a")).block().id();
        CarListing created = carListingService.create(userId).block();

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
        long userId = userService.register(TestUtils.testUser("svcuser17")).block().id();
        CarListing created = carListingService.create(userId).block();

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
        long userId = userService.register(TestUtils.testUser("svcuser17")).block().id();
        CarListing listing = carListingService.create(userId).block();

        carListingService.updateStatus(listing, ListingStatus.PUBLISHED).block();
        CarListing publishedListing = carListingService.getListingByIdOrThrow(listing.id()).block();
        assertThat(publishedListing).isNotNull();
        assertThat(publishedListing.status()).isEqualTo(ListingStatus.PUBLISHED);

        carListingService.updateStatus(publishedListing, ListingStatus.ARCHIVED).block();
        CarListing archivedListing = carListingService.getListingByIdOrThrow(listing.id()).block();
        assertThat(archivedListing).isNotNull();
        assertThat(archivedListing.status()).isEqualTo(ListingStatus.ARCHIVED);

        assertThatThrownBy(() -> carListingService.updateStatus(archivedListing, ListingStatus.PUBLISHED).block())
                .isInstanceOf(ResponseStatusException.class)
                .hasMessage("400 BAD_REQUEST \"Listing publishing cooldown in progress\"");

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
    void getPublishedListingByIdOrThrowReturnsPublishedListing() {
        long userId = userService.register(TestUtils.testUser("svcuser18")).block().id();
        CarListing listing = carListingService.create(userId).block();
        carListingService.updateStatus(listing, ListingStatus.PUBLISHED).block();

        StepVerifier.create(carListingService.getPublishedListingByIdOrThrow(listing.id()))
                .assertNext(dto -> assertThat(dto.id()).isEqualTo(listing.id()))
                .verifyComplete();
    }

    @Test
    void getPublishedListingByIdOrThrowThrowsNotFoundForDraftListing() {
        long userId = userService.register(TestUtils.testUser("svcuser19")).block().id();
        CarListing listing = carListingService.create(userId).block();

        StepVerifier.create(carListingService.getPublishedListingByIdOrThrow(listing.id()))
                .expectErrorMatches(e -> e instanceof ResponseStatusException ex
                        && ex.getStatusCode() == HttpStatus.NOT_FOUND)
                .verify();
    }

    @Test
    void getPublishedListingByIdOrThrowThrowsNotFoundForNonExistentId() {
        StepVerifier.create(carListingService.getPublishedListingByIdOrThrow(9999))
                .expectErrorMatches(e -> e instanceof ResponseStatusException ex
                        && ex.getStatusCode() == HttpStatus.NOT_FOUND)
                .verify();
    }

    @Test
    void getListingByIdOrThrowReturnsListing() {
        long userId = userService.register(TestUtils.testUser("svcuser8")).block().id();
        CarListing created = carListingService.create(userId).block();

        StepVerifier.create(carListingService.getListingByIdOrThrow(created.id()))
                .assertNext(listing -> assertThat(listing.id()).isEqualTo(created.id()))
                .verifyComplete();
    }

    @Test
    void getListingByIdOrThrowThrowsNotFoundForMissingId() {
        StepVerifier.create(carListingService.getListingByIdOrThrow(9999))
                .expectErrorMatches(e -> e instanceof ResponseStatusException ex
                        && ex.getStatusCode() == HttpStatus.NOT_FOUND)
                .verify();
    }

    @Test
    void getPublishedListingsReturnsOnlyPublishedListings() {
        long userId = userService.register(TestUtils.testUser("svcuser12")).block().id();
        carListingService.create(userId).block(); // draft
        CarListing published = carListingService.create(userId).block();
        carListingService.updateStatus(published, ListingStatus.PUBLISHED).block();

        StepVerifier.create(carListingService.getPublishedListings(new GetPublishedListingsRequestDTO()))
                    .assertNext(page -> {
                    assertThat(page.totalElements()).isEqualTo(1);
                    assertThat(page.content()).hasSize(1);
                    assertThat(page.content().get(0).id()).isEqualTo(published.id());
                })
                    .verifyComplete();
    }

    @Test
    void getPublishedListingsFiltersOutListingsPublishedAfterAnchor() {
        long userId = userService.register(TestUtils.testUser("svcuser16")).block().id();
        CarListing listing = carListingService.create(userId).block();
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
        long userId = userService.register(TestUtils.testUser("svcuser13")).block().id();
        CarListing listing = carListingService.create(userId).block();
        carListingService.update(listing.id(), new UpdateCarListingRequestDTO(
                "Sport Car", "Fast and furious", CarBrand.CUSTOM, "Batmobile", "Dark Knight",
                null, null, null, 999999L, null, null, null, null, null, null, null, null, null, null
        )).block();
        carListingService.updateStatus(listing, ListingStatus.PUBLISHED).block();

        StepVerifier.create(carListingService.getPublishedListings(new GetPublishedListingsRequestDTO()))
                .assertNext(page -> {
                    assertThat(page.totalElements()).isEqualTo(1);
                    PublicCarListingItemDTO dto = page.content().get(0);
                    assertThat(dto.id()).isEqualTo(listing.id());
                    assertThat(dto.title()).isEqualTo("Sport Car");
                    assertThat(dto.description()).isEqualTo("Fast and furious");
                    assertThat(dto.price()).isEqualTo(999999L);
                    assertThat(dto.brand()).isEqualTo(CarBrand.CUSTOM);
                    assertThat(dto.customBrandName()).isEqualTo("Batmobile");
                    assertThat(dto.model()).isEqualTo("Dark Knight");
                })
                .verifyComplete();
    }

    @Test
    void getPublishedListingsReturnsEmptyWhenNonePublished() {
        long userId = userService.register(TestUtils.testUser("svcuser14")).block().id();
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
        long userId = userService.register(TestUtils.testUser("svcuser15")).block().id();
        CarListing listing1 = carListingService.create(userId).block();
        CarListing listing2 = carListingService.create(userId).block();
        CarListing listing3 = carListingService.create(userId).block();
        carListingService.updateStatus(listing1, ListingStatus.PUBLISHED).block();
        carListingService.updateStatus(listing3, ListingStatus.PUBLISHED).block();
        carListingService.updateStatus(listing2, ListingStatus.PUBLISHED).block();

        GetPublishedListingsRequestDTO request = new GetPublishedListingsRequestDTO();
        request.setPage(0);
        request.setSize(2);
        StepVerifier.create(carListingService.getPublishedListings(request))
                    .assertNext(page -> {
                        assertThat(page.totalElements()).isEqualTo(3);
                        assertThat(page.content()).hasSize(2);
                        assertThat(page.content().get(0).id()).isEqualTo(listing2.id());
                        assertThat(page.content().get(1).id()).isEqualTo(listing3.id());
                    })
                    .verifyComplete();

        request = new GetPublishedListingsRequestDTO();
        request.setPage(1);
        request.setSize(2);
        StepVerifier.create(carListingService.getPublishedListings(request))
                .assertNext(page -> {
                    assertThat(page.totalElements()).isEqualTo(3);
                    assertThat(page.content()).hasSize(1);
                    assertThat(page.content().get(0).id()).isEqualTo(listing1.id());
                })
                .verifyComplete();
    }

    @Test
    void getPublishedListingAuthorPhoneOrThrowReturnsPhone() {
        long userId = userService.register(TestUtils.testUser("svcuser20")).block().id();
        CarListing listing = carListingService.create(userId).block();
        carListingService.updateStatus(listing, ListingStatus.PUBLISHED).block();

        StepVerifier.create(carListingService.getPublishedListingAuthorPhoneOrThrow(listing.id()))
                .assertNext(dto -> assertThat(dto.phoneNumber()).isEqualTo("+123456789012"))
                .verifyComplete();
    }

    @Test
    void getPublishedListingAuthorPhoneOrThrowThrowsNotFoundForDraftListing() {
        long userId = userService.register(TestUtils.testUser("svcuser21")).block().id();
        CarListing listing = carListingService.create(userId).block();

        StepVerifier.create(carListingService.getPublishedListingAuthorPhoneOrThrow(listing.id()))
                .expectErrorMatches(e -> e instanceof ResponseStatusException ex
                        && ex.getStatusCode() == HttpStatus.NOT_FOUND)
                .verify();
    }

    @Test
    void getPublishedListingAuthorPhoneOrThrowThrowsNotFoundForNonExistentId() {
        StepVerifier.create(carListingService.getPublishedListingAuthorPhoneOrThrow(9999))
                .expectErrorMatches(e -> e instanceof ResponseStatusException ex
                        && ex.getStatusCode() == HttpStatus.NOT_FOUND)
                .verify();
    }

    @Test
    void updateReturnsUpdatedListing() {
        long userId = userService.register(TestUtils.testUser("svcuser9")).block().id();
        CarListing created = carListingService.create(userId).block();

        var request = new UpdateCarListingRequestDTO(
                "Updated Title", null, CarBrand.TOYOTA, null, "Corolla", null,
                null, null, null, null, null, null, null, null, null, null, null, null, null
        );

        carListingService.update(created.id(), request).block();
        StepVerifier.create(carListingService.getListingByIdOrThrow(created.id()))
                .assertNext(listing -> {
                    assertThat(listing.id()).isEqualTo(created.id());
                    assertThat(listing.title()).isEqualTo("Updated Title");
                    assertThat(listing.brand()).isEqualTo(CarBrand.TOYOTA);
                    assertThat(listing.model()).isEqualTo("Corolla");
                })
                .verifyComplete();
    }
}
