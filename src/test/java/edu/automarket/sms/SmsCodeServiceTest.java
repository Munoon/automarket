package edu.automarket.sms;

import edu.automarket.AbstractIntegrationTest;
import edu.automarket.sms.dto.TelegramGatewayAPIRequestDTO;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.test.StepVerifier;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

class SmsCodeServiceTest extends AbstractIntegrationTest {

    @Autowired
    private SmsCodeService smsCodeService;

    @Autowired
    private SmsCodeRepository smsCodeRepository;

    @Autowired
    private DatabaseClient databaseClient;

    @Test
    void sendSmsSavesCodeToDatabase() {
        long before = System.currentTimeMillis();

        smsCodeService.sendSms("+380123456789").block();

        Map<String, Object> row = databaseClient
                .sql("SELECT code, created_at FROM sms_verification_codes WHERE phone_number = :phone")
                .bind("phone", "+380123456789")
                .fetch().one()
                .block();

        assertThat(row).isNotNull();
        assertThat((String) row.get("code")).matches("^[0-9]{6}$");
        assertThat((Long) row.get("created_at")).isGreaterThanOrEqualTo(before);
    }

    @Test
    void sendSmsSendsCodeViaTelegramApi() {
        smsCodeService.sendSms("+380123456789").block();

        ArgumentCaptor<TelegramGatewayAPIRequestDTO> captor = ArgumentCaptor.forClass(TelegramGatewayAPIRequestDTO.class);
        verify(telegramRequestBodySpec).bodyValue(captor.capture());

        TelegramGatewayAPIRequestDTO request = captor.getValue();
        assertThat(request.phoneNumber()).isEqualTo("+380123456789");
        assertThat(request.code()).matches("^[0-9]{6}$");
        assertThat(request.ttl()).isEqualTo(smsCodeService.getAuthCodeTTLSeconds());
    }

    @Test
    void validateSmsCodeSucceedsForValidCode() {
        smsCodeRepository.saveCode("+380123456789", "123456").block();

        StepVerifier.create(smsCodeService.validateSmsCode("+380123456789", "123456"))
                .verifyComplete();
    }

    @Test
    void validateSmsCodeThrowsUnauthorizedForInvalidCode() {
        StepVerifier.create(smsCodeService.validateSmsCode("+380123456789", "000000"))
                .expectErrorSatisfies(ex -> {
                    assertThat(ex).isInstanceOf(ResponseStatusException.class);
                    assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
                })
                .verify();
    }

    @Test
    void getAuthCodeTTLSecondsReturnsPositiveValue() {
        assertThat(smsCodeService.getAuthCodeTTLSeconds()).isEqualTo(60);
    }

    @Test
    void deleteExpiredCodesDeletesExpiredCodes() {
        databaseClient.sql("INSERT INTO sms_verification_codes (phone_number, code, created_at) VALUES (:phone, :code, :createdAt)")
                .bind("phone", "+380123456789")
                .bind("code", "123456")
                .bind("createdAt", 0L)
                .fetch().rowsUpdated().block();

        smsCodeService.deleteExpiredCodes();

        Long count = databaseClient.sql("SELECT COUNT(*) FROM sms_verification_codes")
                .map(row -> row.get(0, Long.class))
                .one().block();
        assertThat(count).isEqualTo(0L);
    }

    @Test
    void deleteExpiredCodesDoesNotDeleteFreshCodes() {
        smsCodeRepository.saveCode("+380123456789", "123456").block();

        smsCodeService.deleteExpiredCodes();

        Long count = databaseClient.sql("SELECT COUNT(*) FROM sms_verification_codes")
                .map(row -> row.get(0, Long.class))
                .one().block();
        assertThat(count).isEqualTo(1L);
    }
}
