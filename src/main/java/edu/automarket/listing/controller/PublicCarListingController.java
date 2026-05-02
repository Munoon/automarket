package edu.automarket.listing.controller;

import edu.automarket.analytics.CarListingAnalyticsService;
import edu.automarket.common.PageDTO;
import edu.automarket.favourites.FavouritesService;
import edu.automarket.listing.CarListingService;
import edu.automarket.listing.dto.AuthorPhoneDTO;
import edu.automarket.listing.dto.GetPublishedListingsRequestDTO;
import edu.automarket.listing.dto.PublicCarListingDTO;
import edu.automarket.listing.dto.PublicCarListingItemDTO;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/listings/public")
public class PublicCarListingController {
    private final CarListingService carListingService;
    private final CarListingAnalyticsService carListingAnalyticsService;
    private final FavouritesService favouritesService;

    public PublicCarListingController(CarListingService carListingService,
                                      CarListingAnalyticsService carListingAnalyticsService,
                                      FavouritesService favouritesService) {
        this.carListingService = carListingService;
        this.carListingAnalyticsService = carListingAnalyticsService;
        this.favouritesService = favouritesService;
    }

    @GetMapping("/{id}")
    public Mono<PublicCarListingDTO> getById(@PathVariable long id, @AuthenticationPrincipal Long userId) {
        return carListingService.getPublishedListingByIdOrThrow(id, userId)
                .doOnNext(listing -> carListingAnalyticsService.recordListingView(listing.id()));
    }

    @GetMapping("/{id}/phone")
    public Mono<AuthorPhoneDTO> getAuthorPhone(@PathVariable long id) {
        return carListingService.getPublishedListingAuthorPhoneOrThrow(id)
                .doOnNext(_ -> carListingAnalyticsService.recordListingPhoneRequest(id));
    }

    @GetMapping
    public Mono<PageDTO<PublicCarListingItemDTO>> getPublishedListings(
            @AuthenticationPrincipal Long userId,
            @ModelAttribute @Valid GetPublishedListingsRequestDTO request) {
        return carListingService.getPublishedListings(request, userId)
                .doOnNext(page -> {
                    List<PublicCarListingItemDTO> listings = page.content();
                    //noinspection ForLoopReplaceableByForEach
                    for (int i = 0; i < listings.size(); i++) {
                        PublicCarListingItemDTO listing = listings.get(i);
                        carListingAnalyticsService.recordListingImpression(listing.id());
                    }
                });
    }

    @GetMapping("/favourites")
    public Mono<PageDTO<PublicCarListingItemDTO>> getFavouritesListings(
            @AuthenticationPrincipal long userId,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "20") int size
    ) {
        return Mono.zip(
                carListingService.getFavouritesListings(userId, offset, Math.min(size, 100)).collectList(),
                favouritesService.countFavouritesByUser(userId)
        ).map(tuple -> new PageDTO<>(tuple.getT1(), tuple.getT2()));
    }
}
