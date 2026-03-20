package edu.automarket.user;

import edu.automarket.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

class UserRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void findByIdReturnsCorrectUser() {
        User saved = userRepository.register("+380123456789").block();

        StepVerifier.create(userRepository.findById(saved.id()))
                .assertNext(user -> {
                    assertThat(user.id()).isEqualTo(saved.id());
                    assertThat(user.phoneNumber()).isEqualTo("+380123456789");
                })
                .verifyComplete();
    }

    @Test
    void findByIdReturnsEmptyWhenUserDoesNotExist() {
        StepVerifier.create(userRepository.findById(99999L))
                .verifyComplete();
    }

    @Test
    void registerPersistsUserWithCorrectFields() {
        long before = System.currentTimeMillis();

        StepVerifier.create(userRepository.register("+380123456789"))
                .assertNext(user -> {
                    assertThat(user.id()).isPositive();
                    assertThat(user.phoneNumber()).isEqualTo("+380123456789");
                    assertThat(user.displayName()).isNull();
                    assertThat(user.createdAt()).isGreaterThanOrEqualTo(before);
                    assertThat(user.active()).isTrue();
                })
                .verifyComplete();
    }

    @Test
    void findByPhoneNumberReturnsCorrectUser() {
        User saved = userRepository.register("+380123456789").block();

        StepVerifier.create(userRepository.findByPhoneNumber("+380123456789"))
                .assertNext(user -> {
                    assertThat(user.id()).isEqualTo(saved.id());
                    assertThat(user.phoneNumber()).isEqualTo("+380123456789");
                })
                .verifyComplete();
    }

    @Test
    void findByPhoneNumberReturnsEmptyWhenNotFound() {
        StepVerifier.create(userRepository.findByPhoneNumber("+380999999999"))
                .verifyComplete();
    }

    @Test
    void updateDisplayNamePersistsChange() {
        User saved = userRepository.register("+380123456789").block();

        userRepository.updateDisplayName(saved.id(), "New Name").block();

        StepVerifier.create(userRepository.findById(saved.id()))
                .assertNext(user -> assertThat(user.displayName()).isEqualTo("New Name"))
                .verifyComplete();
    }
}
