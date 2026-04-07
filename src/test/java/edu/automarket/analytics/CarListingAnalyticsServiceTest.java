package edu.automarket.analytics;

import edu.automarket.AbstractIntegrationTest;
import edu.automarket.listing.CarListingService;
import edu.automarket.listing.model.CarListing;
import edu.automarket.user.UserService;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.r2dbc.core.DatabaseClient;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CarListingAnalyticsServiceTest extends AbstractIntegrationTest {

    @Autowired
    private CarListingAnalyticsService service;

    @Autowired
    private UserService userService;

    @Autowired
    private CarListingService carListingService;

    @Autowired
    private CarListingAnalyticsRepository analyticsRepository;

    @Autowired
    private DatabaseClient databaseClient;

    @Test
    void recordListingImpression_incrementsImpressionCount() {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        CarListing listing = carListingService.create(userId).block();

        service.recordListingImpression(listing.id());
        service.recordListingImpression(listing.id());
        service.recordListingImpression(listing.id());
        service.saveListingAnalytics();

        AggregatedListingsAnalyticsDTO listingDTO = AggregatedListingsAnalyticsDTO.compute(service, listing.id());
        assertThat(listingDTO).isNotNull();
        assertThat(listingDTO.impressionsCount()).isEqualTo(3);
    }

    @Test
    void recordListingView_incrementsViewCount() {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        CarListing listing = carListingService.create(userId).block();

        service.recordListingView(listing.id());
        service.recordListingView(listing.id());
        service.saveListingAnalytics();

        AggregatedListingsAnalyticsDTO listingDTO = AggregatedListingsAnalyticsDTO.compute(service, listing.id());
        assertThat(listingDTO).isNotNull();
        assertThat(listingDTO.viewsCount()).isEqualTo(2);
    }

    @Test
    void recordListingPhoneRequest_incrementsPhoneRequestCount() {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        CarListing listing = carListingService.create(userId).block();

        service.recordListingPhoneRequest(listing.id());
        service.saveListingAnalytics();

        AggregatedListingsAnalyticsDTO listingDTO = AggregatedListingsAnalyticsDTO.compute(service, listing.id());
        assertThat(listingDTO).isNotNull();
        assertThat(listingDTO.phoneRequestsCount()).isEqualTo(1);
    }

    @Test
    void recordListingAddedToFavourity_incrementsFavouritesCount() {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        CarListing listing = carListingService.create(userId).block();

        service.recordListingAddedToFavourity(listing.id());
        service.recordListingAddedToFavourity(listing.id());
        service.saveListingAnalytics();

        AggregatedListingsAnalyticsDTO listingDTO = AggregatedListingsAnalyticsDTO.compute(service, listing.id());
        assertThat(listingDTO).isNotNull();
        assertThat(listingDTO.favouritesCount()).isEqualTo(2);
    }

    @Test
    void saveListingAnalytics_clearsCountersAfterSave() {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        CarListing listing = carListingService.create(userId).block();

        service.recordListingImpression(listing.id());
        service.saveListingAnalytics();
        service.saveListingAnalytics(); // second flush — no new entries

        AggregatedListingsAnalyticsDTO listingDTO = AggregatedListingsAnalyticsDTO.compute(service, listing.id());
        assertThat(listingDTO).isNotNull();
        assertThat(listingDTO.impressionsCount()).isEqualTo(1);
    }

    @Test
    void clearOldAnalytics() {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        long listingId = carListingService.create(userId).block().id();

        long now = System.currentTimeMillis();
        long yearAgoTS = now - Duration.ofDays(365).toMillis() - 1_000;

        CarListingAnalyticsCounter counter1 = new CarListingAnalyticsCounter();
        counter1.impressionsCount().add(3);

        CarListingAnalyticsCounter counter2 = new CarListingAnalyticsCounter();
        counter2.impressionsCount().add(5);

        analyticsRepository.saveAnalytics(now, List.of(Map.entry(listingId, counter1))).block();
        analyticsRepository.saveAnalytics(yearAgoTS, List.of(Map.entry(listingId, counter2))).block();

        StepVerifier.create(databaseClient.sql("SELECT SUM(impressions_count) FROM car_listing_analytics").map(row -> row.get(0, Long.class)).one())
                .assertNext(count -> AssertionsForClassTypes.assertThat(count).isEqualTo(8));

        service.clearOldAnalytics();

        StepVerifier.create(databaseClient.sql("SELECT SUM(impressions_count) FROM car_listing_analytics").map(row -> row.get(0, Long.class)).one())
                .assertNext(count -> AssertionsForClassTypes.assertThat(count).isEqualTo(3));
    }

}