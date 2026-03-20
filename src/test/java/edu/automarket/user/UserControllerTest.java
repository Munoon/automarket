package edu.automarket.user;

import edu.automarket.AbstractIntegrationTest;
import edu.automarket.authentication.AuthenticationService;
import edu.automarket.sms.SmsCodeRepository;
import edu.automarket.sms.SmsCodeService;
import edu.automarket.sms.dto.TelegramGatewayAPIRequestDTO;
import edu.automarket.user.dto.AuthRequestDTO;
import edu.automarket.user.dto.AuthResponseDTO;
import edu.automarket.user.dto.SendVerificationCodeRequestDTO;
import edu.automarket.user.dto.SendVerificationCodeResponseDTO;
import edu.automarket.user.dto.UpdateDisplayNameRequestDTO;
import edu.automarket.user.dto.UserDTO;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

class UserControllerTest extends AbstractIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private UserService userService;

    @Autowired
    private AuthenticationService authenticationService;

    @Autowired
    private SmsCodeRepository smsCodeRepository;

    @Autowired
    private DatabaseClient databaseClient;

    @MockitoSpyBean
    private SmsCodeService smsCodeService;

    // --- POST /api/users/send-verification-code ---

    @Test
    void sendVerificationCodeWithValidPhoneReturns200WithTtl() {
        webTestClient.post().uri("/api/users/send-verification-code")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new SendVerificationCodeRequestDTO("+380123456789"))
                .exchange()
                .expectStatus().isOk()
                .expectBody(SendVerificationCodeResponseDTO.class)
                .value(dto -> assertThat(dto.codeTimeToLiveSeconds()).isEqualTo(60));
    }

    @Test
    void sendVerificationCodeInsertsCodeToDbAndCallsTelegramApi() {
        long before = System.currentTimeMillis();

        webTestClient.post().uri("/api/users/send-verification-code")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new SendVerificationCodeRequestDTO("+380123456789"))
                .exchange()
                .expectStatus().isOk();

        Map<String, Object> row = databaseClient
                .sql("SELECT code, created_at FROM sms_verification_codes WHERE phone_number = :phone")
                .bind("phone", "+380123456789")
                .fetch().one()
                .block();

        assertThat(row).isNotNull();
        assertThat((String) row.get("code")).matches("^[0-9]{6}$");
        assertThat((Long) row.get("created_at")).isGreaterThanOrEqualTo(before);

        ArgumentCaptor<TelegramGatewayAPIRequestDTO> captor = ArgumentCaptor.forClass(TelegramGatewayAPIRequestDTO.class);
        verify(telegramRequestBodySpec, timeout(500)).bodyValue(captor.capture());
        assertThat(captor.getValue().phoneNumber()).isEqualTo("+380123456789");
    }

    @Test
    void sendVerificationCodeReturns500WhenSmsFails() {
        doReturn(Mono.error(new RuntimeException("SMS send failed"))).when(smsCodeService).sendSms(anyString());

        webTestClient.post().uri("/api/users/send-verification-code")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new SendVerificationCodeRequestDTO("+380123456789"))
                .exchange()
                .expectStatus().isEqualTo(500);
    }

    @Test
    void sendVerificationCodeWithInvalidPhoneFormatReturns400() {
        webTestClient.post().uri("/api/users/send-verification-code")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new SendVerificationCodeRequestDTO("12345"))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void sendVerificationCodeWithNullPhoneReturns400() {
        webTestClient.post().uri("/api/users/send-verification-code")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new SendVerificationCodeRequestDTO(null))
                .exchange()
                .expectStatus().isBadRequest();
    }

    // --- POST /api/users/auth ---

    @Test
    void authenticateWithValidCodeCreatesAndReturnsToken() {
        smsCodeRepository.saveCode("+380123456789", "123456").block();

        webTestClient.post().uri("/api/users/auth")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new AuthRequestDTO("+380123456789", "123456"))
                .exchange()
                .expectStatus().isOk()
                .expectBody(AuthResponseDTO.class)
                .value(dto -> {
                    assertThat(dto.token()).isNotBlank();
                    assertThat(dto.tokenExpiresInSeconds()).isPositive();
                    assertThat(dto.profile().phoneNumber()).isEqualTo("+380123456789");
                    assertThat(dto.limits()).isNotNull();
                });
    }

    @Test
    void authenticateWithValidCodeReturnsExistingUser() {
        User existing = userService.getUserByPhoneNumberOrCreate("+380123456789").block();
        smsCodeRepository.saveCode("+380123456789", "123456").block();

        webTestClient.post().uri("/api/users/auth")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new AuthRequestDTO("+380123456789", "123456"))
                .exchange()
                .expectStatus().isOk()
                .expectBody(AuthResponseDTO.class)
                .value(dto -> assertThat(dto.profile().id()).isEqualTo(existing.id()));
    }

    @Test
    void authenticateWithInvalidCodeReturns401() {
        webTestClient.post().uri("/api/users/auth")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new AuthRequestDTO("+380123456789", "000000"))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void authenticateWithInvalidPhoneFormatReturns400() {
        webTestClient.post().uri("/api/users/auth")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new AuthRequestDTO("12345", "123456"))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void authenticateWithInactiveUserReturns401() {
        userService.getUserByPhoneNumberOrCreate("+380123456789").block();
        databaseClient.sql("UPDATE users SET is_active = false WHERE phone_number = :phone")
                .bind("phone", "+380123456789")
                .fetch().rowsUpdated().block();
        smsCodeRepository.saveCode("+380123456789", "123456").block();

        webTestClient.post().uri("/api/users/auth")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new AuthRequestDTO("+380123456789", "123456"))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    // --- PATCH /api/users/display-name ---

    @Test
    void updateDisplayNameReturns204() {
        User user = userService.getUserByPhoneNumberOrCreate("+380123456789").block();
        String token = authenticationService.generateToken(user.id());

        webTestClient.patch().uri("/api/users/display-name")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new UpdateDisplayNameRequestDTO("John Doe"))
                .exchange()
                .expectStatus().isNoContent();

        webTestClient.get().uri("/api/users/profile")
                     .header("Authorization", "Bearer " + token)
                     .exchange()
                     .expectStatus().isOk()
                     .expectBody(UserDTO.class)
                     .value(dto -> assertThat(dto.displayName()).isEqualTo("John Doe"));
    }

    @Test
    void updateDisplayNameWithInvalidCharactersReturns400() {
        User user = userService.getUserByPhoneNumberOrCreate("+380123456789").block();
        String token = authenticationService.generateToken(user.id());

        webTestClient.patch().uri("/api/users/display-name")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new UpdateDisplayNameRequestDTO("Bad@Name123"))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void updateDisplayNameWithoutTokenReturns401() {
        webTestClient.patch().uri("/api/users/display-name")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new UpdateDisplayNameRequestDTO("John Doe"))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    // --- GET /api/users/profile ---

    @Test
    void getProfileWithValidTokenReturnsUserData() {
        User user = userService.getUserByPhoneNumberOrCreate("+380123456789").block();
        String token = authenticationService.generateToken(user.id());

        webTestClient.get().uri("/api/users/profile")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody(UserDTO.class)
                .value(dto -> {
                    assertThat(dto.id()).isEqualTo(user.id());
                    assertThat(dto.phoneNumber()).isEqualTo("+380123456789");
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
