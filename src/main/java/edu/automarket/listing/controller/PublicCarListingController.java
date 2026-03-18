package edu.automarket.listing.controller;

import edu.automarket.common.PageDTO;
import edu.automarket.listing.CarListingService;
import edu.automarket.listing.dto.AuthorPhoneDTO;
import edu.automarket.listing.dto.PublicCarListingDTO;
import edu.automarket.listing.dto.PublicCarListingItemDTO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/listings/public")
public class PublicCarListingController {
    private final CarListingService carListingService;

    public PublicCarListingController(CarListingService carListingService) {
        this.carListingService = carListingService;
    }

    @GetMapping("/{id}")
    public Mono<PublicCarListingDTO> getById(@PathVariable long id) {
        return carListingService.getPublishedListingByIdOrThrow(id);
    }

    @GetMapping("/{id}/phone")
    public Mono<AuthorPhoneDTO> getAuthorPhone(@PathVariable long id) {
        return carListingService.getPublishedListingAuthorPhoneOrThrow(id);
    }

    @GetMapping
    public Mono<PageDTO<PublicCarListingItemDTO>> getPublishedListings(
            @RequestParam long publishedBefore,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return carListingService.getPublishedListings(publishedBefore, page, size);
    }
}
