package edu.automarket.upload;

import edu.automarket.common.ApiException;
import edu.automarket.listing.CarListingService;
import edu.automarket.upload.dto.GenerateSignedUrlRequestDTO;
import edu.automarket.upload.dto.SignUrlResponseDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class UploadController {
    private final UploadService uploadService;
    private final CarListingService carListingService;

    public UploadController(UploadService uploadService,
                            CarListingService carListingService) {
        this.uploadService = uploadService;
        this.carListingService = carListingService;
    }

    @PostMapping("/api/upload/signUrl")
    public Mono<SignUrlResponseDTO> generateSignedUrl(@RequestBody @Valid GenerateSignedUrlRequestDTO request,
                                                      @AuthenticationPrincipal long userId) {
        return carListingService.getListingByIdOrThrow(request.listingId())
                .flatMap(listing -> {
                    if (listing.authorUserId() != userId) {
                        throw new ApiException(HttpStatus.FORBIDDEN, "/problems/access-denied", "Access denied");
                    }
                    return uploadService.generateSignedUploadUrl(userId, request);
                });
    }
}
