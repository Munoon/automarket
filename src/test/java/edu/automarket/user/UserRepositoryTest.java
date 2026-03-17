package edu.automarket.user;

import edu.automarket.AbstractIntegrationTest;
import edu.automarket.TestUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

class UserRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void savePersistsUserAndAssignsId() {
        StepVerifier.create(userRepository.save(TestUtils.testUser("saveuser")))
                .assertNext(user -> {
                    assertThat(user.id()).isNotNull();
                    assertThat(user.username()).isEqualTo("saveuser");
                    assertThat(user.phoneNumber()).isEqualTo("+123456789012");
                    assertThat(user.displayName()).isEqualTo("Test User");
                    assertThat(user.active()).isTrue();
                })
                .verifyComplete();
    }

    @Test
    void findByUsernameReturnsCorrectUser() {
        User findme = userRepository.save(TestUtils.testUser("findme")).block();

        StepVerifier.create(userRepository.findByUsername("findme"))
                .assertNext(user -> {
                    assertThat(user.username()).isEqualTo("findme");
                    assertThat(user.id()).isEqualTo(findme.id());
                })
                .verifyComplete();
    }

    @Test
    void findByUsernameReturnsEmptyWhenUserDoesNotExist() {
        StepVerifier.create(userRepository.findByUsername("ghost"))
                .verifyComplete();
    }

    @Test
    void findByIdReturnsCorrectUser() {
        User saved = userRepository.save(TestUtils.testUser("byid")).block();

        StepVerifier.create(userRepository.findById(saved.id()))
                .assertNext(user -> {
                    assertThat(user.id()).isEqualTo(saved.id());
                    assertThat(user.username()).isEqualTo("byid");
                })
                .verifyComplete();
    }

    @Test
    void findByIdReturnsEmptyWhenUserDoesNotExist() {
        StepVerifier.create(userRepository.findById(99999L))
                .verifyComplete();
    }

    @Test
    void existsByUsernameReturnsTrueWhenUserExists() {
        userRepository.save(TestUtils.testUser("exists")).block();

        StepVerifier.create(userRepository.existsByUsername("exists"))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void existsByUsernameReturnsFalseWhenUserDoesNotExist() {
        StepVerifier.create(userRepository.existsByUsername("nobody"))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void savedUserFieldsAreFullyPersisted() {
        long before = System.currentTimeMillis();
        User saved = userRepository.save(TestUtils.testUser("fullcheck")).block();

        StepVerifier.create(userRepository.findById(saved.id()))
                .assertNext(user -> {
                    assertThat(user.username()).isEqualTo("fullcheck");
                    assertThat(user.phoneNumber()).isEqualTo("+123456789012");
                    assertThat(user.passwordHash()).isEqualTo("hash");
                    assertThat(user.displayName()).isEqualTo("Test User");
                    assertThat(user.createdAt()).isGreaterThanOrEqualTo(before);
                    assertThat(user.active()).isTrue();
                })
                .verifyComplete();
    }
}
