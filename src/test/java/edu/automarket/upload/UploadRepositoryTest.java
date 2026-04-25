package edu.automarket.upload;

import edu.automarket.AbstractIntegrationTest;
import edu.automarket.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class UploadRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private UploadRepository uploadRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void saveUserUploadInsertsRecord() {
        long userId = userRepository.register("+380123456789").block().id();
        long now = System.currentTimeMillis();

        uploadRepository.saveUserUpload("listings/1/img.jpeg", userId, now).block();

        assertThat(uploadRepository.countUserUploads(userId).block()).isEqualTo(1);
    }

    @Test
    void countUserUploadsCountsOnlyForGivenUser() {
        long userId1 = userRepository.register("+380123456789").block().id();
        long userId2 = userRepository.register("+380123456780").block().id();
        long now = System.currentTimeMillis();

        uploadRepository.saveUserUpload("key-1", userId1, now).block();
        uploadRepository.saveUserUpload("key-2", userId1, now).block();
        uploadRepository.saveUserUpload("key-3", userId2, now).block();

        assertThat(uploadRepository.countUserUploads(userId1).block()).isEqualTo(2);
        assertThat(uploadRepository.countUserUploads(userId2).block()).isEqualTo(1);
    }

    @Test
    void findOldUploadsReturnsOnlyEntriesOlderThanThreshold() {
        long userId = userRepository.register("+380123456789").block().id();
        long oldTimestamp = System.currentTimeMillis() - Duration.ofDays(2).toMillis();
        long recentTimestamp = System.currentTimeMillis();

        uploadRepository.saveUserUpload("old-key", userId, oldTimestamp).block();
        uploadRepository.saveUserUpload("recent-key", userId, recentTimestamp).block();

        long threshold = System.currentTimeMillis() - Duration.ofDays(1).toMillis();

        StepVerifier.create(uploadRepository.findOldUploads(threshold, 0))
                .assertNext(key -> assertThat(key).isEqualTo("old-key"))
                .verifyComplete();
    }

    @Test
    void findOldUploadsRespectsOffset() {
        long userId = userRepository.register("+380123456789").block().id();
        long oldTimestamp = System.currentTimeMillis() - Duration.ofDays(2).toMillis();

        uploadRepository.saveUserUpload("key-a", userId, oldTimestamp).block();
        uploadRepository.saveUserUpload("key-b", userId, oldTimestamp + 1).block();
        uploadRepository.saveUserUpload("key-c", userId, oldTimestamp + 2).block();

        long threshold = System.currentTimeMillis() - Duration.ofDays(1).toMillis();

        List<String> all = uploadRepository.findOldUploads(threshold, 0).collectList().block();
        List<String> withOffset = uploadRepository.findOldUploads(threshold, 1).collectList().block();

        assertThat(all).hasSize(3);
        assertThat(all).containsExactly("key-a", "key-b", "key-c");

        assertThat(withOffset).hasSize(2);
        assertThat(withOffset).containsExactly("key-b", "key-c");
    }

    @Test
    void deleteUploadsRemovesSpecifiedKeys() {
        long userId = userRepository.register("+380123456789").block().id();
        long now = System.currentTimeMillis();

        uploadRepository.saveUserUpload("key-a", userId, now).block();
        uploadRepository.saveUserUpload("key-b", userId, now).block();
        uploadRepository.saveUserUpload("key-c", userId, now).block();

        Long deleted = uploadRepository.deleteUploads(List.of("key-a", "key-b")).block();

        assertThat(deleted).isEqualTo(2);
        assertThat(uploadRepository.countUserUploads(userId).block()).isEqualTo(1);

        StepVerifier.create(uploadRepository.filterUploadsByAccess(List.of("key-a", "key-b", "key-c"), userId))
                .assertNext(key -> assertThat(key).isEqualTo("key-c"))
                .verifyComplete();
    }

    @Test
    void filterUploadsByAccessReturnsOnlyKeysOwnedByUser() {
        long userId1 = userRepository.register("+380123456789").block().id();
        long userId2 = userRepository.register("+380123456780").block().id();
        long now = System.currentTimeMillis();

        uploadRepository.saveUserUpload("key-user1-a", userId1, now).block();
        uploadRepository.saveUserUpload("key-user1-b", userId1, now).block();
        uploadRepository.saveUserUpload("key-user2",   userId2, now).block();

        StepVerifier.create(uploadRepository
                .filterUploadsByAccess(List.of("key-user1-a", "key-user1-b", "key-user2"), userId1)
                .sort())
                .assertNext(key -> assertThat(key).isEqualTo("key-user1-a"))
                .assertNext(key -> assertThat(key).isEqualTo("key-user1-b"))
                .verifyComplete();
    }

    @Test
    void filterUploadsByAccessReturnsEmptyWhenNoKeysMatch() {
        long userId1 = userRepository.register("+380123456789").block().id();
        long userId2 = userRepository.register("+380123456780").block().id();

        uploadRepository.saveUserUpload("key-user2", userId2, System.currentTimeMillis()).block();

        StepVerifier.create(uploadRepository.filterUploadsByAccess(List.of("key-user2"), userId1))
                .verifyComplete();
    }
}
