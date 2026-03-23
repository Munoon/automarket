package edu.automarket.user;

import edu.automarket.AbstractIntegrationTest;
import edu.automarket.common.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

class UserServiceTest extends AbstractIntegrationTest {

    @Autowired
    private UserService userService;

    @Test
    void getUserByIdOrThrowReturnsExistingUser() {
        User created = userService.getUserByPhoneNumberOrCreate("+380123456789").block();

        StepVerifier.create(userService.getUserByIdOrThrow(created.id()))
                .assertNext(user -> assertThat(user.id()).isEqualTo(created.id()))
                .verifyComplete();
    }

    @Test
    void getUserByPhoneNumberOrCreateCreatesNewUser() {
        StepVerifier.create(userService.getUserByPhoneNumberOrCreate("+380123456789"))
                .assertNext(user -> {
                    assertThat(user.id()).isPositive();
                    assertThat(user.phoneNumber()).isEqualTo("+380123456789");
                    assertThat(user.active()).isTrue();
                })
                .verifyComplete();
    }

    @Test
    void getUserByPhoneNumberOrCreateReturnsExistingUser() {
        User created = userService.getUserByPhoneNumberOrCreate("+380123456789").block();

        StepVerifier.create(userService.getUserByPhoneNumberOrCreate("+380123456789"))
                .assertNext(user -> assertThat(user.id()).isEqualTo(created.id()))
                .verifyComplete();
    }

    @Test
    void updateDisplayNameUpdatesTheUser() {
        User created = userService.getUserByPhoneNumberOrCreate("+380123456789").block();

        userService.updateDisplayName(created.id(), "New Name").block();

        StepVerifier.create(userService.getUserByIdOrThrow(created.id()))
                .assertNext(user -> assertThat(user.displayName()).isEqualTo("New Name"))
                .verifyComplete();
    }

    @Test
    void getUserByIdOrThrowReturnsNotFoundForNonExistentId() {
        StepVerifier.create(userService.getUserByIdOrThrow(99999L))
                .expectErrorSatisfies(ex -> {
                    assertThat(ex).isInstanceOf(ApiException.class);
                    assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                })
                .verify();
    }
}
