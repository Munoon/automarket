package edu.automarket.favourites;

import edu.automarket.analytics.CarListingAnalyticsService;
import edu.automarket.favourites.dto.FavouriteRequestDTO;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/favourites")
public class FavouritesController {
    private final FavouritesService favouritesService;
    private final CarListingAnalyticsService carListingAnalyticsService;

    public FavouritesController(FavouritesService favouritesService,
                                CarListingAnalyticsService carListingAnalyticsService) {
        this.favouritesService = favouritesService;
        this.carListingAnalyticsService = carListingAnalyticsService;
    }

    @PostMapping
    public Mono<Void> addFavourite(@AuthenticationPrincipal long userId,
                                   @RequestBody FavouriteRequestDTO request) {
        long listingId = request.listingId();
        return favouritesService.addFavourite(userId, listingId)
                .doOnSuccess(_ -> carListingAnalyticsService.recordListingAddedToFavourity(listingId));
    }

    @DeleteMapping
    public Mono<Void> removeFavourite(@AuthenticationPrincipal long userId,
                                      @RequestBody FavouriteRequestDTO request) {
        return favouritesService.removeFavourite(userId, request.listingId());
    }
}
