package edu.automarket.analytics;

import edu.automarket.analytics.dto.ListingAnalyticsDayDTO;
import jakarta.annotation.PreDestroy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.MINUTES;

@Service
public class CarListingAnalyticsService {
    private static final Logger log = LogManager.getLogger(CarListingAnalyticsService.class);
    private final CarListingAnalyticsRepository carListingAnalyticsRepository;
    private final long analyticsStorageDurationMS;
    private final ConcurrentMap<Long, CarListingAnalyticsCounter> listingsCounters = new ConcurrentHashMap<>();
    private volatile long countersTS = System.currentTimeMillis();

    public CarListingAnalyticsService(CarListingAnalyticsRepository carListingAnalyticsRepository,
                                      @Value("${app.listing.analytics.storageDuration:365d}") Duration analyticsStorageDuration) {
        this.carListingAnalyticsRepository = carListingAnalyticsRepository;
        this.analyticsStorageDurationMS = analyticsStorageDuration.toMillis();
    }

    public Flux<ListingAnalyticsDayDTO> getListingAnalyticsByDay(long listingId, ZoneId zoneId) {
        return carListingAnalyticsRepository.getAnalyticsByDay(listingId, zoneId);
    }

    public void recordListingImpression(Long listingId) {
        getListingCounter(listingId).impressionsCount().increment();
    }

    public void recordListingView(Long listingId) {
        getListingCounter(listingId).viewsCount().increment();
    }

    public void recordListingPhoneRequest(Long listingId) {
        getListingCounter(listingId).phoneRequestsCount().increment();
    }

    public void recordListingAddedToFavourity(Long listingId) {
        getListingCounter(listingId).favouritesCount().increment();
    }

    @PreDestroy
    @Scheduled(fixedRate = 1, initialDelay = 1, timeUnit = MINUTES)
    public void saveListingAnalytics() {
        long countersTS = this.countersTS;
        List<Entry<Long, CarListingAnalyticsCounter>> analytics = new ArrayList<>(listingsCounters.size());

        this.countersTS = System.currentTimeMillis();

        int count = 0;
        Iterator<Entry<Long, CarListingAnalyticsCounter>> iterator = listingsCounters.entrySet().iterator();
        while (iterator.hasNext()) {
            analytics.add(iterator.next());
            iterator.remove();
            ++count;
        }

        if (count > 0) {
            log.info("Saving analytics for {} listings", count);
            long startTS = System.currentTimeMillis();
            carListingAnalyticsRepository.saveAnalytics(countersTS, analytics).block();
            long endTS = System.currentTimeMillis();
            log.info("Saved {} listings in {}ms", count, endTS - startTS);
        }
    }

    @Scheduled(fixedRate = 1, initialDelay = 1, timeUnit = HOURS)
    public void clearOldAnalytics() {
        long minTS = System.currentTimeMillis() - analyticsStorageDurationMS;
        Long rowsDeleted = carListingAnalyticsRepository.clearOldAnalytics(minTS).block();
        if (rowsDeleted != null && rowsDeleted > 0) {
            log.info("Deleted {} old analytics records", rowsDeleted);
        }
    }

    private CarListingAnalyticsCounter getListingCounter(Long listingId) {
        return listingsCounters.computeIfAbsent(listingId, _ -> new CarListingAnalyticsCounter());
    }
}
