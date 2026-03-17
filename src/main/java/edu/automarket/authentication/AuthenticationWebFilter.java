package edu.automarket.authentication;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Collections;

@Component
public class AuthenticationWebFilter implements WebFilter {
    private static final Logger log = LoggerFactory.getLogger(AuthenticationWebFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";
    private final AuthenticationService authenticationService;

    public AuthenticationWebFilter(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            try {
                String token = authHeader.substring(BEARER_PREFIX.length());
                Long userId = authenticationService.validateAndExtractUserId(token);
                Authentication auth = new UsernamePasswordAuthenticationToken(
                        userId, null, Collections.emptyList());
                return chain.filter(exchange)
                        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth));
            } catch (Exception e) {
                log.warn("Failed to authenticate user", e);
            }
        }
        return chain.filter(exchange);
    }
}
