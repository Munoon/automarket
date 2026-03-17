package edu.kai.automarket.authentication;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.Random;

@Service
public class AuthenticationService {
    private static final Logger log = LoggerFactory.getLogger(AuthenticationService.class);
    private static final String HEADER = base64url("{\"alg\":\"HS256\",\"typ\":\"JWT\"}") + ".";
    private final byte[] secretBytes;
    private final long expirationSeconds;
    private final ObjectMapper objectMapper;

    public AuthenticationService(@Value("${app.jwt.secret:}") String secret,
                                 @Value("${app.jwt.expiration:31d}") Duration expiration,
                                 ObjectMapper objectMapper) {
        if (secret == null || secret.isBlank()) {
            this.secretBytes = new byte[256];
            new Random().nextBytes(secretBytes);
            log.warn("Using random JWT secret. Please set app.jwt.secret property in production!");
        } else {
            this.secretBytes = Base64.getDecoder().decode(secret);
        }

        this.expirationSeconds = expiration.toSeconds();
        this.objectMapper = objectMapper;
    }

    public String generateToken(Long userId) {
        long nowSec = System.currentTimeMillis() / 1000;
        String payload = base64url(objectMapper.writeValueAsBytes(Map.of(
                "sub", userId.toString(),
                "iat", nowSec,
                "exp", nowSec + expirationSeconds
        )));
        String content = HEADER + payload;
        return content + "." + base64url(hmacSha256(content));
    }

    public Long validateAndExtractUserId(String token) {
        if (!token.startsWith(HEADER)) {
            return null;
        }

        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new SecurityException("Invalid JWT signature");
        }

        String payload = parts[1];
        String signature = parts[2];
        if (!MessageDigest.isEqual(hmacSha256(HEADER + payload), Base64.getUrlDecoder().decode(signature))) {
            throw new SecurityException("Invalid JWT signature");
        }

        Map<?, ?> claims = objectMapper.readValue(Base64.getUrlDecoder().decode(payload), Map.class);
        long exp = ((Number) claims.get("exp")).longValue();
        if (exp < System.currentTimeMillis() / 1000) {
            throw new SecurityException("JWT expired");
        }

        return Long.parseLong((String) claims.get("sub"));
    }

    public long tokenExpirationSeconds() {
        return expirationSeconds;
    }

    private byte[] hmacSha256(String input) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secretBytes, "HmacSHA256"));
            return mac.doFinal(input.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to compute HMAC-SHA256", e);
        }
    }

    private static String base64url(String s) {
        return base64url(s.getBytes(StandardCharsets.UTF_8));
    }

    private static String base64url(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
