package edu.automarket.analytics;

import edu.automarket.AbstractIntegrationTest;
import edu.automarket.TestUtils;
import edu.automarket.common.PageDTO;
import edu.automarket.listing.CarListingService;
import edu.automarket.listing.dto.OwnCarListingListItemDTO;
import edu.automarket.listing.model.CarListing;
import edu.automarket.user.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

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

    @Test
    void recordListingImpression_incrementsImpressionCount() {
        long userId = userService.register(TestUtils.testUser("anasvc1")).block().id();
        CarListing listing = carListingService.create(userId).block();

        service.recordListingImpression(listing.id());
        service.recordListingImpression(listing.id());
        service.recordListingImpression(listing.id());
        service.saveListingAnalytics();

        OwnCarListingListItemDTO listingDTO = queryAnalytics(userId, listing.id());
        assertThat(listingDTO).isNotNull();
        assertThat(listingDTO.impressionsCount()).isEqualTo(3);
    }

    @Test
    void recordListingView_incrementsViewCount() {
        long userId = userService.register(TestUtils.testUser("anasvc2")).block().id();
        CarListing listing = carListingService.create(userId).block();

        service.recordListingView(listing.id());
        service.recordListingView(listing.id());
        service.saveListingAnalytics();

        OwnCarListingListItemDTO listingDTO = queryAnalytics(userId, listing.id());
        assertThat(listingDTO).isNotNull();
        assertThat(listingDTO.viewsCount()).isEqualTo(2);
    }

    @Test
    void recordListingPhoneRequest_incrementsPhoneRequestCount() {
        long userId = userService.register(TestUtils.testUser("anasvc3")).block().id();
        CarListing listing = carListingService.create(userId).block();

        service.recordListingPhoneRequest(listing.id());
        service.saveListingAnalytics();

        OwnCarListingListItemDTO listingDTO = queryAnalytics(userId, listing.id());
        assertThat(listingDTO).isNotNull();
        assertThat(listingDTO.phoneRequestsCount()).isEqualTo(1);
    }

    @Test
    void recordListingAddedToFavourity_incrementsFavouritesCount() {
        long userId = userService.register(TestUtils.testUser("anasvc4")).block().id();
        CarListing listing = carListingService.create(userId).block();

        service.recordListingAddedToFavourity(listing.id());
        service.recordListingAddedToFavourity(listing.id());
        service.saveListingAnalytics();

        OwnCarListingListItemDTO listingDTO = queryAnalytics(userId, listing.id());
        assertThat(listingDTO).isNotNull();
        assertThat(listingDTO.favouritesCount()).isEqualTo(2);
    }

    @Test
    void saveListingAnalytics_clearsCountersAfterSave() {
        long userId = userService.register(TestUtils.testUser("anasvc5")).block().id();
        CarListing listing = carListingService.create(userId).block();

        service.recordListingImpression(listing.id());
        service.saveListingAnalytics();
        service.saveListingAnalytics(); // second flush — no new entries

        OwnCarListingListItemDTO listingDTO = queryAnalytics(userId, listing.id());
        assertThat(listingDTO).isNotNull();
        assertThat(listingDTO.impressionsCount()).isEqualTo(1);
    }

    @Test
    void clearOldAnalytics() {
        long userId = userService.register(TestUtils.testUser("anarepo4")).block().id();
        long listingId = carListingService.create(userId).block().id();

        long now = System.currentTimeMillis();
        long yearAgoTS = now - Duration.ofDays(365).toMillis() - 1_000;

        CarListingAnalyticsCounter counter1 = new CarListingAnalyticsCounter();
        counter1.impressionsCount().add(3);

        CarListingAnalyticsCounter counter2 = new CarListingAnalyticsCounter();
        counter2.impressionsCount().add(5);

        analyticsRepository.saveAnalytics(now, List.of(Map.entry(listingId, counter1))).block();
        analyticsRepository.saveAnalytics(yearAgoTS, List.of(Map.entry(listingId, counter2))).block();

        OwnCarListingListItemDTO listingDTO = queryAnalytics(userId, listingId);
        assertThat(listingDTO).isNotNull();
        assertThat(listingDTO.impressionsCount()).isEqualTo(8);

        service.clearOldAnalytics();

        listingDTO = queryAnalytics(userId, listingId);
        assertThat(listingDTO).isNotNull();
        assertThat(listingDTO.impressionsCount()).isEqualTo(3);
    }

    private OwnCarListingListItemDTO queryAnalytics(long userId, long listingId) {
        PageDTO<OwnCarListingListItemDTO> page = carListingService.getOwnListings(userId, null, 0, 100).block();
        for (OwnCarListingListItemDTO itemDTO : page.content()) {
            if (itemDTO.id() == listingId) {
                return itemDTO;
            }
        }
        return null;
    }
}