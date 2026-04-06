package edu.automarket.config;

import edu.automarket.captcha.CaptchaService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Configuration
public class SpaRoutingConfig {
    private final CaptchaService captchaService;

    public SpaRoutingConfig(CaptchaService captchaService) {
        this.captchaService = captchaService;
    }

    @Bean
    public RouterFunction<ServerResponse> spaFallbackRoute() {
        return RouterFunctions.route(
                RequestPredicates.GET("/**").and(this::isSpaRoute),
                this::serveRoute
        );
    }

    private boolean isSpaRoute(ServerRequest request) {
        String path = request.path();
        return !path.equals("/api") && !path.startsWith("/api/") && !path.contains(".");
    }

    private Mono<ServerResponse> serveRoute(ServerRequest request) {
        String routePath = request.path().startsWith("/") ? request.path().substring(1) : request.path();
        Resource routeResource = new ClassPathResource("static/" + routePath + ".html");
        if (routeResource.exists()) {
            return serveResource(routeResource);
        }

        return serveResource(new ClassPathResource("static/fallback.html"));
    }

    private Mono<ServerResponse> serveResource(Resource resource) {
        // TODO improve performance
        try {
            String html = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            html = html.replace("%%HCAPTCHA_SITEKEY%%", captchaService.sitekey());
            return ServerResponse.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .bodyValue(html);
        } catch (IOException e) {
            return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
