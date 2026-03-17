package edu.automarket.user;

import edu.automarket.AbstractIntegrationTest;
import edu.automarket.authentication.AuthenticationService;
import edu.automarket.user.dto.AuthRequestDTO;
import edu.automarket.user.dto.AuthResponseDTO;
import edu.automarket.user.dto.RegisterRequestDTO;
import edu.automarket.user.dto.UserDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import static edu.automarket.TestUtils.testUser;
import static org.assertj.core.api.Assertions.assertThat;

class UserControllerTest extends AbstractIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthenticationService authenticationService;

    private RegisterRequestDTO validRegisterRequest() {
        return new RegisterRequestDTO("testuser", "+123456789012", "hash", "Test User");
    }

    // --- POST /api/users/register ---

    @Test
    void registerReturns201WithUserData() {
        webTestClient.post()
                .uri("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(validRegisterRequest())
                .exchange()
                .expectStatus().isCreated()
                .expectBody(UserDTO.class)
                .value(dto -> {
                    assertThat(dto.id()).isNotNull();
                    assertThat(dto.username()).isEqualTo("testuser");
                    assertThat(dto.phoneNumber()).isEqualTo("+123456789012");
                    assertThat(dto.displayName()).isEqualTo("Test User");
                    assertThat(dto.active()).isTrue();
                });
    }

    @Test
    void registerDuplicateUsernameReturns409() {
        webTestClient.post().uri("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(validRegisterRequest())
                .exchange().expectStatus().isCreated();

        webTestClient.post().uri("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(validRegisterRequest())
                .exchange().expectStatus().isEqualTo(409);
    }

    @Test
    void registerWithBlankUsernameReturns400() {
        webTestClient.post().uri("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new RegisterRequestDTO("   ", "+123456789012", "hash", "Test User"))
                .exchange().expectStatus().isBadRequest();
    }

    @Test
    void registerWithDisallowedUsernameCharactersReturns400() {
        webTestClient.post().uri("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new RegisterRequestDTO("bad user!", "+123456789012", "hash", "Test User"))
                .exchange().expectStatus().isBadRequest();
    }

    @Test
    void registerWithTooShortUsernameReturns400() {
        webTestClient.post().uri("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new RegisterRequestDTO("ab", "+123456789012", "hash", "Test User"))
                .exchange().expectStatus().isBadRequest();
    }

    @Test
    void registerWithDisallowedDisplayNameCharactersReturns400() {
        webTestClient.post().uri("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new RegisterRequestDTO("testuser", "+123456789012", "hash", "Bad@Name"))
                .exchange().expectStatus().isBadRequest();
    }

    @Test
    void registerWithInvalidPhoneNumberReturns400() {
        webTestClient.post().uri("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new RegisterRequestDTO("testuser", "12345", "hash", "Test User"))
                .exchange().expectStatus().isBadRequest();
    }

    // --- POST /api/users/auth ---

    @Test
    void authenticateWithValidCredentialsReturnsToken() {
        userRepository.save(testUser("testUser")).block();

        webTestClient.post().uri("/api/users/auth")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new AuthRequestDTO("testUser", "hash"))
                .exchange()
                .expectStatus().isOk()
                .expectBody(AuthResponseDTO.class)
                .value(dto -> {
                    assertThat(dto.token()).isNotBlank();
                    assertThat(dto.username()).isEqualTo("testUser");
                    assertThat(dto.tokenExpiresInSeconds()).isPositive();
                });
    }

    @Test
    void authenticateWithWrongPasswordReturns401() {
        userRepository.save(testUser("testuser")).block();

        webTestClient.post().uri("/api/users/auth")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new AuthRequestDTO("testuser", "wronghash"))
                .exchange().expectStatus().isUnauthorized();
    }

    @Test
    void authenticateWithNonExistentUsernameReturns401() {
        webTestClient.post().uri("/api/users/auth")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new AuthRequestDTO("ghost", "hash"))
                .exchange().expectStatus().isUnauthorized();
    }

    @Test
    void authenticateWithDisallowedUsernameCharactersReturns400() {
        webTestClient.post().uri("/api/users/auth")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new AuthRequestDTO("bad user!", "hash"))
                .exchange().expectStatus().isBadRequest();
    }

    @Test
    void authenticateWithInactiveUserReturns401() {
        User testUSer = new User(null, "testUser", "+123456789012", "hash", "Test User", System.currentTimeMillis(), false);
        userRepository.save(testUSer).block();

        webTestClient.post().uri("/api/users/auth")
                     .contentType(MediaType.APPLICATION_JSON)
                     .bodyValue(new AuthRequestDTO("testUser", "hash"))
                     .exchange().expectStatus().isUnauthorized();
    }

    // --- GET /api/users/profile ---

    @Test
    void getProfileWithValidTokenReturnsUserData() {
        User user = userRepository.save(testUser("testUser")).block();
        String token = authenticationService.generateToken(user.id());

        webTestClient.get().uri("/api/users/profile")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody(UserDTO.class)
                .value(dto -> {
                    assertThat(dto.id()).isEqualTo(user.id());
                    assertThat(dto.username()).isEqualTo("testUser");
                });
    }

    @Test
    void getProfileWithoutTokenReturns401() {
        webTestClient.get().uri("/api/users/profile")
                .exchange().expectStatus().isUnauthorized();
    }

    @Test
    void getProfileWithInvalidTokenReturns401() {
        webTestClient.get().uri("/api/users/profile")
                .header("Authorization", "Bearer garbage.token.value")
                .exchange().expectStatus().isUnauthorized();
    }
}
