package edu.automarket.upload;

import edu.automarket.common.ApiException;
import edu.automarket.upload.dto.GenerateSignedUrlRequestDTO;
import edu.automarket.upload.dto.SignUrlResponseDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsResponse;
import software.amazon.awssdk.services.s3.model.DeletedObject;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class UploadService {
    private static final Map<String, String> CONTENT_TYPE_EXTENSIONS = Map.of(
            "image/jpeg", ".jpeg",
            "image/png", ".png",
            "image/webp", ".webp"
    );
    private static final Logger log = LoggerFactory.getLogger(UploadService.class);
    private final UploadRepository uploadRepository;
    private final String bucketName;
    private final String cdnEndpointUrl;
    private final int maxUploadsLimitPerUser;
    private final S3Presigner presigner;
    private final S3Client s3Client;

    public UploadService(UploadRepository uploadRepository,
                         @Value("${app.s3.bucketName:}") String bucketName,
                         @Value("${app.s3.endpointUrl:}") String endpointUrl,
                         @Value("${app.s3.cdnEndpointUrl:}") String cdnEndpointUrl,
                         @Value("${app.s3.accessKeyId:}") String accessKeyID,
                         @Value("${app.s3.secretAccessKey:}") String secretAccessKey,
                         @Value("${app.s3.pathStyleAccessEnabled:false}") boolean pathStyleAccessEnabled,
                         @Value("${app.s3.maxUploadsLimitPerUser:20}") int maxUploadsLimitPerUser) {
        this.uploadRepository = uploadRepository;
        this.cdnEndpointUrl = cdnEndpointUrl;
        this.maxUploadsLimitPerUser = maxUploadsLimitPerUser;

        if (bucketName != null && !bucketName.isBlank()
                && endpointUrl != null && !endpointUrl.isBlank()
                && accessKeyID != null && !accessKeyID.isBlank()
                && secretAccessKey != null && !secretAccessKey.isBlank()) {
            URI endpointOverride = URI.create(endpointUrl);
            AwsCredentialsProvider credentialsProvider = () -> AwsBasicCredentials.create(accessKeyID, secretAccessKey);
            S3Configuration s3Config = S3Configuration.builder()
                    .pathStyleAccessEnabled(pathStyleAccessEnabled)
                    .build();

            this.bucketName = bucketName;
            this.presigner = S3Presigner.builder()
                    .endpointOverride(endpointOverride)
                    .credentialsProvider(credentialsProvider)
                    .region(Region.EU_CENTRAL_1)
                    .serviceConfiguration(s3Config)
                    .build();
            this.s3Client = S3Client.builder()
                    .endpointOverride(endpointOverride)
                    .credentialsProvider(credentialsProvider)
                    .region(Region.EU_CENTRAL_1)
                    .serviceConfiguration(s3Config)
                    .build();
        } else {
            log.warn("Not all s3 properties are provided. File upload will not work.");
            this.bucketName = null;
            this.presigner = null;
            this.s3Client = null;
        }
    }

    public Mono<SignUrlResponseDTO> generateSignedUploadUrl(long userId, GenerateSignedUrlRequestDTO request) {
        if (presigner == null) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "/problems/upload-unavailable",
                    "Upload service is unavailable");
        }

        return uploadRepository.countUserUploads(userId)
                .flatMap(count -> {
                    if (count >= maxUploadsLimitPerUser) {
                        throw new ApiException(HttpStatus.BAD_REQUEST,
                                "/problems/upload-limit-exceeded",
                                "Upload limit exceeded. Please, try again later.");
                    }

                    String extension = CONTENT_TYPE_EXTENSIONS.get(request.contentType());
                    String key = "listings/" + request.listingId() + '/' + UUID.randomUUID() + extension;

                    PresignedPutObjectRequest signedRequest = presigner.presignPutObject(builder -> builder
                            .putObjectRequest(PutObjectRequest.builder()
                                    .bucket(bucketName)
                                    .cacheControl("public, max-age=31536000")
                                    .contentLength(request.contentLength())
                                    .contentMD5(request.md5())
                                    .contentType(request.contentType())
                                    .acl(ObjectCannedACL.PUBLIC_READ)
                                    .key(key)
                                    .build())
                            .signatureDuration(Duration.ofMinutes(1)));

                    return uploadRepository.saveUserUpload(key, userId, System.currentTimeMillis())
                            .thenReturn(new SignUrlResponseDTO(
                                    signedRequest.url().toString(),
                                    signedRequest.signedHeaders(),
                                    key,
                                    getCdnEndpointUrl(key)
                            ));
                });
    }

    public Flux<String> filterUploadsByAccess(List<String> keys, long userId) {
        return uploadRepository.filterUploadsByAccess(keys, userId);
    }

    public Mono<Void> markUploadAsUsed(List<String> keys) {
        return uploadRepository.deleteUploads(keys).then();
    }

    public DeleteObjectsResponse deleteFiles(List<String> keys) {
        try {
            List<ObjectIdentifier> objects = new ArrayList<>(keys.size());
            //noinspection ForLoopReplaceableByForEach
            for (int i = 0; i < keys.size(); i++) {
                String key = keys.get(i);
                objects.add(ObjectIdentifier.builder().key(key).build());
            }

            DeleteObjectsRequest deleteRequest = DeleteObjectsRequest.builder()
                    .bucket(bucketName)
                    .delete(Delete.builder().objects(objects).build())
                    .build();
            return s3Client.deleteObjects(deleteRequest);
        } catch (Exception e) {
            log.error("Failed to delete files from S3", e);
            return DeleteObjectsResponse.builder().build();
        }
    }

    public String getCdnEndpointUrl(String key) {
        if (cdnEndpointUrl == null) {
            return null;
        }
        return cdnEndpointUrl + "/" + key;
    }

    public String[] getCdnEndpointUrl(String[] key) {
        if (cdnEndpointUrl == null || key == null || key.length == 0) {
            return null;
        }

        String[] result = new String[key.length];
        for (int i = 0; i < key.length; i++) {
            result[i] = getCdnEndpointUrl(key[i]);
        }
        return result;
    }

    @Scheduled(fixedRate = 1, initialDelay = 1, timeUnit = TimeUnit.HOURS)
    public void cleanUnusedUploads() {
        long maxUploadedAt = System.currentTimeMillis() - Duration.ofDays(1).toMillis();

        int offset = 0;
        for (int i = 0; i < 5; i++) {
            List<String> files = uploadRepository.findOldUploads(maxUploadedAt, offset).collectList().block();
            if (files.isEmpty()) {
                break;
            }

            DeleteObjectsResponse response = deleteFiles(files);
            List<DeletedObject> deletedObjects = response.deleted();
            List<String> deletedKeys = new ArrayList<>(deletedObjects.size());
            for (DeletedObject deletedObject : deletedObjects) {
                deletedKeys.add(deletedObject.key());
            }
            uploadRepository.deleteUploads(deletedKeys).block();

            log.info("Removed {} files from CDN, since they wasn't used after upload", deletedKeys.size());
            if (response.hasErrors()) {
                log.warn("Some files fails to delete: {}", response.errors());
            }

            offset += files.size() - deletedKeys.size(); // offset files that we've failed to delete
        }
    }
}
