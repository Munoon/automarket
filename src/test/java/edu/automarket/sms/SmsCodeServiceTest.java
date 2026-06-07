package edu.automarket.sms;

import edu.automarket.AbstractIntegrationTest;
import edu.automarket.common.ApiException;
import edu.automarket.sms.dto.PreludeSendRequestDTO;
import edu.automarket.sms.dto.PreludeVerifyResponseDTO;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SmsCodeServiceTest extends AbstractIntegrationTest {

    @Autowired
    private SmsCodeService smsCodeService;

    @Test
    void sendSmsSendsVerificationCodeViaPreludeApi() {
        smsCodeService.sendSms("+380123456789").block();

        ArgumentCaptor<PreludeSendRequestDTO> captor = ArgumentCaptor.forClass(PreludeSendRequestDTO.class);
        verify(webClientBodySpec).bodyValue(captor.capture());

        PreludeSendRequestDTO request = captor.getValue();
        assertThat(request.target().value()).isEqualTo("+380123456789");
        assertThat(request.target().type()).isEqualTo("phone_number");
        assertThat(request.options().codeSize()).isEqualTo(6);
    }

    @Test
    void validateSmsCodeSucceedsWhenPreludeReturnsSuccess() {
        StepVerifier.create(smsCodeService.validateSmsCode("+380123456789", "123456"))
                .verifyComplete();
    }

    @Test
    void validateSmsCodeThrowsUnauthorizedWhenPreludeReturnsFailure() {
        when(responseSpec.bodyToMono(PreludeVerifyResponseDTO.class))
                .thenReturn(Mono.just(new PreludeVerifyResponseDTO("invalid_code")));

        StepVerifier.create(smsCodeService.validateSmsCode("+380123456789", "000000"))
                .expectErrorSatisfies(ex -> {
                    assertThat(ex).isInstanceOf(ApiException.class);
                    assertThat(((ApiException) ex).getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
                })
                .verify();
    }

    @Test
    void getAuthCodeTTLSecondsReturnsPositiveValue() {
        assertThat(smsCodeService.getAuthCodeTTLSeconds()).isEqualTo(60);
    }
}
