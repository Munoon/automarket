package edu.automarket.listing;

import edu.automarket.common.PageDTO;
import edu.automarket.listing.dto.AuthorPhoneDTO;
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
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
public class CarListingService {
    private final CarListingRepository carListingRepository;
    private final long listingRepublishCooldownMS;
    private final int listingsCountPerAuthorLimit;

    public CarListingService(CarListingRepository carListingRepository,
                             @Value("${app.listing.republishCooldown:3d}") Duration listingRepublishCooldouwn,
                             @Value("${app.listing.countLimitPerAuthor:30}") int listingsCountPerAuthorLimit) {
        this.carListingRepository = carListingRepository;
        this.listingRepublishCooldownMS = listingRepublishCooldouwn.toMillis();
        this.listingsCountPerAuthorLimit = listingsCountPerAuthorLimit;
    }

    public Mono<CarListing> create(long authorUserId) {
        return carListingRepository.countByUserIdAndStatuses(authorUserId, mapStatusNamesToString(null))
                .flatMap(count -> {
                    if (count >= listingsCountPerAuthorLimit) {
                        throw new ResponseStatusException(
                                HttpStatus.FORBIDDEN, "You have reached the limit of listings per user");
                    }
                    return carListingRepository.create(authorUserId, System.currentTimeMillis());
                });
    }

    public Mono<PageDTO<OwnCarListingListItemDTO>> getOwnListings(long userId, ListingStatus[] statuses, int page, int size) {
        String[] statusNames = mapStatusNamesToString(statuses);

        return Mono.zip(
                carListingRepository.countByUserIdAndStatuses(userId, statusNames),
                carListingRepository.findByUserIdAndStatuses(userId, statusNames, page, size).collectList()
        ).map(tuple -> new PageDTO<>(tuple.getT2(), tuple.getT1()));
    }

    public Mono<CarListing> getListingByIdOrThrow(long id) {
        return carListingRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Listing not found")));
    }

    public Mono<Void> delete(long id) {
        return carListingRepository.deleteById(id);
    }

    public Mono<Void> updateStatus(CarListing listing, ListingStatus newStatus) {
        long publishedAt = newStatus == ListingStatus.PUBLISHED
                ? System.currentTimeMillis()
                : listing.publishedAt();

        if (newStatus == ListingStatus.PUBLISHED
            && listing.publishedAt() > 0
            && publishedAt - listing.publishedAt() < listingRepublishCooldownMS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Listing publishing cooldown in progress");
        }

        return carListingRepository.updateStatus(listing.id(), newStatus, publishedAt);
    }

    public Mono<Void> update(long id, UpdateCarListingRequestDTO request) {
        return carListingRepository.update(id, request);
    }

    public Mono<PublicCarListingDTO> getPublishedListingByIdOrThrow(long id) {
        return carListingRepository.findPublishedById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Listing not found")));
    }

    public Mono<AuthorPhoneDTO> getPublishedListingAuthorPhoneOrThrow(long id) {
        return carListingRepository.findAuthorPhoneByPublishedId(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Listing not found")));
    }

    public Mono<PageDTO<PublicCarListingItemDTO>> getPublishedListings(GetPublishedListingsRequestDTO request) {
        return Mono.zip(
                carListingRepository.countPublished(request),
                carListingRepository.findPublished(request).collectList()
        ).map(tuple -> new PageDTO<>(tuple.getT2(), tuple.getT1()));
    }

    private static String[] mapStatusNamesToString(ListingStatus[] statuses) {
        if (statuses == null || statuses.length == 0) {
            ListingStatus[] allStatuses = ListingStatus.values();
            String[] statusNames = new String[allStatuses.length];
            for (int i = 0; i < allStatuses.length; i++) {
                statusNames[i] = allStatuses[i].name();
            }
            return statusNames;
        } else {
            String[] statusNames = new String[statuses.length];
            for (int i = 0; i < statuses.length; i++) {
                statusNames[i] = statuses[i].name();
            }
            return statusNames;
        }
    }

    public long getListingRepublishCooldownMS() {
        return listingRepublishCooldownMS;
    }

    public int getListingsCountPerAuthorLimit() {
        return listingsCountPerAuthorLimit;
    }
}
