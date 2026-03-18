package edu.automarket.authentication;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Base64;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthenticationServiceTest {
    private AuthenticationService service;
    private String secret;

    @BeforeEach
    void setUp() {
        byte[] rawSecret = new byte[64];
        new Random().nextBytes(rawSecret);
        secret = Base64.getEncoder().encodeToString(rawSecret);

        service = new AuthenticationService(secret, Duration.ofHours(1), new ObjectMapper());
    }

    @Test
    void generatedTokenContainsCorrectUserId() {
        String token = service.generateToken(42L);
        assertThat(service.validateAndExtractUserId(token)).isEqualTo(42L);
    }

    @Test
    void differentUserIdsProduceDifferentTokens() {
        assertThat(service.generateToken(1L)).isNotEqualTo(service.generateToken(2L));
    }

    @Test
    void tokenExpirationMatchesConfiguredDuration() {
        AuthenticationService twoHour = new AuthenticationService(secret, Duration.ofHours(2), new ObjectMapper());
        assertThat(twoHour.tokenExpirationSeconds()).isEqualTo(7200);
    }

    @Test
    void tokenSignedByDifferentSecretThrowsSecurityException() throws ExecutionException, InterruptedException {
        byte[] rawSecret = new byte[64];
        new Random().nextBytes(rawSecret);
        String otherSecret = Base64.getEncoder().encodeToString(rawSecret);
        AuthenticationService other = new AuthenticationService(otherSecret, Duration.ofHours(1), new ObjectMapper());

        // we have to sign the key in a separate key, so that it's secret don't get to the ThreadLocal,
        // that is used when service.validateAndExtractUserId(foreignToken) is invoked
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        String foreignToken = CompletableFuture.supplyAsync(() -> other.generateToken(1L), executorService).get();
        executorService.shutdown();

        assertThatThrownBy(() -> service.validateAndExtractUserId(foreignToken))
                .isInstanceOf(SecurityException.class)
                .hasMessage("Invalid JWT signature");
    }

    @Test
    void tamperedSignatureThrowsSecurityException() {
        String token = service.generateToken(1L);
        String tampered = token.substring(0, token.length() - 4) + "XXXX";

        assertThatThrownBy(() -> service.validateAndExtractUserId(tampered))
                .isInstanceOf(SecurityException.class)
                .hasMessage("Invalid JWT signature");
    }

    @Test
    void expiredTokenThrowsSecurityException() throws InterruptedException {
        AuthenticationService shortLived = new AuthenticationService(secret, Duration.ofSeconds(1), new ObjectMapper());
        String token = shortLived.generateToken(1L);
        Thread.sleep(2100);

        assertThatThrownBy(() -> shortLived.validateAndExtractUserId(token))
                .isInstanceOf(SecurityException.class)
                .hasMessage("JWT expired");
    }

    @Test
    void tokenNotMatchingExpectedHeaderReturnsNull() {
        assertThatThrownBy(() -> service.validateAndExtractUserId("garbage.token.value"))
                .isInstanceOf(SecurityException.class)
                .hasMessage("Invalid JWT token");
    }
}
