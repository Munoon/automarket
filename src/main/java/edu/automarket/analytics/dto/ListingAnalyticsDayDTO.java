package edu.automarket.analytics.dto;

public record ListingAnalyticsDayDTO(
        long ts,
        long impressionsCount,
        long viewsCount,
        long phoneRequestsCount,
        long favouritesCount
) {
}
