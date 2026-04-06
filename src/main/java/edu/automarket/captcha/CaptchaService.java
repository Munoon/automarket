package edu.automarket.captcha;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;

@Service
public class CaptchaService {
    private static final Logger log = LoggerFactory.getLogger(CaptchaService.class);
    private final String captchaSiteKey;
    private final String captchaSecret;
    private final String captchaEndpoint;
    private final WebClient webClient;

    public CaptchaService(@Value("${app.captcha.sitekey:}") String captchaSiteKey,
                          @Value("${app.captcha.secret:}") String captchaSecret,
                          @Value("${app.captcha.endpoint:https://api.hcaptcha.com/siteverify}") String captchaEndpoint,
                          WebClient webClient) {
        this.captchaSiteKey = captchaSiteKey != null ? captchaSiteKey : "";
        this.captchaSecret = captchaSecret != null ? captchaSecret : "";
        this.captchaEndpoint = captchaEndpoint != null ? captchaEndpoint : "";
        this.webClient = webClient;

        if (!enabled()) {
            log.warn("Captcha is not enabled. " +
                    "Please set app.captcha.sitekey and app.captcha.secret to enable captcha verification.");
        }
    }

    public Mono<Boolean> validateCaptcha(String token, ServerHttpRequest httpRequest) {
        if (!enabled()) {
            return Mono.just(true);
        }

        if (token == null || token.isBlank()) {
            return Mono.just(false);
        }

        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("secret", captchaSecret);
        builder.part("sitekey", captchaSiteKey);
        builder.part("response", token);

        InetSocketAddress remoteAddress = httpRequest.getRemoteAddress();
        if (remoteAddress != null) {
            String ip = remoteAddress.getAddress().getHostAddress();
            if (ip != null && !"127.0.0.1".equals(ip)) {
                builder.part("remoteip", ip);
            }
        }

        return webClient.post()
                .uri(captchaEndpoint)
                .header("Content-Type", "multipart/form-data")
                .body(BodyInserters.fromMultipartData(builder.build()))
                .retrieve()
                .bodyToMono(CaptchaResponseDTO.class)
                .map(CaptchaResponseDTO::success)
                .doOnError(error -> log.error("Error validating captcha", error))
                .onErrorReturn(false);
    }

    private boolean enabled() {
        return !captchaSiteKey.isBlank() && !captchaSecret.isBlank() && !captchaEndpoint.isBlank();
    }

    public String sitekey() {
        return enabled() ? captchaSiteKey : "";
    }
}
