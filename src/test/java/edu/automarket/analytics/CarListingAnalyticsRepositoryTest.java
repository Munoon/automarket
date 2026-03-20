package edu.automarket.analytics;

import edu.automarket.AbstractIntegrationTest;
import edu.automarket.common.PageDTO;
import edu.automarket.listing.CarListingService;
import edu.automarket.listing.dto.OwnCarListingListItemDTO;
import edu.automarket.user.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class CarListingAnalyticsRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private CarListingAnalyticsRepository analyticsRepository;

    @Autowired
    private CarListingService carListingService;

    @Autowired
    private UserService userService;

    @Test
    void saveAnalytics_insertsRowWithCorrectCounters() {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        long listingId = carListingService.create(userId).block().id();

        CarListingAnalyticsCounter counter = new CarListingAnalyticsCounter();
        counter.impressionsCount().add(5);
        counter.viewsCount().add(3);
        counter.phoneRequestsCount().add(2);
        counter.favouritesCount().add(1);

        long ts = System.currentTimeMillis();
        analyticsRepository.saveAnalytics(ts, List.of(Map.entry(listingId, counter))).block();

        OwnCarListingListItemDTO listingDTO = queryAnalytics(userId, listingId);
        assertThat(listingDTO).isNotNull();
        assertThat(listingDTO.impressionsCount()).isEqualTo(5);
        assertThat(listingDTO.viewsCount()).isEqualTo(3);
        assertThat(listingDTO.phoneRequestsCount()).isEqualTo(2);
        assertThat(listingDTO.favouritesCount()).isEqualTo(1);
    }

    @Test
    void saveAnalytics_insertsSeparateRowsForMultipleListings() {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        long listingId1 = carListingService.create(userId).block().id();
        long listingId2 = carListingService.create(userId).block().id();

        CarListingAnalyticsCounter counter1 = new CarListingAnalyticsCounter();
        counter1.impressionsCount().add(2);
        CarListingAnalyticsCounter counter2 = new CarListingAnalyticsCounter();
        counter2.impressionsCount().add(4);

        long ts = System.currentTimeMillis();
        analyticsRepository.saveAnalytics(ts, List.of(
                Map.entry(listingId1, counter1),
                Map.entry(listingId2, counter2)
        )).block();

        OwnCarListingListItemDTO listingDTO = queryAnalytics(userId, listingId1);
        assertThat(listingDTO).isNotNull();
        assertThat(listingDTO.impressionsCount()).isEqualTo(2);

        listingDTO = queryAnalytics(userId, listingId2);
        assertThat(listingDTO).isNotNull();
        assertThat(listingDTO.impressionsCount()).isEqualTo(4);
    }

    @Test
    void saveAnalytics_allowsMultipleRowsForSameListingWithDifferentTimestamps() {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        long listingId = carListingService.create(userId).block().id();

        CarListingAnalyticsCounter counter1 = new CarListingAnalyticsCounter();
        counter1.impressionsCount().add(3);
        CarListingAnalyticsCounter counter2 = new CarListingAnalyticsCounter();
        counter2.impressionsCount().add(7);

        long ts1 = System.currentTimeMillis();
        long ts2 = ts1 + 60_000;

        analyticsRepository.saveAnalytics(ts1, List.of(Map.entry(listingId, counter1))).block();
        analyticsRepository.saveAnalytics(ts2, List.of(Map.entry(listingId, counter2))).block();

        OwnCarListingListItemDTO listingDTO = queryAnalytics(userId, listingId);
        assertThat(listingDTO).isNotNull();
        assertThat(listingDTO.impressionsCount()).isEqualTo(10);
    }

    @Test
    void saveAnalytics_aggregatesCountersOnConflict() {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        long listingId = carListingService.create(userId).block().id();

        long ts = System.currentTimeMillis();

        CarListingAnalyticsCounter counter1 = new CarListingAnalyticsCounter();
        counter1.impressionsCount().add(3);
        counter1.viewsCount().add(1);

        CarListingAnalyticsCounter counter2 = new CarListingAnalyticsCounter();
        counter2.impressionsCount().add(5);
        counter2.viewsCount().add(2);

        analyticsRepository.saveAnalytics(ts, List.of(Map.entry(listingId, counter1))).block();
        analyticsRepository.saveAnalytics(ts, List.of(Map.entry(listingId, counter2))).block();

        OwnCarListingListItemDTO listingDTO = queryAnalytics(userId, listingId);
        assertThat(listingDTO).isNotNull();
        assertThat(listingDTO.impressionsCount()).isEqualTo(8);
        assertThat(listingDTO.viewsCount()).isEqualTo(3);
    }

    @Test
    void clearOldAnalytics() {
        long userId = userService.getUserByPhoneNumberOrCreate("+380123456789").block().id();
        long listingId = carListingService.create(userId).block().id();

        long ts = System.currentTimeMillis();
        long yearAgoTS = ts - Duration.ofDays(365).toMillis() - 1_000;

        CarListingAnalyticsCounter counter1 = new CarListingAnalyticsCounter();
        counter1.impressionsCount().add(3);

        CarListingAnalyticsCounter counter2 = new CarListingAnalyticsCounter();
        counter2.impressionsCount().add(5);

        analyticsRepository.saveAnalytics(ts, List.of(Map.entry(listingId, counter1))).block();
        analyticsRepository.saveAnalytics(yearAgoTS, List.of(Map.entry(listingId, counter2))).block();

        OwnCarListingListItemDTO listingDTO = queryAnalytics(userId, listingId);
        assertThat(listingDTO).isNotNull();
        assertThat(listingDTO.impressionsCount()).isEqualTo(8);

        Long rowsDeleted = analyticsRepository.clearOldAnalytics(System.currentTimeMillis() - Duration.ofDays(5).toMillis()).block();
        assertThat(rowsDeleted).isNotNull().isEqualTo(1);

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