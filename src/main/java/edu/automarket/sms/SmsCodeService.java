package edu.automarket.sms;

import edu.automarket.common.ApiException;
import edu.automarket.sms.dto.PreludeSendRequestDTO;
import edu.automarket.sms.dto.PreludeTargetDTO;
import edu.automarket.sms.dto.PreludeVerifyRequestDTO;
import edu.automarket.sms.dto.PreludeVerifyResponseDTO;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
public class SmsCodeService {
    private static final Logger log = LogManager.getLogger(SmsCodeService.class);
    private static final String PRELUDE_SEND_URL = "https://api.prelude.dev/v2/verification";
    private static final String PRELUDE_CHECK_URL = "https://api.prelude.dev/v2/verification/check";

    private final WebClient webClient;
    private final String preludeApiToken;
    private final int authCodeTTLSeconds;

    public SmsCodeService(WebClient webClient,
                          @Value("${app.sms.preludeApiToken:}") String preludeApiToken,
                          @Value("${app.sms.authCodeTtl:1m}") Duration authCodeTtl) {
        this.webClient = webClient;
        this.preludeApiToken = preludeApiToken == null || preludeApiToken.isBlank() ? null : preludeApiToken;
        this.authCodeTTLSeconds = (int) authCodeTtl.toSeconds();

        if (this.preludeApiToken == null) {
            log.warn("Prelude API token is not set. SMS will not be sent and any code will be accepted.");
        }
    }

    public Mono<Void> sendSms(String phoneNumber) {
        if (preludeApiToken == null) {
            log.info("Dev mode: skipping SMS send for {}", phoneNumber);
            return Mono.empty();
        }

        var body = new PreludeSendRequestDTO(
                new PreludeSendRequestDTO.Options("auto", "uk", 6),
                new PreludeTargetDTO(phoneNumber)
        );

        return webClient.post()
                        .uri(PRELUDE_SEND_URL)
                        .header("Authorization", "Bearer " + preludeApiToken)
                        .header("Content-Type", "application/json")
                        .bodyValue(body)
                        .retrieve()
                        .bodyToMono(Void.class);
    }

    public Mono<Void> validateSmsCode(String phoneNumber, String code) {
        if (preludeApiToken == null) {
            return Mono.empty();
        }

        return webClient.post()
                        .uri(PRELUDE_CHECK_URL)
                        .header("Authorization", "Bearer " + preludeApiToken)
                        .header("Content-Type", "application/json")
                        .bodyValue(new PreludeVerifyRequestDTO(phoneNumber, code))
                        .retrieve()
                        .bodyToMono(PreludeVerifyResponseDTO.class)
                        .flatMap(response -> "success".equals(response.status())
                                ? Mono.empty()
                                : Mono.error(new ApiException(HttpStatus.UNAUTHORIZED, "/problems/invalid-sms-code", "Invalid SMS code")));
    }

    public int getAuthCodeTTLSeconds() {
        return authCodeTTLSeconds;
    }
}
