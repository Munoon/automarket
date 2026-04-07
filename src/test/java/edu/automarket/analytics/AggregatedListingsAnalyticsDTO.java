package edu.automarket.analytics;

import edu.automarket.analytics.dto.ListingAnalyticsDayDTO;

import java.time.ZoneOffset;
import java.util.List;

public record AggregatedListingsAnalyticsDTO(
        long impressionsCount,
        long viewsCount,
        long phoneRequestsCount,
        long favouritesCount
) {
    public static AggregatedListingsAnalyticsDTO compute(CarListingAnalyticsService analyticsService, long listingId) {
        List<ListingAnalyticsDayDTO> data = analyticsService.getListingAnalyticsByDay(listingId, ZoneOffset.UTC)
                .collectList().block();

        long impressionsCount = 0;
        long viewsCount = 0;
        long phoneRequestsCount = 0;
        long favouritesCount = 0;

        for (ListingAnalyticsDayDTO entry : data) {
            impressionsCount += entry.impressionsCount();
            viewsCount += entry.viewsCount();
            phoneRequestsCount += entry.phoneRequestsCount();
            favouritesCount += entry.favouritesCount();
        }

        return new AggregatedListingsAnalyticsDTO(impressionsCount, viewsCount, phoneRequestsCount, favouritesCount);
    }
}
