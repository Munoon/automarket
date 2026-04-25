package edu.automarket.upload;

import com.sun.net.httpserver.HttpServer;
import edu.automarket.AbstractIntegrationTest;
import edu.automarket.common.ApiException;
import edu.automarket.listing.CarListingService;
import edu.automarket.listing.model.CarListing;
import edu.automarket.upload.dto.GenerateSignedUrlRequestDTO;
import edu.automarket.upload.dto.SignUrlResponseDTO;
import edu.automarket.user.UserService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class UploadServiceTest extends AbstractIntegrationTest {
    private static HttpServer MOCK_S3;
    private static int MOCK_S3_PORT;
    private static final List<RecordedRequest> receivedRequests = new CopyOnWriteArrayList<>();

    record RecordedRequest(String method, String uri, String body) {}

    @BeforeAll
    static void startMockS3() throws IOException {
        MOCK_S3 = HttpServer.create(new InetSocketAddress(0), 0);
        MOCK_S3_PORT = MOCK_S3.getAddress().getPort();
        MOCK_S3.createContext("/", exchange -> {
            try {
                byte[] bodyBytes = exchange.getRequestBody().readAllBytes();
                String body = new String(bodyBytes);

                receivedRequests.add(new RecordedRequest(
                        exchange.getRequestMethod(),
                        exchange.getRequestURI().toString(),
                        body
                ));

                // Echo back requested keys as deleted
                StringBuilder xml = new StringBuilder(
                        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                                "<DeleteResult xmlns=\"http://s3.amazonaws.com/doc/2006-03-01/\">"
                );
                Matcher m = Pattern.compile("<Key>(.*?)</Key>").matcher(body);
                while (m.find()) {
                    xml.append("<Deleted><Key>").append(m.group(1)).append("</Key></Deleted>");
                }
                xml.append("</DeleteResult>");

                byte[] responseBytes = xml.toString().getBytes();
                exchange.getResponseHeaders().add("Content-Type", "application/xml");
                exchange.sendResponseHeaders(200, responseBytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(responseBytes);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        MOCK_S3.start();
    }

    @DynamicPropertySource
    static void s3Properties(DynamicPropertyRegistry registry) {
        registry.add("app.s3.endpointUrl", () -> "http://localhost:" + MOCK_S3_PORT);
        registry.add("app.s3.bucketName", () -> "test-bucket");
        registry.add("app.s3.accessKeyId", () -> "test-key");
        registry.add("app.s3.secretAccessKey", () -> "test-secret");
        registry.add("app.s3.cdnEndpointUrl", () -> "http://cdn.test");
        registry.add("app.s3.pathStyleAccessEnabled", () -> "true");
    }

    @AfterAll
    static void stopMockS3() {
        MOCK_S3.stop(0);
    }

    @Autowired
    private UploadService uploadService;

    @Autowired
    private UploadRepository uploadRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private CarListingService carListingService;

    @BeforeEach
    void clearRequests() {
        receivedRequests.clear();
    }

    @Test
    void generateSignedUploadUrlCreatesPresignedUrlAndSavesToDb() {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        CarListing listing = carListingService.create(userId).block();

        var request = new GenerateSignedUrlRequestDTO(
                (int) listing.id(), 1024, "A".repeat(24), "image/jpeg"
        );

        SignUrlResponseDTO response = uploadService.generateSignedUploadUrl(userId, request).block();

        assertThat(response.uploadUrl()).contains("localhost:" + MOCK_S3_PORT);
        assertThat(response.fileKey()).startsWith("listings/" + listing.id() + "/");
        assertThat(response.fileKey()).endsWith(".jpeg");
        assertThat(response.fileUrl()).isEqualTo("http://cdn.test/" + response.fileKey());
        assertThat(uploadRepository.countUserUploads(userId).block()).isEqualTo(1);
    }

    @Test
    void generateSignedUploadUrlThrowsWhenUploadLimitExceeded() {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        CarListing listing = carListingService.create(userId).block();

        long now = System.currentTimeMillis();
        for (int i = 0; i < 20; i++) {
            uploadRepository.saveUserUpload("key-" + i, userId, now).block();
        }

        var request = new GenerateSignedUrlRequestDTO(
                (int) listing.id(), 1024, "A".repeat(24), "image/jpeg"
        );

        StepVerifier.create(uploadService.generateSignedUploadUrl(userId, request))
                .expectErrorMatches(e -> e instanceof ApiException ex
                        && ex.getStatus() == HttpStatus.BAD_REQUEST)
                .verify();
    }

    @Test
    void deleteFilesCallsS3DeleteObjects() {
        List<String> keys = List.of("listings/1/file1.jpeg", "listings/1/file2.jpeg");

        uploadService.deleteFiles(keys);

        assertThat(receivedRequests).hasSize(1);
        RecordedRequest req = receivedRequests.get(0);
        assertThat(req.method()).isEqualTo("POST");
        assertThat(req.uri()).contains("delete");
        assertThat(req.body())
                .contains("<Key>listings/1/file1.jpeg</Key>")
                .contains("<Key>listings/1/file2.jpeg</Key>");
    }

    @Test
    void cleanUnusedUploadsDeletesOldFilesFromS3AndDb() {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();

        long oldTimestamp = System.currentTimeMillis() - Duration.ofDays(2).toMillis();
        uploadRepository.saveUserUpload("listings/1/old-file.jpeg", userId, oldTimestamp).block();

        uploadService.cleanUnusedUploads();

        assertThat(receivedRequests).hasSize(1);
        assertThat(receivedRequests.get(0).body()).contains("old-file.jpeg");
        assertThat(uploadRepository.countUserUploads(userId).block()).isEqualTo(0);
    }

    @Test
    void filterUploadsByAccessReturnsOnlyKeysOwnedByUser() {
        long userId1 = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        long userId2 = userService.getUserByPhoneNumberOrCreate("+380123456780").block().id();

        long now = System.currentTimeMillis();
        uploadRepository.saveUserUpload("key-user1-a", userId1, now).block();
        uploadRepository.saveUserUpload("key-user1-b", userId1, now).block();
        uploadRepository.saveUserUpload("key-user2", userId2, now).block();

        List<String> result = uploadService
                .filterUploadsByAccess(List.of("key-user1-a", "key-user1-b", "key-user2"), userId1)
                .collectList()
                .block();

        assertThat(result).containsExactlyInAnyOrder("key-user1-a", "key-user1-b");
    }

    @Test
    void filterUploadsByAccessReturnsEmptyWhenNoneMatch() {
        long userId1 = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        long userId2 = userService.getUserByPhoneNumberOrCreate("+380123456780").block().id();

        uploadRepository.saveUserUpload("key-user2", userId2, System.currentTimeMillis()).block();

        List<String> result = uploadService
                .filterUploadsByAccess(List.of("key-user2"), userId1)
                .collectList()
                .block();

        assertThat(result).isEmpty();
    }

    @Test
    void markUploadAsUsedRemovesKeysFromDb() {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();

        long now = System.currentTimeMillis();
        uploadRepository.saveUserUpload("key-a", userId, now).block();
        uploadRepository.saveUserUpload("key-b", userId, now).block();
        uploadRepository.saveUserUpload("key-c", userId, now).block();

        uploadService.markUploadAsUsed(List.of("key-a", "key-b")).block();

        assertThat(uploadRepository.countUserUploads(userId).block()).isEqualTo(1);

        List<String> remaining = uploadService
                .filterUploadsByAccess(List.of("key-a", "key-b", "key-c"), userId)
                .collectList()
                .block();
        assertThat(remaining).containsExactly("key-c");
    }
}
