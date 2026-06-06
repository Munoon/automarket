package edu.automarket.captcha;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CaptchaServiceTest {
    @Test
    void validateCaptcha() throws IOException {
        HttpServer mockServer = HttpServer.create(new InetSocketAddress(8989), 0);
        HttpHandler mockServerHandler = mock(HttpHandler.class);
        mockServer.createContext("/", mockServerHandler);
        mockServer.start();

        try {
            WebClient webClient = WebClient.builder().build();
            CaptchaService service = new CaptchaService("sitekey", "secret", "http://127.0.0.1:8989/", null, webClient);

            ServerHttpRequest httpRequest = mock();
            when(httpRequest.getRemoteAddress()).thenReturn(new InetSocketAddress("123.123.123.123", 8080));

            AtomicReference<String> lastRequest = new AtomicReference<>();
            doAnswer(answ -> {
                HttpExchange exchange = answ.getArgument(0, HttpExchange.class);
                lastRequest.set(new String(exchange.getRequestBody().readAllBytes()));

                byte[] response = "{\"success\": true}".getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.length);
                exchange.getResponseBody().write(response);
                exchange.close();

                return null;
            }).when(mockServerHandler).handle(any());

            StepVerifier.create(service.validateCaptcha("sample-token", httpRequest))
                    .assertNext(Assertions::assertTrue)
                    .verifyComplete();

            verify(mockServerHandler, times(1)).handle(any());
            assertThat(lastRequest.get()).contains("""
                    Content-Disposition: form-data; name="secret"\r
                    Content-Type: text/plain;charset=UTF-8\r
                    Content-Length: 6\r
                    \r
                    secret\r
                    """);
            assertThat(lastRequest.get()).contains("""
                    Content-Disposition: form-data; name="sitekey"\r
                    Content-Type: text/plain;charset=UTF-8\r
                    Content-Length: 7\r
                    \r
                    sitekey\r
                    """);
            assertThat(lastRequest.get()).contains("""
                    Content-Disposition: form-data; name="response"\r
                    Content-Type: text/plain;charset=UTF-8\r
                    Content-Length: 12\r
                    \r
                    sample-token\r
                    """);
            assertThat(lastRequest.get()).contains("""
                    Content-Disposition: form-data; name="remoteip"\r
                    Content-Type: text/plain;charset=UTF-8\r
                    Content-Length: 15\r
                    \r
                    123.123.123.123\r
                    """);

            doAnswer(answ -> {
                HttpExchange exchange = answ.getArgument(0, HttpExchange.class);
                lastRequest.set(new String(exchange.getRequestBody().readAllBytes()));

                byte[] response = "{\"success\": false}".getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.length);
                exchange.getResponseBody().write(response);
                exchange.close();

                return null;
            }).when(mockServerHandler).handle(any());
            StepVerifier.create(service.validateCaptcha("sample-token", httpRequest))
                    .assertNext(Assertions::assertFalse)
                    .verifyComplete();

            // connection refused exception to be thrown
            mockServer.stop(0);
            StepVerifier.create(service.validateCaptcha("sample-token", httpRequest))
                    .assertNext(Assertions::assertFalse)
                    .verifyComplete();
        } finally {
            mockServer.stop(0);
        }
    }

    @Test
    void validateCaptchaWithRemoteIpHeader() throws IOException {
        HttpServer mockServer = HttpServer.create(new InetSocketAddress(8990), 0);
        HttpHandler mockServerHandler = mock(HttpHandler.class);
        mockServer.createContext("/", mockServerHandler);
        mockServer.start();

        try {
            WebClient webClient = WebClient.builder().build();
            CaptchaService service = new CaptchaService("sitekey", "secret", "http://127.0.0.1:8990/", "X-Real-IP", webClient);

            ServerHttpRequest httpRequest = mock();
            HttpHeaders headers = new HttpHeaders();
            headers.add("X-Real-IP", "200.200.200.200");
            when(httpRequest.getHeaders()).thenReturn(headers);
            when(httpRequest.getRemoteAddress()).thenReturn(new InetSocketAddress("123.123.123.123", 8080));

            AtomicReference<String> lastRequest = new AtomicReference<>();
            doAnswer(answ -> {
                HttpExchange exchange = answ.getArgument(0, HttpExchange.class);
                lastRequest.set(new String(exchange.getRequestBody().readAllBytes()));

                byte[] response = "{\"success\": true}".getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.length);
                exchange.getResponseBody().write(response);
                exchange.close();

                return null;
            }).when(mockServerHandler).handle(any());

            StepVerifier.create(service.validateCaptcha("sample-token", httpRequest))
                    .assertNext(Assertions::assertTrue)
                    .verifyComplete();

            assertThat(lastRequest.get()).contains("""
                    Content-Disposition: form-data; name="remoteip"\r
                    Content-Type: text/plain;charset=UTF-8\r
                    Content-Length: 15\r
                    \r
                    200.200.200.200\r
                    """);
        } finally {
            mockServer.stop(0);
        }
    }

    @Test
    void validateCaptchaWithRemoteIpHeaderMissing() throws IOException {
        HttpServer mockServer = HttpServer.create(new InetSocketAddress(8991), 0);
        HttpHandler mockServerHandler = mock(HttpHandler.class);
        mockServer.createContext("/", mockServerHandler);
        mockServer.start();

        try {
            WebClient webClient = WebClient.builder().build();
            CaptchaService service = new CaptchaService("sitekey", "secret", "http://127.0.0.1:8991/", "X-Real-IP", webClient);

            ServerHttpRequest httpRequest = mock();
            HttpHeaders headers = new HttpHeaders();
            when(httpRequest.getHeaders()).thenReturn(headers);
            when(httpRequest.getRemoteAddress()).thenReturn(new InetSocketAddress("123.123.123.123", 8080));

            AtomicReference<String> lastRequest = new AtomicReference<>();
            doAnswer(answ -> {
                HttpExchange exchange = answ.getArgument(0, HttpExchange.class);
                lastRequest.set(new String(exchange.getRequestBody().readAllBytes()));

                byte[] response = "{\"success\": true}".getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.length);
                exchange.getResponseBody().write(response);
                exchange.close();

                return null;
            }).when(mockServerHandler).handle(any());

            StepVerifier.create(service.validateCaptcha("sample-token", httpRequest))
                    .assertNext(Assertions::assertTrue)
                    .verifyComplete();

            // Header is configured but not present in request — no remoteip should be sent
            assertThat(lastRequest.get()).doesNotContain("name=\"remoteip\"");
        } finally {
            mockServer.stop(0);
        }
    }

    @Test
    void sitekey() {
        CaptchaService service = new CaptchaService("sitekey", "secret", "http://127.0.0.1:8989/", null, mock());
        assertThat(service.sitekey()).isEqualTo("sitekey");

        service = new CaptchaService(null, "secret", "http://127.0.0.1:8989/", null, mock());
        assertThat(service.sitekey()).isEqualTo("");

        service = new CaptchaService("abc", "", "http://127.0.0.1:8989/", null, mock());
        assertThat(service.sitekey()).isEqualTo("");
    }
}