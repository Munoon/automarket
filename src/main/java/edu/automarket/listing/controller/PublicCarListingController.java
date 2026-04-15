package edu.automarket.listing.controller;

import edu.automarket.analytics.CarListingAnalyticsService;
import edu.automarket.common.PageDTO;
import edu.automarket.listing.CarListingService;
import edu.automarket.listing.dto.AuthorPhoneDTO;
import edu.automarket.listing.dto.GetPublishedListingsRequestDTO;
import edu.automarket.listing.dto.PublicCarListingDTO;
import edu.automarket.listing.dto.PublicCarListingItemDTO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/listings/public")
public class PublicCarListingController {
    private final CarListingService carListingService;
    private final CarListingAnalyticsService carListingAnalyticsService;

    public PublicCarListingController(CarListingService carListingService,
                                      CarListingAnalyticsService carListingAnalyticsService) {
        this.carListingService = carListingService;
        this.carListingAnalyticsService = carListingAnalyticsService;
    }

    @GetMapping("/{id}")
    public Mono<PublicCarListingDTO> getById(@PathVariable long id) {
        return carListingService.getPublishedListingByIdOrThrow(id)
                .doOnNext(listing -> carListingAnalyticsService.recordListingView(listing.id()));
    }

    @GetMapping("/{id}/phone")
    public Mono<AuthorPhoneDTO> getAuthorPhone(@PathVariable long id) {
        return carListingService.getPublishedListingAuthorPhoneOrThrow(id)
                .doOnNext(_ -> carListingAnalyticsService.recordListingPhoneRequest(id));
    }

    @GetMapping
    public Mono<PageDTO<PublicCarListingItemDTO>> getPublishedListings(
            @ModelAttribute @Valid GetPublishedListingsRequestDTO request) {
        return carListingService.getPublishedListings(request)
                .doOnNext(page -> {
                    List<PublicCarListingItemDTO> listings = page.content();
                    //noinspection ForLoopReplaceableByForEach
                    for (int i = 0; i < listings.size(); i++) {
                        PublicCarListingItemDTO listing = listings.get(i);
                        carListingAnalyticsService.recordListingImpression(listing.id());
                    }
                });
    }
}
