package edu.automarket.listing.controller;

import edu.automarket.analytics.CarListingAnalyticsService;
import edu.automarket.analytics.dto.ListingAnalyticsDayDTO;
import edu.automarket.common.ApiException;
import edu.automarket.common.ArrayUtils;
import edu.automarket.common.PageDTO;
import edu.automarket.listing.CarListingService;
import edu.automarket.listing.dto.OwnCarListingDTO;
import edu.automarket.listing.dto.OwnCarListingImageDTO;
import edu.automarket.listing.dto.OwnCarListingListItemDTO;
import edu.automarket.listing.dto.UpdateCarListingRequestDTO;
import edu.automarket.listing.dto.UpdateListingStatusRequestDTO;
import edu.automarket.listing.model.CarListing;
import edu.automarket.listing.model.ListingStatus;
import edu.automarket.upload.UploadService;
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
import reactor.core.scheduler.Schedulers;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;

@RestController
@RequestMapping("/api/listings/own")
public class OwnCarListingController {
    private final CarListingService carListingService;
    private final CarListingAnalyticsService carListingAnalyticsService;
    private final UploadService uploadService;

    public OwnCarListingController(CarListingService carListingService,
                                   CarListingAnalyticsService carListingAnalyticsService,
                                   UploadService uploadService) {
        this.carListingService = carListingService;
        this.carListingAnalyticsService = carListingAnalyticsService;
        this.uploadService = uploadService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<OwnCarListingDTO> create(@AuthenticationPrincipal long userId) {
        return carListingService.create(userId).map(this::toDTO);
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
                    return toDTO(listing);
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

                    List<String> removedImages = new ArrayList<>();
                    List<String> addedImages = new ArrayList<>();

                    Mono<CarListing> updateMono = validateImagesAndUpdate(
                            userId, request, listing, removedImages, addedImages);

                    if (removedImages.isEmpty() && addedImages.isEmpty()) {
                        return updateMono;
                    }

                    return updateMono.doOnSuccess(_ -> {
                        if (!removedImages.isEmpty()) {
                            Mono.fromCallable(() -> uploadService.deleteFiles(removedImages))
                                    .subscribeOn(Schedulers.boundedElastic())
                                    .subscribe();
                        }
                        if (!addedImages.isEmpty()) {
                            uploadService.markUploadAsUsed(addedImages).subscribe();
                        }
                    });
                })
                .map(this::toDTO);
    }

    private Mono<CarListing> validateImagesAndUpdate(long userId,
                                                     UpdateCarListingRequestDTO request,
                                                     CarListing listing,
                                                     List<String> removedImages,
                                                     List<String> addedImages) {
        String[] oldImages = listing.imageKeys() != null ? listing.imageKeys() : new String[0];
        String[] newImages = request.imageKeys() != null ? request.imageKeys() : new String[0];

        if (!Arrays.equals(oldImages, newImages)) {
            Set<String> newImagesSet = new HashSet<>();

            for (String oldImage : oldImages) {
                if (!ArrayUtils.contains(newImages, oldImage)) {
                    removedImages.add(oldImage);
                }
            }
            for (String newImage : newImages) {
                if (!newImagesSet.add(newImage)) {
                    throw new ApiException(HttpStatus.BAD_REQUEST,
                            "/problems/duplicate-image-keys",
                            "Duplicate image keys are not allowed");
                }
                if (!ArrayUtils.contains(oldImages, newImage)) {
                    addedImages.add(newImage);
                }
            }

            if (!addedImages.isEmpty()) {
                return uploadService.filterUploadsByAccess(addedImages, userId)
                        .collectList()
                        .flatMap(accessibleKeys -> {
                            if (accessibleKeys.size() != addedImages.size()) {
                                throw new ApiException(HttpStatus.BAD_REQUEST,
                                        "/problems/invalid-image-keys",
                                        "Some of the image keys are invalid");
                            }
                            return carListingService.update(listing, request);
                        });
            }
        }

        return carListingService.update(listing, request);
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

                    if (listing.imageKeys() == null || listing.imageKeys().length == 0) {
                        return carListingService.delete(id);
                    }

                    return carListingService.delete(id).doFinally(_ -> {
                        Mono.fromCallable(() -> uploadService.deleteFiles(Arrays.asList(listing.imageKeys())))
                                .subscribeOn(Schedulers.boundedElastic())
                                .subscribe();
                    });
                });
    }

    private OwnCarListingDTO toDTO(CarListing carListing) {
        String[] imageKeys = carListing.imageKeys();
        if (imageKeys == null || imageKeys.length == 0) {
            return new OwnCarListingDTO(carListing, null);
        }

        OwnCarListingImageDTO[] images = new OwnCarListingImageDTO[imageKeys.length];
        for (int i = 0; i < imageKeys.length; i++) {
            String imageKey = imageKeys[i];
            images[i] = new OwnCarListingImageDTO(imageKey, uploadService.getCdnEndpointUrl(imageKey));
        }
        return new OwnCarListingDTO(carListing, images);
    }
}
