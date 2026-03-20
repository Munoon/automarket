package edu.automarket;

import edu.automarket.analytics.CarListingAnalyticsService;
import edu.automarket.sms.dto.TelegramGatewayAPIResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@AutoConfigureWebTestClient
@TestPropertySource(properties = {
        "app.listing.republishCooldown=2s",
        "app.sms.telegramApiToken=test-token",
        "app.jwt.secret=1234567890"
})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AbstractIntegrationTest {

    @MockitoBean
    protected WebClient webClient;

    protected WebClient.RequestBodySpec telegramRequestBodySpec;

    @Autowired
    private DatabaseClient databaseClient;

    @Autowired
    private CarListingAnalyticsService carListingAnalyticsService;

    @BeforeEach
    void truncateTables() {
        carListingAnalyticsService.saveListingAnalytics(); // flush stale in-memory counters before listings are gone
        databaseClient.sql("TRUNCATE TABLE sms_verification_codes, car_listing_analytics, car_listings, users RESTART IDENTITY CASCADE")
                .fetch()
                .rowsUpdated()
                .block();
    }

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setupWebClientMock() {
        var uriSpec = mock(WebClient.RequestBodyUriSpec.class);
        telegramRequestBodySpec = mock(WebClient.RequestBodySpec.class);
        var headersSpec = mock(WebClient.RequestHeadersSpec.class);
        var responseSpec = mock(WebClient.ResponseSpec.class);

        when(webClient.post()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString())).thenReturn(telegramRequestBodySpec);
        when(telegramRequestBodySpec.header(anyString(), anyString())).thenReturn(telegramRequestBodySpec);
        when(telegramRequestBodySpec.bodyValue(any())).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(TelegramGatewayAPIResponseDTO.class))
                .thenReturn(Mono.just(new TelegramGatewayAPIResponseDTO(true)));
    }
}
