package edu.automarket.upload;

import edu.automarket.AbstractIntegrationTest;
import edu.automarket.authentication.AuthenticationService;
import edu.automarket.common.ApiException;
import edu.automarket.common.ProblemDTO;
import edu.automarket.listing.CarListingService;
import edu.automarket.listing.model.CarListing;
import edu.automarket.upload.dto.GenerateSignedUrlRequestDTO;
import edu.automarket.upload.dto.SignUrlResponseDTO;
import edu.automarket.user.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

class UploadControllerTest extends AbstractIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private UserService userService;

    @Autowired
    private CarListingService carListingService;

    @Autowired
    private AuthenticationService authenticationService;

    @MockitoBean
    private UploadService uploadService;

    // --- POST /api/upload/signUrl ---

    @Test
    void generateSignedUrlReturns401WithoutToken() {
        webTestClient.post()
                .uri("/api/upload/signUrl")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(validRequest(1))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void generateSignedUrlReturns404ForNonExistentListing() {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        String token = authenticationService.generateToken(userId);

        webTestClient.post()
                .uri("/api/upload/signUrl")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(validRequest(9999))
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void generateSignedUrlReturns403ForOtherUsersListing() {
        long userId1 = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        long userId2 = userService.getUserByPhoneNumberOrCreate("+380123456780").block().id();
        String token2 = authenticationService.generateToken(userId2);
        CarListing listing = carListingService.create(userId1).block();

        webTestClient.post()
                .uri("/api/upload/signUrl")
                .header("Authorization", "Bearer " + token2)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(validRequest((int) listing.id()))
                .exchange()
                .expectStatus().isForbidden()
                .expectHeader().contentType("application/problem+json")
                .expectBody(ProblemDTO.class)
                .value(problem -> {
                    assertThat(problem.type()).isEqualTo("/problems/access-denied");
                    assertThat(problem.status()).isEqualTo(403);
                });
    }

    @Test
    void generateSignedUrlReturns200WithValidRequest() {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        String token = authenticationService.generateToken(userId);
        CarListing listing = carListingService.create(userId).block();

        SignUrlResponseDTO mockResponse = new SignUrlResponseDTO(
                "http://s3.test/upload-url",
                Map.of("content-type", List.of("image/jpeg")),
                "listings/" + listing.id() + "/image.jpeg",
                "http://cdn.test/listings/" + listing.id() + "/image.jpeg"
        );
        when(uploadService.generateSignedUploadUrl(anyLong(), any())).thenReturn(Mono.just(mockResponse));

        webTestClient.post()
                .uri("/api/upload/signUrl")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(validRequest((int) listing.id()))
                .exchange()
                .expectStatus().isOk()
                .expectBody(SignUrlResponseDTO.class)
                .value(response -> {
                    assertThat(response.uploadUrl()).isEqualTo(mockResponse.uploadUrl());
                    assertThat(response.fileKey()).isEqualTo(mockResponse.fileKey());
                    assertThat(response.fileUrl()).isEqualTo(mockResponse.fileUrl());
                });
    }

    @Test
    void generateSignedUrlReturns503WhenS3NotConfigured() {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        String token = authenticationService.generateToken(userId);
        CarListing listing = carListingService.create(userId).block();

        when(uploadService.generateSignedUploadUrl(anyLong(), any()))
                .thenThrow(new ApiException(HttpStatus.SERVICE_UNAVAILABLE,
                        "/problems/upload-unavailable", "Upload service is unavailable"));

        webTestClient.post()
                .uri("/api/upload/signUrl")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(validRequest((int) listing.id()))
                .exchange()
                .expectStatus().isEqualTo(503)
                .expectHeader().contentType("application/problem+json")
                .expectBody(ProblemDTO.class)
                .value(problem -> {
                    assertThat(problem.type()).isEqualTo("/problems/upload-unavailable");
                    assertThat(problem.status()).isEqualTo(503);
                });
    }

    @Test
    void generateSignedUrlReturns400WhenUploadLimitExceeded() {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        String token = authenticationService.generateToken(userId);
        CarListing listing = carListingService.create(userId).block();

        when(uploadService.generateSignedUploadUrl(anyLong(), any()))
                .thenThrow(new ApiException(HttpStatus.BAD_REQUEST,
                        "/problems/upload-limit-exceeded", "Upload limit exceeded. Please, try again later."));

        webTestClient.post()
                .uri("/api/upload/signUrl")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(validRequest((int) listing.id()))
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentType("application/problem+json")
                .expectBody(ProblemDTO.class)
                .value(problem -> {
                    assertThat(problem.type()).isEqualTo("/problems/upload-limit-exceeded");
                    assertThat(problem.status()).isEqualTo(400);
                });
    }

    @Test
    void generateSignedUrlReturns400WithInvalidContentType() {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        String token = authenticationService.generateToken(userId);
        CarListing listing = carListingService.create(userId).block();

        webTestClient.post()
                .uri("/api/upload/signUrl")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new GenerateSignedUrlRequestDTO((int) listing.id(), 1024, "A".repeat(24), "image/tiff"))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void generateSignedUrlReturns400WhenContentLengthTooLarge() {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        String token = authenticationService.generateToken(userId);
        CarListing listing = carListingService.create(userId).block();

        webTestClient.post()
                .uri("/api/upload/signUrl")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new GenerateSignedUrlRequestDTO((int) listing.id(), 10 * 1024 * 1024 + 1, "A".repeat(24), "image/jpeg"))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void generateSignedUrlReturns400WithInvalidMd5Length() {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        String token = authenticationService.generateToken(userId);
        CarListing listing = carListingService.create(userId).block();

        webTestClient.post()
                .uri("/api/upload/signUrl")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new GenerateSignedUrlRequestDTO((int) listing.id(), 1024, "tooshort", "image/jpeg"))
                .exchange()
                .expectStatus().isBadRequest();
    }

    private GenerateSignedUrlRequestDTO validRequest(int listingId) {
        return new GenerateSignedUrlRequestDTO(listingId, 1024, "A".repeat(24), "image/jpeg");
    }
}
