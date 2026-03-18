package edu.automarket.authentication;

import edu.automarket.AbstractIntegrationTest;
import edu.automarket.user.User;
import edu.automarket.user.UserRepository;
import edu.automarket.user.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.reactive.server.WebTestClient;

import static edu.automarket.TestUtils.testUser;

class AuthenticationWebFilterTest extends AbstractIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private AuthenticationService authenticationService;

    @Autowired
    private UserService userService;

    @Test
    void requestWithoutAuthorizationHeaderIsRejected() {
        webTestClient.get()
                .uri("/api/users/profile")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void requestWithValidBearerTokenIsAuthenticated() {
        User user = userService.register(testUser("filteruser")).block();
        String token = authenticationService.generateToken(user.id());

        webTestClient.get()
                .uri("/api/users/profile")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void requestWithTamperedTokenIsRejected() {
        String token = authenticationService.generateToken(1L);
        String tampered = token.substring(0, token.length() - 4) + "XXXX";

        webTestClient.get()
                .uri("/api/users/profile")
                .header("Authorization", "Bearer " + tampered)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void requestWithNonBearerPrefixIsRejected() {
        User user = userService.register(testUser("filteruser2")).block();
        String token = authenticationService.generateToken(user.id());

        webTestClient.get()
                .uri("/api/users/profile")
                .header("Authorization", "Token " + token)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void requestWithTokenFromUnknownUserIsRejected() {
        String token = authenticationService.generateToken(99999L);

        webTestClient.get()
                .uri("/api/users/profile")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isNotFound();
    }
}
