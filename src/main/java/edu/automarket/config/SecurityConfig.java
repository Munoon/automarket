package edu.automarket.config;

import edu.automarket.authentication.AuthenticationWebFilter;
import edu.automarket.common.ProblemDTO;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {
    private static final Logger log = LogManager.getLogger(SecurityConfig.class);
    private static final MediaType PROBLEM_JSON = MediaType.parseMediaType("application/problem+json");
    private static final ProblemDTO UNAUTHORIZED_PROBLEM =
            new ProblemDTO("/problems/unauthorized", "Unauthorized", HttpStatus.UNAUTHORIZED.value());

    private final AuthenticationWebFilter jwtAuthFilter;
    private final ObjectMapper objectMapper;

    public SecurityConfig(AuthenticationWebFilter jwtAuthFilter, ObjectMapper objectMapper) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.objectMapper = objectMapper;
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers("/api/users/send-verification-code", "/api/users/auth").permitAll()
                        .pathMatchers("/api/listings/public/**").permitAll()
                        .anyExchange().authenticated()
                )
                .exceptionHandling(spec -> spec.authenticationEntryPoint(this::writeProblemResponse))
                .addFilterAt(jwtAuthFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .build();
    }

    private Mono<Void> writeProblemResponse(ServerWebExchange exchange, AuthenticationException ex) {
        log.error("Handled authentication exception", ex);

        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(PROBLEM_JSON);
        try {
            byte[] bytes = objectMapper.writeValueAsBytes(SecurityConfig.UNAUTHORIZED_PROBLEM);
            DataBuffer buffer = response.bufferFactory().wrap(bytes);
            return response.writeWith(Mono.just(buffer));
        } catch (JacksonException e) {
            return Mono.error(e);
        }
    }

    @Bean
    public WebClient webClient() {
        return WebClient.create();
    }
}
