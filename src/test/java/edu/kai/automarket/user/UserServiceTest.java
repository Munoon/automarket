package edu.kai.automarket.user;

import edu.kai.automarket.AbstractIntegrationTest;
import edu.kai.automarket.user.dto.RegisterRequestDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

class UserServiceTest extends AbstractIntegrationTest {

    @Autowired
    private UserService userService;

    private RegisterRequestDTO registerRequest(String username) {
        return new RegisterRequestDTO(username, "+123456789012", "hash123", "Display Name");
    }

    @Test
    void registerCreatesActiveUserWithAssignedId() {
        StepVerifier.create(userService.register(registerRequest("newuser")))
                .assertNext(user -> {
                    assertThat(user.id()).isNotNull();
                    assertThat(user.username()).isEqualTo("newuser");
                    assertThat(user.phoneNumber()).isEqualTo("+123456789012");
                    assertThat(user.active()).isTrue();
                })
                .verifyComplete();
    }

    @Test
    void registerDuplicateUsernameReturnsConflict() {
        userService.register(registerRequest("dupuser")).block();

        StepVerifier.create(userService.register(registerRequest("dupuser")))
                .expectErrorSatisfies(ex -> {
                    assertThat(ex).isInstanceOf(ResponseStatusException.class);
                    assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                })
                .verify();
    }

    @Test
    void getUserByUsernameReturnsExistingUser() {
        userService.register(registerRequest("findme")).block();

        StepVerifier.create(userService.getUserByUsername("findme"))
                .assertNext(user -> assertThat(user.username()).isEqualTo("findme"))
                .verifyComplete();
    }

    @Test
    void getUserByUsernameReturnsEmptyForNonExistentUser() {
        StepVerifier.create(userService.getUserByUsername("nobody"))
                .verifyComplete();
    }

    @Test
    void getUserByIdOrThrowReturnsExistingUser() {
        User created = userService.register(registerRequest("byid")).block();

        StepVerifier.create(userService.getUserByIdOrThrow(created.id()))
                .assertNext(user -> assertThat(user.id()).isEqualTo(created.id()))
                .verifyComplete();
    }

    @Test
    void getUserByIdOrThrowReturnsNotFoundForNonExistentId() {
        StepVerifier.create(userService.getUserByIdOrThrow(99999L))
                .expectErrorSatisfies(ex -> {
                    assertThat(ex).isInstanceOf(ResponseStatusException.class);
                    assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                })
                .verify();
    }
}
