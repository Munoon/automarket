package edu.automarket.listing.controller;

import edu.automarket.analytics.CarListingAnalyticsService;
import edu.automarket.analytics.dto.ListingAnalyticsDayDTO;
import edu.automarket.common.ApiException;
import edu.automarket.common.PageDTO;
import edu.automarket.listing.CarListingService;
import edu.automarket.listing.dto.OwnCarListingDTO;
import edu.automarket.listing.dto.OwnCarListingListItemDTO;
import edu.automarket.listing.dto.UpdateCarListingRequestDTO;
import edu.automarket.listing.dto.UpdateListingStatusRequestDTO;
import edu.automarket.listing.model.ListingStatus;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.ZoneId;

@RestController
@RequestMapping("/api/listings/own")
public class OwnCarListingController {
    private final CarListingService carListingService;
    private final CarListingAnalyticsService carListingAnalyticsService;

    public OwnCarListingController(CarListingService carListingService,
                                   CarListingAnalyticsService carListingAnalyticsService) {
        this.carListingService = carListingService;
        this.carListingAnalyticsService = carListingAnalyticsService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<OwnCarListingDTO> create(@AuthenticationPrincipal long userId) {
        return carListingService.create(userId).map(OwnCarListingDTO::new);
    }

    @GetMapping
    public Mono<PageDTO<OwnCarListingListItemDTO>> getOwnListings(
            @AuthenticationPrincipal long userId,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "20") int size
    ) {
        return carListingService.getOwnListings(userId, offset, size);
    }

    @GetMapping("/{id}")
    public Mono<OwnCarListingDTO> getById(
            @AuthenticationPrincipal long userId,
            @PathVariable long id
    ) {
        return carListingService.getListingByIdOrThrow(id)
                .map(listing -> {
                    if (listing.authorUserId() != userId) {
                        throw new ApiException(HttpStatus.FORBIDDEN, "/problems/access-denied", "Access denied");
                    }
                    return new OwnCarListingDTO(listing);
                });
    }

    @GetMapping("/{id}/analytics")
    public Flux<ListingAnalyticsDayDTO> getAnalytics(
            @AuthenticationPrincipal long userId,
            @PathVariable long id,
            @RequestParam(defaultValue = "UTC") String timezone
    ) {
        ZoneId zoneId = ZoneId.of(timezone);
        return carListingService.getListingByIdOrThrow(id)
                .flatMapMany(listing -> {
                    if (listing.authorUserId() != userId) {
                        throw new ApiException(HttpStatus.FORBIDDEN, "/problems/access-denied", "Access denied");
                    }
                    return carListingAnalyticsService.getListingAnalyticsByDay(id, zoneId);
                });
    }

    @PatchMapping("/{id}/status")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> updateStatus(
            @AuthenticationPrincipal long userId,
            @PathVariable long id,
            @Valid @RequestBody UpdateListingStatusRequestDTO request
    ) {
        return carListingService.getListingByIdOrThrow(id)
                .flatMap(listing -> {
                    if (listing.authorUserId() != userId) {
                        throw new ApiException(HttpStatus.FORBIDDEN, "/problems/access-denied", "Access denied");
                    }

                    ListingStatus newStatus = request.status();
                    if (listing.status() == newStatus) {
                        throw new ApiException(HttpStatus.BAD_REQUEST, "/problems/status-already-set", "Status already set");
                    }

                    return carListingService.updateStatus(listing, newStatus);
                });
    }

    @PatchMapping("/{id}")
    public Mono<OwnCarListingDTO> update(
            @AuthenticationPrincipal long userId,
            @PathVariable long id,
            @Valid @RequestBody UpdateCarListingRequestDTO request
    ) {
        request.validate();
        return carListingService.getListingByIdOrThrow(id)
                .flatMap(listing -> {
                    if (listing.authorUserId() != userId) {
                        throw new ApiException(HttpStatus.FORBIDDEN, "/problems/access-denied", "Access denied");
                    }
                    return carListingService.update(listing, request);
                })
                .map(OwnCarListingDTO::new);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> delete(
            @AuthenticationPrincipal long userId,
            @PathVariable long id
    ) {
        return carListingService.getListingByIdOrThrow(id)
                .flatMap(listing -> {
                    if (listing.authorUserId() != userId) {
                        throw new ApiException(HttpStatus.FORBIDDEN, "/problems/access-denied", "Access denied");
                    }
                    return carListingService.delete(id);
                });
    }
}
