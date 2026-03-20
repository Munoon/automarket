package edu.automarket.sms;

import edu.automarket.sms.dto.TelegramGatewayAPIRequestDTO;
import edu.automarket.sms.dto.TelegramGatewayAPIResponseDTO;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Service
public class SmsCodeService {
    private static final Logger log = LogManager.getLogger(SmsCodeService.class);
    private final SecureRandom secureRandom = new SecureRandom();
    private final SmsCodeRepository smsCodeRepository;
    private final WebClient webClient;
    private final String telegramApiToken;
    private final int authCodeTTLSeconds;

    public SmsCodeService(SmsCodeRepository smsCodeRepository,
                          WebClient webClient,
                          @Value("${app.sms.telegramApiToken:}") String telegramApiToken,
                          @Value("${app.sms.authCodeTtl:1m}") Duration authCodeTtl) {
        this.smsCodeRepository = smsCodeRepository;
        this.webClient = webClient;
        this.telegramApiToken = telegramApiToken == null || telegramApiToken.isBlank() ? null : telegramApiToken;
        this.authCodeTTLSeconds = (int) authCodeTtl.toSeconds();

        if (this.telegramApiToken == null) {
            log.warn("Telegram API token is not set. SMS will not be sent.");
        }
    }

    public Mono<Void> sendSms(String phoneNumber) {
        String authCode = String.format("%06d", secureRandom.nextInt(1000000));

        return smsCodeRepository.saveCode(phoneNumber, authCode)
                .doOnSuccess(_ -> sendSmsViaTelegram(phoneNumber, authCode));
    }

    public Mono<Void> validateSmsCode(String phoneNumber, String code) {
        long minCreatedAt = System.currentTimeMillis() - authCodeTTLSeconds * 1000L;
        return smsCodeRepository.validateSmsCodeAndDelete(phoneNumber, code, minCreatedAt)
                .flatMap(valid -> valid
                        ? Mono.empty()
                        : Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid SMS code")));
    }

    private void sendSmsViaTelegram(String phoneNumber, String authCode) {
        if (telegramApiToken == null) {
            log.info("SMS code for {}: {}", phoneNumber, authCode);
            return;
        }

        webClient.post()
                 .uri("https://gatewayapi.telegram.org/sendVerificationMessage")
                 .header("Authorization", "Bearer " + telegramApiToken)
                 .header("Content-Type", "application/json")
                 .bodyValue(new TelegramGatewayAPIRequestDTO(phoneNumber, authCode, authCodeTTLSeconds))
                 .retrieve()
                 .bodyToMono(TelegramGatewayAPIResponseDTO.class)
                 .doOnNext(result -> {
                     if (!result.ok()) {
                         log.warn("Failed to send SMS code for {}. Auth code: {}", phoneNumber, authCode);
                     }
                 })
                 .doOnError(ex -> log.error("Failed to send SMS code for {}. Auth code: {}", phoneNumber, authCode, ex))
                 .subscribe();
    }

    @Scheduled(fixedRate = 1, initialDelay = 1, timeUnit = TimeUnit.HOURS)
    public void deleteExpiredCodes() {
        long minCreatedAt = System.currentTimeMillis() - authCodeTTLSeconds * 1000L;
        Long deleted = smsCodeRepository.deleteExpiredCodes(minCreatedAt).block();
        if (deleted != null && deleted > 0) {
            log.info("Deleted {} expired SMS codes", deleted);
        }
    }

    public int getAuthCodeTTLSeconds() {
        return authCodeTTLSeconds;
    }
}
