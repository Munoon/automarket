package edu.automarket.listing;

import edu.automarket.common.ApiException;
import edu.automarket.common.PageDTO;
import edu.automarket.favourites.FavouritesService;
import edu.automarket.listing.dto.AuthorPhoneDTO;
import edu.automarket.listing.dto.CarListingPromotionPeriod;
import edu.automarket.listing.dto.GetPublishedListingsRequestDTO;
import edu.automarket.listing.dto.OwnCarListingListItemDTO;
import edu.automarket.listing.dto.PublicCarListingDTO;
import edu.automarket.listing.dto.PublicCarListingItemDTO;
import edu.automarket.listing.dto.UpdateCarListingRequestDTO;
import edu.automarket.listing.model.CarListing;
import edu.automarket.listing.model.ListingStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;

@Service
public class CarListingService {
    private final CarListingRepository carListingRepository;
    private final FavouritesService favouritesService;
    private final long listingRepublishCooldownMS;
    private final int listingsCountPerAuthorLimit;

    public CarListingService(CarListingRepository carListingRepository,
                             FavouritesService favouritesService,
                             @Value("${app.listing.republishCooldown:3d}") Duration listingRepublishCooldouwn,
                             @Value("${app.listing.countLimitPerAuthor:30}") int listingsCountPerAuthorLimit) {
        this.carListingRepository = carListingRepository;
        this.favouritesService = favouritesService;
        this.listingRepublishCooldownMS = listingRepublishCooldouwn.toMillis();
        this.listingsCountPerAuthorLimit = listingsCountPerAuthorLimit;
    }

    public Mono<CarListing> create(long authorUserId) {
        return carListingRepository.countByUserIdAndStatuses(authorUserId)
                .flatMap(count -> {
                    if (count >= listingsCountPerAuthorLimit) {
                        throw new ApiException(
                                HttpStatus.FORBIDDEN, "/problems/listing-limit-reached", "You have reached the limit of listings per user");
                    }
                    return carListingRepository.create(authorUserId, System.currentTimeMillis());
                });
    }

    public Mono<PageDTO<OwnCarListingListItemDTO>> getOwnListings(long userId, int offset, int size) {
        return Mono.zip(
                getOwnListingsCount(userId),
                carListingRepository.findByUserId(userId, offset, size).collectList()
        ).map(tuple -> new PageDTO<>(tuple.getT2(), tuple.getT1()));
    }

    public Mono<Long> getOwnListingsCount(long userId) {
        return carListingRepository.countByUserIdAndStatuses(userId);
    }

    public Mono<CarListing> getListingByIdOrThrow(long id) {
        return carListingRepository.findById(id)
                .switchIfEmpty(Mono.error(new ApiException(HttpStatus.NOT_FOUND, "/problems/listing-not-found", "Listing not found")));
    }

    public Mono<Void> delete(long id) {
        return carListingRepository.deleteById(id);
    }

    public Mono<CarListing> updateStatus(CarListing listing, ListingStatus newStatus) {
        long publishedAt = newStatus == ListingStatus.PUBLISHED
                ? System.currentTimeMillis()
                : listing.publishedAt();

        if (newStatus == ListingStatus.PUBLISHED) {
            if (listing.publishedAt() > 0 && publishedAt - listing.publishedAt() < listingRepublishCooldownMS) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "/problems/listing-publish-cooldown",
                        "Listing publishing cooldown in progress");
            }
            listing.validatePublishedListingFields();
        } else if (listing.status() == ListingStatus.PUBLISHED) { // unpublish
            favouritesService.removeFavouritesByListingId(listing.id())
                    .subscribeOn(Schedulers.boundedElastic())
                    .subscribe();
        }

        CarListing updatedListing = listing.withStatus(newStatus, publishedAt);
        return carListingRepository.update(updatedListing).thenReturn(updatedListing);
    }

    public Mono<CarListing> update(CarListing carListing, UpdateCarListingRequestDTO request) {
        CarListing updatedListing = carListing.update(request);

        if (updatedListing.status() == ListingStatus.PUBLISHED) {
            updatedListing.validatePublishedListingFields();
        }

        return carListingRepository.update(updatedListing).thenReturn(updatedListing);
    }

    public Mono<PublicCarListingDTO> getPublishedListingByIdOrThrow(long id, Long userId) {
        return carListingRepository.findPublishedById(id, userId)
                .switchIfEmpty(Mono.error(new ApiException(HttpStatus.NOT_FOUND, "/problems/listing-not-found", "Listing not found")));
    }

    public Mono<AuthorPhoneDTO> getPublishedListingAuthorPhoneOrThrow(long id) {
        return carListingRepository.findAuthorPhoneByPublishedId(id)
                .switchIfEmpty(Mono.error(new ApiException(HttpStatus.NOT_FOUND, "/problems/listing-not-found", "Listing not found")));
    }

    public Mono<PageDTO<PublicCarListingItemDTO>> getPublishedListings(GetPublishedListingsRequestDTO request,
                                                                       Long userId) {
        return Mono.zip(
                carListingRepository.countPublished(request),
                carListingRepository.findPublished(request, userId).collectList()
        ).map(tuple -> new PageDTO<>(tuple.getT2(), tuple.getT1()));
    }

    public Flux<PublicCarListingItemDTO> getFavouritesListings(long userId, int offset, int size) {
        return carListingRepository.findFavouritesByUserId(userId, offset, size);
    }

    public Mono<CarListing> promoteCarListing(CarListing carListing, CarListingPromotionPeriod period) {
        if (carListing.promotedUntil() > 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "/problems/listing-already-promoted",
                    "Listing is already promoted");
        }

        CarListing updatedCarListing = carListing.withPromotedUntil(System.currentTimeMillis() + period.ms);
        return carListingRepository.update(updatedCarListing).thenReturn(updatedCarListing);
    }

    public long getListingRepublishCooldownMS() {
        return listingRepublishCooldownMS;
    }

    public int getListingsCountPerAuthorLimit() {
        return listingsCountPerAuthorLimit;
    }
}
