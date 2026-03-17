package edu.automarket.listing;

import edu.automarket.common.PageDTO;
import edu.automarket.listing.dto.CarListingListItemDTO;
import edu.automarket.listing.dto.UpdateCarListingRequestDTO;
import edu.automarket.listing.model.CarListing;
import edu.automarket.listing.model.ListingStatus;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@Service
public class CarListingService {
    private final CarListingRepository carListingRepository;

    public CarListingService(CarListingRepository carListingRepository) {
        this.carListingRepository = carListingRepository;
    }

    public Mono<CarListing> create(long authorUserId) {
        return carListingRepository.create(authorUserId, System.currentTimeMillis());
    }

    public Mono<PageDTO<CarListingListItemDTO>> getOwnListings(long userId, ListingStatus[] statuses, int page, int size) {
        String[] statusNames;
        if (statuses == null || statuses.length == 0) {
            ListingStatus[] allStatuses = ListingStatus.values();
            statusNames = new String[allStatuses.length];
            for (int i = 0; i < allStatuses.length; i++) {
                statusNames[i] = allStatuses[i].name();
            }
        } else {
            statusNames = new String[statuses.length];
            for (int i = 0; i < statuses.length; i++) {
                statusNames[i] = statuses[i].name();
            }
        }

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

    public Mono<Void> updateStatus(long id, ListingStatus status) {
        return carListingRepository.updateStatus(id, status);
    }

    public Mono<Void> update(long id, UpdateCarListingRequestDTO request) {
        return carListingRepository.update(id, request);
    }
}
